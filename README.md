# NexFleet — Azure Function

Componente serverless do NexFleet, plataforma de gestão de frotas. Parte da v1.0.0; o sistema completo está no repositório guarda-chuva [nexfleet](https://github.com/mateusblm/nexfleet).

## Arquitetura

Uma Function App Java (`nexfleet-notificacoes`) com dois papéis:

- **Gatilho HTTP `POST /api/calcular-eficiencia`** (`CalcularEficiencia`): sem estado e sem banco. O BFF envia os abastecimentos e manutenções de cada veículo ao montar `GET /aggregated-data` e recebe consumo médio (km/l), custo por km e gasto, por veículo e da frota.
- **Notificação de abastecimento**: consome `AbastecimentoRegistrado` v1 publicado pela outbox de Operações, do **Service Bus** (`NotificarAbastecimentoServiceBus`, tópico `nexfleet-eventos-v1`, assinatura `notificacoes-abastecimentos-v1`) ou do **RabbitMQ** local (`NotificarAbastecimentoRabbit`), registra a notificação de forma idempotente no **Azure SQL** (schema `notificacoes`) e envia o e-mail pelo Azure Communication Services (localmente o e-mail é só capturado no banco).

```text
Navegador → shell + microfrontends (Static Web Apps)
          → Azure API Management (APIM) → Gateway → BFF
              ├─ MS1 Frota ........ MongoDB Atlas
              ├─ MS2 Operações .... Azure SQL (schema operacoes)
              ├─ Identidade, Indicadores ... Azure SQL (um schema e um login por serviço)
              └─ Azure Function HTTP calcular-eficiencia
Operações → outbox → Service Bus (nuvem) / RabbitMQ (local) → Indicadores + Function (notificação)
```

**Contrato de `calcular-eficiencia`** (completo em [docs/calcular-eficiencia.md](docs/calcular-eficiencia.md)):

- `authLevel = FUNCTION`: a chave vai no cabeçalho `x-functions-key` (ou `?code=`); sem chave válida o runtime responde `401`;
- corpo `{ "veiculos": [ { veiculoId, placa, abastecimentos: [{data, litros, valor, quilometragem}], manutencoes: [{valor}] } ] }`;
- no máximo **10.000 itens** por requisição (veículos + abastecimentos + manutenções); JSON inválido, número negativo ou acima do limite → `400` sem cálculo;
- com menos de dois abastecimentos, `consumoMedioKmL` e `custoPorKm` do veículo vêm `null` (o gasto é calculado);
- o cabeçalho `X-Correlation-ID` é lido, registrado nos logs e devolvido (gerado se ausente ou inválido).

**Clean Architecture + Vertical Slice.** Fatias em `br.com.nexfleet.notificacoes`:

| Fatia | Camadas | Responsabilidade |
|---|---|---|
| `calculareficiencia` | api, application, domain | gatilho HTTP e cálculo de eficiência |
| `notificarabastecimento` | api, application, infrastructure | gatilhos de evento, idempotência e e-mail |

Pacote técnico: `config` (composição das dependências).

**Testes:** unitários do cálculo e do gatilho HTTP (`CalcularEficienciaTest`, `CalcularEficienciaValidacaoTest`, `CalcularEficienciaFunctionTest`, `NotificarAbastecimentoTest`), de integração com SQL Server real em Testcontainers (`MigracaoSqlServerIT`, `JdbcProvedorEmailCapturadoIT`) e de arquitetura com ArchUnit (`ArchitectureTest`: domínio e aplicação sem framework; entrada sem acesso direto à persistência).

## Tecnologias

- Java 21, Azure Functions (runtime 4, extension bundle 4.x), `azure-functions-java-library` 3.2.3, `azure-functions-maven-plugin` 1.38.0, `azure-functions-java-library-rabbitmq` 2.0.0
- Azure SDK BOM 1.3.9 (`azure-communication-email`, `azure-identity`)
- SQL Server / Azure SQL (`mssql-jdbc` 13.4.0, HikariCP 7.0.2; migração Flyway `flyway-sqlserver` 12.4.0)
- Jackson 2.20.0
- JUnit 5.13.4, AssertJ 3.27.6, Testcontainers 2.0.5 (SQL Server), ArchUnit 1.4.1
- Maven Wrapper (Maven 3.9.16)
- Docker (`mcr.microsoft.com/azure-functions/java:4-java21`), GitHub Actions (testes em todo push; imagem no Docker Hub em tag `v*`)

## Como rodar localmente

```bash
./mvnw -B -ntp verify                                   # unitários, ArchUnit e ITs (Testcontainers; exige Docker)
docker build -t mateusblm/nexfleet-function:local .     # imagem para execução local (porta 80)
```

Na Azure a Function é implantada por pacote zip (OneDeploy), não por imagem.

Variáveis de ambiente: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `EMAIL_PROVIDER` (`azure-communication` na nuvem; padrão captura local), `AZURE_COMMUNICATION_EMAIL_ENDPOINT`, `EMAIL_SENDER_ADDRESS`, `EMAIL_RECIPIENT_ADDRESS`, `NexFleetServiceBus__fullyQualifiedNamespace` (Service Bus, com identidade gerenciada), `RabbitMqConnection` (local), `AzureWebJobsStorage` e `FUNCTIONS_WORKER_RUNTIME=java`. No Compose do guarda-chuva, `EFICIENCIA_FUNCTION_KEY` e `FUNCTION_MASTER_KEY` definem as chaves locais.

**Ambiente completo** (todos os serviços, SQL Server, MongoDB e RabbitMQ locais) sobe pelo repositório guarda-chuva [nexfleet](https://github.com/mateusblm/nexfleet), com os repositórios clonados lado a lado (`scripts/clonar-tudo.sh`):

```bash
scripts/iniciar-local.sh                                     # 1ª vez: gera o .env e a chave RSA local
docker compose -f compose.yaml -f compose.build.yaml up -d --build
scripts/seed-demo.sh                                         # dados de demonstração
```

Sem o override `compose.build.yaml`, o `docker compose up -d` usa as imagens publicadas no Docker Hub.

## URLs na nuvem

A Function não tem URL de uso público: roda na Function App `func-nexfleet-homolog-ukzjodf42myfe`, não é exposta pelo APIM e só o BFF a chama, com a chave da função. O resultado aparece em `/aggregated-data` (campo `eficiencia`).

Imagem Docker (só para execução local): [`mateusblm/nexfleet-function:v1.0.0`](https://hub.docker.com/r/mateusblm/nexfleet-function), publicada também pelo CI em tag `v*`; na nuvem não se usa imagem.

| Recurso | Endereço |
|---|---|
| Frontend | https://white-mushroom-0cadda010.3.azurestaticapps.net |
| API (APIM) | https://apim-nexfleet-homolog-ukzjodf42myfe.azure-api.net |
| Swagger UI (BFF, Frota, Operações, Identidade, Indicadores) | https://apim-nexfleet-homolog-ukzjodf42myfe.azure-api.net/swagger-ui.html |
| Dados agregados (exige token) | https://apim-nexfleet-homolog-ukzjodf42myfe.azure-api.net/aggregated-data |
| AsyncAPI dos eventos | https://white-mushroom-0cadda010.3.azurestaticapps.net/docs/eventos/ |
| Repositório guarda-chuva | https://github.com/mateusblm/nexfleet |

> **Partida a frio:** os Container Apps escalam a zero e o Azure SQL pausa sem uso. A primeira chamada após um período de inatividade pode levar de 1 a 2 minutos (ou responder 503/504); tente de novo em seguida.

## Equipe

- Vinícius dos Reis Góes
- Mateus Burlamaqui
- Lucas Roussenq
- Erik Carvalho
