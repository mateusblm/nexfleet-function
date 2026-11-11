package br.com.nexfleet.notificacoes.notificarabastecimento.api;

import br.com.nexfleet.notificacoes.config.CompositionRoot;
import br.com.nexfleet.notificacoes.notificarabastecimento.application.NotificarAbastecimento;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.ServiceBusTopicTrigger;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class NotificarAbastecimentoFunction {
  private record Payload(
      UUID eventoId,
      UUID abastecimentoId,
      UUID veiculoId,
      UUID motoristaId,
      Instant realizadoEm,
      String combustivel,
      BigDecimal litros,
      BigDecimal valorTotal,
      Long quilometragem,
      UUID correlationId) {}

  private final NotificarAbastecimento casoDeUso;
  private final ObjectMapper json;

  public NotificarAbastecimentoFunction() {
    this(CompositionRoot.notificarAbastecimento(), new ObjectMapper().findAndRegisterModules());
  }

  NotificarAbastecimentoFunction(NotificarAbastecimento casoDeUso, ObjectMapper json) {
    this.casoDeUso = casoDeUso;
    this.json = json;
  }

  @FunctionName("NotificarAbastecimentoServiceBus")
  public void executar(
      @ServiceBusTopicTrigger(
          name = "evento",
          dataType = "string",
          topicName = "nexfleet-eventos-v1",
          subscriptionName = "notificacoes-abastecimentos-v1",
          connection = "NexFleetServiceBus") String evento,
      ExecutionContext contexto) {
    processar(evento, contexto);
  }

  void processar(String evento, ExecutionContext contexto) {
    try {
      Payload payload = json.readValue(evento, Payload.class);
      var resultado = casoDeUso.executar(new NotificarAbastecimento.Entrada(
          payload.eventoId(),
          payload.abastecimentoId(),
          payload.veiculoId(),
          payload.realizadoEm(),
          payload.combustivel(),
          payload.litros(),
          payload.valorTotal(),
          payload.quilometragem()));
      UUID correlationId = payload.correlationId() == null
          ? payload.eventoId()
          : payload.correlationId();
      contexto.getLogger().info("event_consumed correlationId=" + correlationId
          + " eventId=" + payload.eventoId()
          + " eventType=AbastecimentoRegistrado result=" + resultado);
    } catch (RuntimeException falha) {
      throw falha;
    } catch (Exception falha) {
      throw new IllegalArgumentException("Evento AbastecimentoRegistrado.v1 inválido.", falha);
    }
  }
}
