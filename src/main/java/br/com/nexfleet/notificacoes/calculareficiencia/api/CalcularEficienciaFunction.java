package br.com.nexfleet.notificacoes.calculareficiencia.api;

import br.com.nexfleet.notificacoes.calculareficiencia.application.CalcularEficiencia;
import br.com.nexfleet.notificacoes.calculareficiencia.domain.Abastecimento;
import br.com.nexfleet.notificacoes.calculareficiencia.domain.EntradaInvalidaException;
import br.com.nexfleet.notificacoes.calculareficiencia.domain.Manutencao;
import br.com.nexfleet.notificacoes.calculareficiencia.domain.Veiculo;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Gatilho HTTP sem estado {@code POST /api/calcular-eficiencia}. A chave da função
 * ({@code authLevel = FUNCTION}) é verificada pelo runtime, que responde 401 sem ela.
 * Contrato em {@code docs/calcular-eficiencia.md}.
 */
public final class CalcularEficienciaFunction {
  private record AbastecimentoJson(Instant data, BigDecimal litros, BigDecimal valor, Long quilometragem) {}

  private record ManutencaoJson(BigDecimal valor) {}

  private record VeiculoJson(
      UUID veiculoId, String placa, List<AbastecimentoJson> abastecimentos, List<ManutencaoJson> manutencoes) {}

  private record Corpo(List<VeiculoJson> veiculos) {}

  private final CalcularEficiencia casoDeUso;
  private final ObjectMapper json;

  public CalcularEficienciaFunction() {
    this(new CalcularEficiencia(), new ObjectMapper().findAndRegisterModules());
  }

  CalcularEficienciaFunction(CalcularEficiencia casoDeUso, ObjectMapper json) {
    this.casoDeUso = casoDeUso;
    this.json = json.copy()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
        .enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);
  }

  @FunctionName("CalcularEficiencia")
  public HttpResponseMessage executar(
      @HttpTrigger(
          name = "requisicao",
          methods = {HttpMethod.POST},
          authLevel = AuthorizationLevel.FUNCTION,
          route = "calcular-eficiencia") HttpRequestMessage<Optional<String>> requisicao,
      ExecutionContext contexto) {
    try {
      Corpo corpo = json.readValue(requisicao.getBody().orElse(""), Corpo.class);
      if (corpo == null) {
        throw new EntradaInvalidaException("Corpo ausente.");
      }
      var resultado = casoDeUso.executar(paraDominio(corpo));
      contexto.getLogger().info("calcular_eficiencia result=ok veiculos=" + resultado.veiculos().size());
      return responder(requisicao, HttpStatus.OK, resultado);
    } catch (EntradaInvalidaException invalida) {
      return rejeitar(requisicao, contexto, invalida.getMessage());
    } catch (JsonProcessingException malformado) {
      return rejeitar(requisicao, contexto, "JSON malformado ou com tipos inválidos.");
    }
  }

  private static List<Veiculo> paraDominio(Corpo corpo) {
    if (corpo.veiculos() == null) {
      throw new EntradaInvalidaException("Campo veiculos ausente.");
    }
    return corpo.veiculos().stream()
        .map(v -> {
          if (v == null) {
            throw new EntradaInvalidaException("Veículo nulo.");
          }
          return new Veiculo(
              v.veiculoId(),
              v.placa(),
              v.abastecimentos() == null ? null : v.abastecimentos().stream()
                  .map(a -> a == null ? null
                      : new Abastecimento(a.data(), a.litros(), a.valor(), a.quilometragem()))
                  .toList(),
              v.manutencoes() == null ? null : v.manutencoes().stream()
                  .map(m -> m == null ? null : new Manutencao(m.valor()))
                  .toList());
        })
        .toList();
  }

  private HttpResponseMessage rejeitar(
      HttpRequestMessage<Optional<String>> requisicao, ExecutionContext contexto, String motivo) {
    contexto.getLogger().warning("calcular_eficiencia result=bad_request");
    return responder(requisicao, HttpStatus.BAD_REQUEST, Map.of("erro", motivo));
  }

  private HttpResponseMessage responder(
      HttpRequestMessage<Optional<String>> requisicao, HttpStatus status, Object corpo) {
    try {
      return requisicao.createResponseBuilder(status)
          .header("Content-Type", "application/json")
          .body(json.writeValueAsString(corpo))
          .build();
    } catch (JsonProcessingException falha) {
      throw new IllegalStateException("Falha ao serializar resposta.", falha);
    }
  }
}
