package br.com.nexfleet.notificacoes.calculareficiencia.api;

import br.com.nexfleet.notificacoes.calculareficiencia.application.CalcularEficiencia;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import java.util.Optional;

public final class CalcularEficienciaFunction {
  private final CalcularEficiencia casoDeUso;
  private final ObjectMapper json;

  public CalcularEficienciaFunction() {
    this(new CalcularEficiencia(), new ObjectMapper().findAndRegisterModules());
  }

  CalcularEficienciaFunction(CalcularEficiencia casoDeUso, ObjectMapper json) {
    this.casoDeUso = casoDeUso;
    this.json = json;
  }

  @FunctionName("CalcularEficiencia")
  public HttpResponseMessage executar(
      @HttpTrigger(
          name = "requisicao",
          methods = {HttpMethod.POST},
          authLevel = AuthorizationLevel.FUNCTION,
          route = "calcular-eficiencia") HttpRequestMessage<Optional<String>> requisicao,
      ExecutionContext contexto) {
    throw new UnsupportedOperationException("não implementado");
  }
}
