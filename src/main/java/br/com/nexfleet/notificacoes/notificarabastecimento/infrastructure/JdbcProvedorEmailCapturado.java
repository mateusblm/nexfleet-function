package br.com.nexfleet.notificacoes.notificarabastecimento.infrastructure;

import br.com.nexfleet.notificacoes.notificarabastecimento.application.ProvedorEmail;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import javax.sql.DataSource;

public final class JdbcProvedorEmailCapturado implements ProvedorEmail {
  private final DataSource dataSource;

  public JdbcProvedorEmailCapturado(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public boolean enviarSeAusente(Email email) {
    String sql = """
        INSERT INTO notificacoes.email_capturado
          (evento_id, abastecimento_id, assunto, corpo, status, solicitado_em)
        VALUES (?, ?, ?, ?, 'CAPTURADO_LOCALMENTE', ?)
        ON CONFLICT (evento_id) DO NOTHING
        """;
    try (var conexao = dataSource.getConnection(); var comando = conexao.prepareStatement(sql)) {
      comando.setObject(1, email.eventoId());
      comando.setObject(2, email.abastecimentoId());
      comando.setString(3, email.assunto());
      comando.setString(4, email.corpo());
      comando.setObject(5, OffsetDateTime.ofInstant(email.solicitadoEm(), ZoneOffset.UTC));
      return comando.executeUpdate() == 1;
    } catch (SQLException falha) {
      throw new IllegalStateException("Não foi possível registrar a solicitação de e-mail.", falha);
    }
  }
}
