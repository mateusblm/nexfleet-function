package br.com.nexfleet.notificacoes.notificarabastecimento.infrastructure;

import br.com.nexfleet.notificacoes.notificarabastecimento.application.ProvedorEmail.Email;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import javax.sql.DataSource;

/**
 * Grava a linha de {@code notificacoes.email_capturado} uma única vez por {@code eventoId}.
 *
 * <p>O {@code UPDLOCK, HOLDLOCK} serializa duas entregas simultâneas do mesmo evento: a segunda
 * espera a primeira e insere 0 linhas. Se ainda assim a chave colidir (2627/2601), o evento já
 * existe; a perdedora de um deadlock (1205) tenta de novo.
 */
final class ReservaIdempotente {
  private static final int TENTATIVAS = 3;
  private static final int DEADLOCK = 1205;
  private static final int VIOLACAO_PK = 2627;
  private static final int VIOLACAO_UNICA = 2601;

  private ReservaIdempotente() {}

  /** Devolve {@code true} se esta chamada gravou a linha; {@code false} se o evento já existia. */
  static boolean reservar(
      DataSource dataSource, Email email, String status, String destinatario, String provedor)
      throws SQLException {
    String sql = """
        INSERT INTO notificacoes.email_capturado
          (evento_id, abastecimento_id, assunto, corpo, status, solicitado_em,
           destinatario, provedor)
        SELECT ?, ?, ?, ?, ?, ?, ?, ?
         WHERE NOT EXISTS (
           SELECT 1 FROM notificacoes.email_capturado WITH (UPDLOCK, HOLDLOCK)
            WHERE evento_id = ?)
        """;
    for (int tentativa = 1; ; tentativa++) {
      try (var conexao = dataSource.getConnection(); var comando = conexao.prepareStatement(sql)) {
        comando.setObject(1, email.eventoId());
        comando.setObject(2, email.abastecimentoId());
        comando.setString(3, email.assunto());
        comando.setString(4, email.corpo());
        comando.setString(5, status);
        comando.setObject(6, OffsetDateTime.ofInstant(email.solicitadoEm(), ZoneOffset.UTC));
        comando.setString(7, destinatario);
        comando.setString(8, provedor);
        comando.setObject(9, email.eventoId());
        return comando.executeUpdate() == 1;
      } catch (SQLException falha) {
        if (falha.getErrorCode() == VIOLACAO_PK || falha.getErrorCode() == VIOLACAO_UNICA) {
          return false;
        }
        if (falha.getErrorCode() != DEADLOCK || tentativa >= TENTATIVAS) {
          throw falha;
        }
      }
    }
  }
}
