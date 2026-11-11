package br.com.nexfleet.notificacoes.notificarabastecimento.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class NotificarAbastecimentoTest {
  private static final Instant AGORA = Instant.parse("2026-11-06T12:00:00Z");

  @Test
  void enviaUmaSolicitacaoComOsDadosDoEvento() {
    var recebido = new AtomicReference<ProvedorEmail.Email>();
    ProvedorEmail provedor = email -> { recebido.set(email); return true; };
    var casoDeUso = new NotificarAbastecimento(
        provedor, Clock.fixed(AGORA, ZoneOffset.UTC));

    var resultado = casoDeUso.executar(eventoValido());

    assertThat(resultado).isEqualTo(NotificarAbastecimento.Resultado.ENVIADA);
    assertThat(recebido.get().eventoId()).isEqualTo(eventoValido().eventoId());
    assertThat(recebido.get().assunto()).isEqualTo("Abastecimento registrado no NexFleet");
    assertThat(recebido.get().corpo())
        .contains("45.5 L de DIESEL", "R$ 320.00", "1500 km");
    assertThat(recebido.get().solicitadoEm()).isEqualTo(AGORA);
  }

  @Test
  void informaDuplicataQuandoOProvedorJaPossuiOEvento() {
    var casoDeUso = new NotificarAbastecimento(
        email -> false, Clock.fixed(AGORA, ZoneOffset.UTC));

    assertThat(casoDeUso.executar(eventoValido()))
        .isEqualTo(NotificarAbastecimento.Resultado.DUPLICADA);
  }

  @Test
  void rejeitaEventoIncompletoParaPermitirRetryEDlq() {
    var entrada = new NotificarAbastecimento.Entrada(
        null, UUID.randomUUID(), UUID.randomUUID(), AGORA,
        "DIESEL", new BigDecimal("45.5"), new BigDecimal("320"), 1500L);
    var casoDeUso = new NotificarAbastecimento(
        email -> true, Clock.fixed(AGORA, ZoneOffset.UTC));

    assertThatThrownBy(() -> casoDeUso.executar(entrada))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Evento AbastecimentoRegistrado.v1 inválido.");
  }

  private static NotificarAbastecimento.Entrada eventoValido() {
    return new NotificarAbastecimento.Entrada(
        UUID.fromString("11111111-1111-1111-1111-111111111111"),
        UUID.fromString("22222222-2222-2222-2222-222222222222"),
        UUID.fromString("33333333-3333-3333-3333-333333333333"),
        AGORA,
        "DIESEL",
        new BigDecimal("45.5"),
        new BigDecimal("320"),
        1500L);
  }
}
