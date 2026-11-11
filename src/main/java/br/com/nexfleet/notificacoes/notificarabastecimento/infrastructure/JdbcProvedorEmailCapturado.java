package br.com.nexfleet.notificacoes.notificarabastecimento.infrastructure;

import br.com.nexfleet.notificacoes.notificarabastecimento.application.ProvedorEmail;
import java.sql.SQLException;
import javax.sql.DataSource;

public final class JdbcProvedorEmailCapturado implements ProvedorEmail {
  private final DataSource dataSource;

  public JdbcProvedorEmailCapturado(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public boolean enviarSeAusente(Email email) {
    try {
      return ReservaIdempotente.reservar(dataSource, email, "CAPTURADO_LOCALMENTE", null, null);
    } catch (SQLException falha) {
      throw new IllegalStateException("Não foi possível registrar a solicitação de e-mail.", falha);
    }
  }
}
