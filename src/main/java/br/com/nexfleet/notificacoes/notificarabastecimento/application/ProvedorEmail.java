package br.com.nexfleet.notificacoes.notificarabastecimento.application;

import java.time.Instant;
import java.util.UUID;

public interface ProvedorEmail {
  record Email(
      UUID eventoId,
      UUID abastecimentoId,
      String assunto,
      String corpo,
      Instant solicitadoEm) {}

  boolean enviarSeAusente(Email email);
}
