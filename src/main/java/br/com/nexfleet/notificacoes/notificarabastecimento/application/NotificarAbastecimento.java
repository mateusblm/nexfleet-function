package br.com.nexfleet.notificacoes.notificarabastecimento.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class NotificarAbastecimento {
  public record Entrada(
      UUID eventoId,
      UUID abastecimentoId,
      UUID veiculoId,
      Instant realizadoEm,
      String combustivel,
      BigDecimal litros,
      BigDecimal valorTotal,
      Long quilometragem) {}

  public enum Resultado { ENVIADA, DUPLICADA }

  private final ProvedorEmail provedor;
  private final Clock relogio;

  public NotificarAbastecimento(ProvedorEmail provedor, Clock relogio) {
    this.provedor = Objects.requireNonNull(provedor);
    this.relogio = Objects.requireNonNull(relogio);
  }

  public Resultado executar(Entrada entrada) {
    validar(entrada);
    String assunto = "Abastecimento registrado no NexFleet";
    String corpo = "Veículo %s: %s L de %s, valor total R$ %s, hodômetro %d km."
        .formatted(
            entrada.veiculoId(),
            entrada.litros().stripTrailingZeros().toPlainString(),
            entrada.combustivel().trim(),
            entrada.valorTotal().setScale(2, RoundingMode.HALF_UP).toPlainString(),
            entrada.quilometragem());
    boolean enviada = provedor.enviarSeAusente(new ProvedorEmail.Email(
        entrada.eventoId(), entrada.abastecimentoId(), assunto, corpo, Instant.now(relogio)));
    return enviada ? Resultado.ENVIADA : Resultado.DUPLICADA;
  }

  private static void validar(Entrada entrada) {
    if (entrada == null
        || entrada.eventoId() == null
        || entrada.abastecimentoId() == null
        || entrada.veiculoId() == null
        || entrada.realizadoEm() == null
        || entrada.combustivel() == null
        || entrada.combustivel().isBlank()
        || entrada.litros() == null
        || entrada.litros().signum() <= 0
        || entrada.valorTotal() == null
        || entrada.valorTotal().signum() < 0
        || entrada.quilometragem() == null
        || entrada.quilometragem() < 0) {
      throw new IllegalArgumentException("Evento AbastecimentoRegistrado.v1 inválido.");
    }
  }
}
