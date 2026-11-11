# Contrato: `POST /api/calcular-eficiencia`

Gatilho HTTP sem estado da Function (fatia `calculareficiencia`). Calcula consumo médio, custo
por km e gasto por veículo e da frota a partir do histórico enviado pelo chamador (o BFF). Não
acessa banco nem publica eventos.

- **Método/rota:** `POST /api/calcular-eficiencia`
- **Autenticação:** `authLevel = FUNCTION`. Envie a chave da função no cabeçalho
  `x-functions-key` (ou na query `?code=`). Sem chave válida o runtime do Azure Functions
  responde `401` antes de executar o código.
- **Content-Type:** `application/json` na requisição e na resposta.

## Requisição

```json
{
  "veiculos": [
    {
      "veiculoId": "00000000-0000-0000-0000-00000000000a",
      "placa": "ABC1D23",
      "abastecimentos": [
        { "data": "2026-10-01T10:00:00Z", "litros": 40, "valor": 200.00, "quilometragem": 10000 },
        { "data": "2026-10-20T10:00:00Z", "litros": 50, "valor": 250.50, "quilometragem": 10500 },
        { "data": "2026-10-10T10:00:00Z", "litros": 30, "valor": 150.25, "quilometragem": 10300 }
      ],
      "manutencoes": [ { "valor": 99.25 }, { "valor": 50 } ]
    },
    {
      "veiculoId": "00000000-0000-0000-0000-00000000000c",
      "placa": "CDE3F45",
      "abastecimentos": [
        { "data": "2026-10-01T10:00:00Z", "litros": 20, "valor": 100, "quilometragem": 5000 }
      ],
      "manutencoes": [ { "valor": 30 } ]
    }
  ]
}
```

| Campo | Tipo | Regra |
|---|---|---|
| `veiculos` | array | obrigatório (pode ser vazio) |
| `veiculoId` | UUID | obrigatório |
| `placa` | string | ecoada na resposta |
| `abastecimentos`, `manutencoes` | array | opcionais; ausentes valem como vazios |
| `data` | ISO-8601 (instante, ex. `2026-10-01T10:00:00Z`) | obrigatório |
| `litros`, `valor` | número ≥ 0 | obrigatório |
| `quilometragem` | inteiro ≥ 0 | obrigatório (decimal é rejeitado) |

Limite: no máximo **1.000 itens** por requisição, contando veículos + abastecimentos + manutenções.
Campos desconhecidos são ignorados.

## Resposta `200`

```json
{
  "veiculos": [
    {
      "veiculoId": "00000000-0000-0000-0000-00000000000a",
      "placa": "ABC1D23",
      "consumoMedioKmL": 6.25,
      "custoPorKm": 1.50,
      "gastoAbastecimento": 600.75,
      "gastoManutencao": 149.25,
      "gastoTotal": 750.00,
      "distanciaKm": 500
    },
    {
      "veiculoId": "00000000-0000-0000-0000-00000000000c",
      "placa": "CDE3F45",
      "consumoMedioKmL": null,
      "custoPorKm": null,
      "gastoAbastecimento": 100.00,
      "gastoManutencao": 30.00,
      "gastoTotal": 130.00,
      "distanciaKm": 0
    }
  ],
  "frota": {
    "consumoMedioKmL": 6.25,
    "custoPorKm": 1.50,
    "gastoTotal": 880.00,
    "distanciaKm": 500
  }
}
```

`veiculos` mantém a ordem da requisição.

## Regras de cálculo

Por veículo, com os abastecimentos ordenados por `quilometragem`:

- `distanciaKm` = km máximo − km mínimo (0 com menos de dois abastecimentos).
- `consumoMedioKmL` = distância / soma dos litros dos abastecimentos **posteriores ao primeiro**
  (o primeiro só marca o tanque cheio de partida).
- `gastoAbastecimento` = soma dos `valor` de todos os abastecimentos;
  `gastoManutencao` = soma das manutenções; `gastoTotal` = soma dos dois.
- `custoPorKm` = `gastoTotal` / distância.
- Com menos de dois abastecimentos ou distância 0, `consumoMedioKmL` e `custoPorKm` vêm `null`
  (o gasto é calculado normalmente). Se os litros posteriores ao primeiro somarem 0, só o
  consumo vem `null`.

Frota:

- `distanciaKm`, litros e gastos usados em `consumoMedioKmL` e `custoPorKm` somam apenas os
  veículos com distância > 0 (média ponderada, não média das médias).
- `gastoTotal` soma o gasto de **todos** os veículos, inclusive os sem distância.
- Sem nenhum veículo com distância > 0, `consumoMedioKmL` e `custoPorKm` vêm `null`.

Arredondamento: valores monetários, consumo e custo por km com 2 casas decimais, `HALF_EVEN`,
calculados sobre as somas sem arredondar.

## Erros

| Status | Quando | Corpo |
|---|---|---|
| `400` | JSON malformado, corpo ausente, tipo inválido, campo obrigatório ausente, número negativo, mais de 1.000 itens | `{ "erro": "<motivo>" }` |
| `401` | sem chave da função ou chave inválida (runtime do Azure Functions) | definido pelo runtime |

Nenhum cálculo é feito quando a resposta é `400`.
