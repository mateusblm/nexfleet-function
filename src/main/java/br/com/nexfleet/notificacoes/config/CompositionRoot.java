package br.com.nexfleet.notificacoes.config;

import br.com.nexfleet.notificacoes.notificarabastecimento.application.NotificarAbastecimento;
import br.com.nexfleet.notificacoes.notificarabastecimento.application.ProvedorEmail;
import br.com.nexfleet.notificacoes.notificarabastecimento.infrastructure.AzureCommunicationProvedorEmail;
import br.com.nexfleet.notificacoes.notificarabastecimento.infrastructure.JdbcProvedorEmailCapturado;
import com.azure.communication.email.EmailClientBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.time.Clock;

public final class CompositionRoot {
  private CompositionRoot() {}

  private static final class Holder {
    private static final NotificarAbastecimento CASO_DE_USO = criar();

    private static NotificarAbastecimento criar() {
      var config = new HikariConfig();
      config.setJdbcUrl(obrigatoria("DB_URL"));
      config.setUsername(obrigatoria("DB_USER"));
      config.setPassword(obrigatoria("DB_PASSWORD"));
      config.setMaximumPoolSize(3);
      config.setMinimumIdle(0);
      config.setPoolName("nexfleet-notificacoes");
      var dataSource = new HikariDataSource(config);
      ProvedorEmail provedor = criarProvedor(dataSource);
      return new NotificarAbastecimento(provedor, Clock.systemUTC());
    }

    private static ProvedorEmail criarProvedor(HikariDataSource dataSource) {
      String provider = System.getenv().getOrDefault("EMAIL_PROVIDER", "captured-local");
      if (!"azure-communication".equals(provider)) {
        return new JdbcProvedorEmailCapturado(dataSource);
      }
      var client = new EmailClientBuilder()
          .endpoint(obrigatoria("AZURE_COMMUNICATION_EMAIL_ENDPOINT"))
          .credential(new DefaultAzureCredentialBuilder().build())
          .buildClient();
      return new AzureCommunicationProvedorEmail(
          dataSource,
          client,
          obrigatoria("EMAIL_SENDER_ADDRESS"),
          obrigatoria("EMAIL_RECIPIENT_ADDRESS"));
    }
  }

  public static NotificarAbastecimento notificarAbastecimento() {
    return Holder.CASO_DE_USO;
  }

  private static String obrigatoria(String nome) {
    String valor = System.getenv(nome);
    if (valor == null || valor.isBlank()) {
      throw new IllegalStateException("Configuração obrigatória ausente: " + nome);
    }
    return valor;
  }
}
