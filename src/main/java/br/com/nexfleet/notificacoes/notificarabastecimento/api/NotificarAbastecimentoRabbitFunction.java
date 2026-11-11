package br.com.nexfleet.notificacoes.notificarabastecimento.api;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.rabbitmq.annotation.RabbitMQTrigger;

/** Compatibilidade do ambiente local; fica desabilitada na Function Azure. */
public final class NotificarAbastecimentoRabbitFunction {
  private final NotificarAbastecimentoFunction delegate = new NotificarAbastecimentoFunction();

  @FunctionName("NotificarAbastecimentoRabbit")
  public void executar(
      @RabbitMQTrigger(
          connectionStringSetting = "RabbitMqConnection",
          queueName = "nexfleet.notificacoes.abastecimentos.v1") String evento,
      ExecutionContext contexto) {
    delegate.processar(evento, contexto);
  }
}
