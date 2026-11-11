package br.com.nexfleet.notificacoes.notificarabastecimento.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MigracaoSqlServerIT {
  @Test
  void aplicaAsMigracoesEmBancoVazioUmaUnicaVez() {
    String url = BancoSqlServer.bancoVazio("notificacoes_migracao");

    var primeira = BancoSqlServer.migrar(url);
    var segunda = BancoSqlServer.migrar(url);

    assertThat(primeira.success).isTrue();
    assertThat(primeira.migrationsExecuted).isEqualTo(1);
    assertThat(primeira.targetSchemaVersion).isEqualTo("1");
    assertThat(segunda.migrationsExecuted).isZero();
  }
}
