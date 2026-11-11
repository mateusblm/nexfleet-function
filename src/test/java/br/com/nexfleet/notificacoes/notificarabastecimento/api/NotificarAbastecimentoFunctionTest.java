package br.com.nexfleet.notificacoes.notificarabastecimento.api;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.nexfleet.notificacoes.notificarabastecimento.application.NotificarAbastecimento;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

class NotificarAbastecimentoFunctionTest {
  @Test
  void registraCorrelationIdDoEventoSemExporOConteudo() {
    UUID eventoId = UUID.randomUUID();
    UUID correlationId = UUID.randomUUID();
    var mensagens = new StringBuilder();
    var function = function();

    function.executar(evento(eventoId, correlationId), contexto(mensagens));

    assertThat(mensagens.toString())
        .contains("correlationId=" + correlationId)
        .contains("eventId=" + eventoId)
        .contains("eventType=AbastecimentoRegistrado")
        .doesNotContain("Gasolina")
        .doesNotContain("150.00");
  }

  @Test
  void eventoAntigoUsaEventoIdComoCorrelacao() {
    UUID eventoId = UUID.randomUUID();
    var mensagens = new StringBuilder();

    function().executar(evento(eventoId, null), contexto(mensagens));

    assertThat(mensagens.toString()).contains("correlationId=" + eventoId);
  }

  @Test
  void aceitaCamposNovosDoEventoComoVersao() {
    UUID eventoId = UUID.randomUUID();
    var mensagens = new StringBuilder();
    String comVersao = evento(eventoId, null)
        .replaceFirst("}$", ",\"versao\":1,\"campoFuturo\":\"x\"}");

    function().executar(comVersao, contexto(mensagens));

    assertThat(mensagens.toString())
        .contains("eventId=" + eventoId)
        .contains("result=");
  }

  private NotificarAbastecimentoFunction function() {
    var caso = new NotificarAbastecimento(
        email -> true,
        Clock.fixed(Instant.parse("2026-11-07T12:00:00Z"), ZoneOffset.UTC));
    return new NotificarAbastecimentoFunction(
        caso, new ObjectMapper().findAndRegisterModules());
  }

  private String evento(UUID eventoId, UUID correlationId) {
    String correlation = correlationId == null
        ? ""
        : ",\"correlationId\":\"" + correlationId + "\"";
    return ("{\"eventoId\":\"%s\",\"abastecimentoId\":\"%s\","
        + "\"veiculoId\":\"%s\",\"motoristaId\":\"%s\","
        + "\"realizadoEm\":\"2026-11-07T10:00:00Z\",\"combustivel\":\"Gasolina\","
        + "\"litros\":30,\"valorTotal\":150.00,\"quilometragem\":40000%s}")
        .formatted(eventoId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), correlation);
  }

  private ExecutionContext contexto(StringBuilder mensagens) {
    Logger logger = Logger.getLogger("function-test-" + UUID.randomUUID());
    logger.setUseParentHandlers(false);
    logger.addHandler(new Handler() {
      @Override public void publish(LogRecord record) { mensagens.append(record.getMessage()); }
      @Override public void flush() {}
      @Override public void close() {}
    });
    return new ExecutionContext() {
      @Override public Logger getLogger() { return logger; }
      @Override public String getInvocationId() { return "invocation-test"; }
      @Override public String getFunctionName() { return "NotificarAbastecimento"; }
    };
  }
}
