package br.com.nexfleet.notificacoes.calculareficiencia.api;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.nexfleet.notificacoes.calculareficiencia.application.CalcularEficiencia;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.HttpStatusType;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

class CalcularEficienciaFunctionTest {
  private static final String VEICULO_ID = "00000000-0000-0000-0000-00000000000a";
  private final ObjectMapper json = new ObjectMapper();
  private final CalcularEficienciaFunction function =
      new CalcularEficienciaFunction(new CalcularEficiencia(), new ObjectMapper().findAndRegisterModules());

  @Test
  void devolveEficienciaPorVeiculoEDaFrotaParaCorpoValido() throws Exception {
    String corpo = """
        {"veiculos":[
          {"veiculoId":"%s","placa":"ABC1D23",
           "abastecimentos":[
             {"data":"2026-10-01T10:00:00Z","litros":40,"valor":200.00,"quilometragem":10000},
             {"data":"2026-10-20T10:00:00Z","litros":50,"valor":250.50,"quilometragem":10500},
             {"data":"2026-10-10T10:00:00Z","litros":30,"valor":150.25,"quilometragem":10300}],
           "manutencoes":[{"valor":99.25},{"valor":50}]},
          {"veiculoId":"00000000-0000-0000-0000-00000000000c","placa":"CDE3F45",
           "abastecimentos":[
             {"data":"2026-10-01T10:00:00Z","litros":20,"valor":100,"quilometragem":5000}],
           "manutencoes":[{"valor":30}]}
        ]}""".formatted(VEICULO_ID);

    HttpResponseMessage resposta = function.executar(requisicao(corpo), contexto());

    assertThat(resposta.getStatus()).isEqualTo(HttpStatus.OK);
    assertThat(resposta.getHeader("Content-Type")).isEqualTo("application/json");
    JsonNode raiz = json.readTree((String) resposta.getBody());
    JsonNode a = raiz.get("veiculos").get(0);
    assertThat(a.get("veiculoId").asText()).isEqualTo(VEICULO_ID);
    assertThat(a.get("placa").asText()).isEqualTo("ABC1D23");
    assertThat(a.get("consumoMedioKmL").decimalValue()).isEqualByComparingTo("6.25");
    assertThat(a.get("custoPorKm").decimalValue()).isEqualByComparingTo("1.50");
    assertThat(a.get("gastoAbastecimento").decimalValue()).isEqualByComparingTo("600.75");
    assertThat(a.get("gastoManutencao").decimalValue()).isEqualByComparingTo("149.25");
    assertThat(a.get("gastoTotal").decimalValue()).isEqualByComparingTo("750.00");
    assertThat(a.get("distanciaKm").asLong()).isEqualTo(500);
    JsonNode c = raiz.get("veiculos").get(1);
    assertThat(c.get("consumoMedioKmL").isNull()).isTrue();
    assertThat(c.get("custoPorKm").isNull()).isTrue();
    assertThat(c.get("gastoTotal").decimalValue()).isEqualByComparingTo("130.00");
    JsonNode frota = raiz.get("frota");
    assertThat(frota.get("consumoMedioKmL").decimalValue()).isEqualByComparingTo("6.25");
    assertThat(frota.get("custoPorKm").decimalValue()).isEqualByComparingTo("1.50");
    assertThat(frota.get("gastoTotal").decimalValue()).isEqualByComparingTo("880.00");
    assertThat(frota.get("distanciaKm").asLong()).isEqualTo(500);
  }

  @Test
  void respondeBadRequestParaJsonMalformado() {
    assertThat(function.executar(requisicao("{\"veiculos\": [ {"), contexto()).getStatus())
        .isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void respondeBadRequestParaCorpoAusente() {
    assertThat(function.executar(requisicao(null), contexto()).getStatus())
        .isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void respondeBadRequestParaValorNegativo() {
    String corpo = """
        {"veiculos":[{"veiculoId":"%s","placa":"ABC1D23","abastecimentos":[
          {"data":"2026-10-01T10:00:00Z","litros":-1,"valor":10,"quilometragem":100}],
          "manutencoes":[]}]}""".formatted(VEICULO_ID);

    assertThat(function.executar(requisicao(corpo), contexto()).getStatus())
        .isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void respondeBadRequestAcimaDeMilItens() {
    var manutencoes = new StringBuilder();
    for (int i = 0; i < 1000; i++) {
      manutencoes.append(i == 0 ? "" : ",").append("{\"valor\":1}");
    }
    String corpo = """
        {"veiculos":[{"veiculoId":"%s","placa":"ABC1D23","abastecimentos":[],
          "manutencoes":[%s]}]}""".formatted(VEICULO_ID, manutencoes);

    HttpResponseMessage resposta = function.executar(requisicao(corpo), contexto());

    assertThat(resposta.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat((String) resposta.getBody()).contains("erro");
  }

  @Test
  void exigeChaveDaFuncaoNoGatilhoHttpPost() throws Exception {
    Method metodo = CalcularEficienciaFunction.class.getMethod(
        "executar", HttpRequestMessage.class, ExecutionContext.class);
    HttpTrigger gatilho = metodo.getParameters()[0].getAnnotation(HttpTrigger.class);

    assertThat(metodo.getAnnotation(FunctionName.class).value()).isEqualTo("CalcularEficiencia");
    assertThat(gatilho.authLevel()).isEqualTo(AuthorizationLevel.FUNCTION);
    assertThat(gatilho.route()).isEqualTo("calcular-eficiencia");
    assertThat(gatilho.methods()).containsExactly(HttpMethod.POST);
  }

  private static HttpRequestMessage<Optional<String>> requisicao(String corpo) {
    return new HttpRequestMessage<>() {
      @Override public URI getUri() { return URI.create("http://localhost/api/calcular-eficiencia"); }
      @Override public HttpMethod getHttpMethod() { return HttpMethod.POST; }
      @Override public Map<String, String> getHeaders() { return Map.of(); }
      @Override public Map<String, String> getQueryParameters() { return Map.of(); }
      @Override public Optional<String> getBody() { return Optional.ofNullable(corpo); }
      @Override public HttpResponseMessage.Builder createResponseBuilder(HttpStatus status) {
        return new RespostaFalsa.Construtor(status);
      }
      @Override public HttpResponseMessage.Builder createResponseBuilder(HttpStatusType status) {
        return new RespostaFalsa.Construtor(status);
      }
    };
  }

  private static ExecutionContext contexto() {
    return new ExecutionContext() {
      @Override public Logger getLogger() { return Logger.getLogger("calcular-eficiencia-teste"); }
      @Override public String getInvocationId() { return "invocation-test"; }
      @Override public String getFunctionName() { return "CalcularEficiencia"; }
    };
  }

  private record RespostaFalsa(HttpStatusType status, Map<String, String> cabecalhos, Object corpo)
      implements HttpResponseMessage {
    @Override public HttpStatusType getStatus() { return status; }
    @Override public String getHeader(String chave) { return cabecalhos.get(chave); }
    @Override public Object getBody() { return corpo; }

    static final class Construtor implements HttpResponseMessage.Builder {
      private HttpStatusType status;
      private final Map<String, String> cabecalhos = new HashMap<>();
      private Object corpo;

      Construtor(HttpStatusType status) { this.status = status; }
      @Override public HttpResponseMessage.Builder status(HttpStatusType novo) { status = novo; return this; }
      @Override public HttpResponseMessage.Builder header(String chave, String valor) {
        cabecalhos.put(chave, valor);
        return this;
      }
      @Override public HttpResponseMessage.Builder body(Object novo) { corpo = novo; return this; }
      @Override public HttpResponseMessage build() { return new RespostaFalsa(status, cabecalhos, corpo); }
    }
  }
}
