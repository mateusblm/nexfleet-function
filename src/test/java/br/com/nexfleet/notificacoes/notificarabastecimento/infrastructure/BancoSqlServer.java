package br.com.nexfleet.notificacoes.notificarabastecimento.infrastructure;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.testcontainers.mssqlserver.MSSQLServerContainer;

/** SQL Server 2022 efêmero compartilhado pelos ITs; cada chamada cria um banco vazio. */
final class BancoSqlServer {
  static final MSSQLServerContainer SQL_SERVER = iniciar();

  private BancoSqlServer() {}

  private static MSSQLServerContainer iniciar() {
    var container = new MSSQLServerContainer("mcr.microsoft.com/mssql/server:2022-latest")
        .acceptLicense();
    container.start();
    return container;
  }

  /** Simula o bootstrap do admin: banco novo com o schema criado antes do Flyway. */
  static String bancoVazio(String nome) {
    try (var admin = DriverManager.getConnection(
            SQL_SERVER.getJdbcUrl(), SQL_SERVER.getUsername(), SQL_SERVER.getPassword());
        var comando = admin.createStatement()) {
      comando.execute("CREATE DATABASE " + nome);
    } catch (SQLException falha) {
      throw new IllegalStateException(falha);
    }
    String url = SQL_SERVER.getJdbcUrl() + ";databaseName=" + nome;
    try (var conexao = DriverManager.getConnection(
            url, SQL_SERVER.getUsername(), SQL_SERVER.getPassword());
        var comando = conexao.createStatement()) {
      comando.execute("CREATE SCHEMA notificacoes");
    } catch (SQLException falha) {
      throw new IllegalStateException(falha);
    }
    return url;
  }

  static MigrateResult migrar(String url) {
    return Flyway.configure()
        .dataSource(url, SQL_SERVER.getUsername(), SQL_SERVER.getPassword())
        .schemas("notificacoes")
        .createSchemas(false)
        .load()
        .migrate();
  }

  static HikariDataSource dataSource(String url) {
    var config = new HikariConfig();
    config.setJdbcUrl(url);
    config.setUsername(SQL_SERVER.getUsername());
    config.setPassword(SQL_SERVER.getPassword());
    config.setMaximumPoolSize(4);
    return new HikariDataSource(config);
  }
}
