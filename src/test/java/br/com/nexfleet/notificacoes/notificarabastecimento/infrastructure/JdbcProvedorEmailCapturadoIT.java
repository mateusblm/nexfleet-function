package br.com.nexfleet.notificacoes.notificarabastecimento.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.nexfleet.notificacoes.notificarabastecimento.application.NotificarAbastecimento;
import br.com.nexfleet.notificacoes.notificarabastecimento.application.NotificarAbastecimento.Resultado;
import br.com.nexfleet.notificacoes.notificarabastecimento.application.ProvedorEmail;
import com.zaxxer.hikari.HikariDataSource;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class JdbcProvedorEmailCapturadoIT {
  private static final Instant AGORA = Instant.parse("2026-11-11T12:00:00Z");
  private static HikariDataSource dataSource;

  @BeforeAll
  static void prepararBanco() {
    String url = BancoSqlServer.bancoVazio("notificacoes_email");
    BancoSqlServer.migrar(url);
    dataSource = BancoSqlServer.dataSource(url);
  }

  @AfterAll
  static void fecharBanco() {
    if (dataSource != null) {
      dataSource.close();
    }
  }

  @Test
  void mesmoEventoProcessadoDuasVezesGeraUmUnicoRegistro() throws SQLException {
    var casoDeUso = new NotificarAbastecimento(
        new JdbcProvedorEmailCapturado(dataSource), Clock.fixed(AGORA, ZoneOffset.UTC));
    var evento = evento(UUID.randomUUID());

    var primeira = casoDeUso.executar(evento);
    var segunda = casoDeUso.executar(evento);

    assertThat(primeira).isEqualTo(Resultado.ENVIADA);
    assertThat(segunda).isEqualTo(Resultado.DUPLICADA);
    assertThat(registros(evento.eventoId())).isEqualTo(1);
  }

  @Test
  void mesmoEventoEmDuasExecucoesConcorrentesGeraUmUnicoRegistro() throws Exception {
    var provedor = new JdbcProvedorEmailCapturado(dataSource);
    ExecutorService threads = Executors.newFixedThreadPool(2);
    try {
      for (int rodada = 0; rodada < 25; rodada++) {
        var email = email(UUID.randomUUID());
        var largada = new CyclicBarrier(2);
        List<Future<Boolean>> resultados = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
          resultados.add(threads.submit(() -> {
            largada.await();
            return provedor.enviarSeAusente(email);
          }));
        }

        long enviados = 0;
        for (var resultado : resultados) {
          if (resultado.get()) {
            enviados++;
          }
        }

        assertThat(enviados).as("rodada %d", rodada).isEqualTo(1);
        assertThat(registros(email.eventoId())).as("rodada %d", rodada).isEqualTo(1);
      }
    } finally {
      threads.shutdownNow();
    }
  }

  private static int registros(UUID eventoId) throws SQLException {
    try (var conexao = dataSource.getConnection();
        var comando = conexao.prepareStatement(
            "SELECT COUNT(*) FROM notificacoes.email_capturado WHERE evento_id = ?")) {
      comando.setObject(1, eventoId);
      try (var linhas = comando.executeQuery()) {
        linhas.next();
        return linhas.getInt(1);
      }
    }
  }

  private static ProvedorEmail.Email email(UUID eventoId) {
    return new ProvedorEmail.Email(
        eventoId, UUID.randomUUID(), "Abastecimento registrado no NexFleet",
        "Veículo x: 45.5 L de DIESEL.", AGORA);
  }

  private static NotificarAbastecimento.Entrada evento(UUID eventoId) {
    return new NotificarAbastecimento.Entrada(
        eventoId, UUID.randomUUID(), UUID.randomUUID(), AGORA, "DIESEL",
        new BigDecimal("45.5"), new BigDecimal("320"), 1500L);
  }
}
