package br.com.nexfleet.notificacoes.notificarabastecimento.infrastructure;

import br.com.nexfleet.notificacoes.notificarabastecimento.application.ProvedorEmail;
import com.azure.communication.email.EmailClient;
import com.azure.communication.email.models.EmailMessage;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;
import javax.sql.DataSource;

public final class AzureCommunicationProvedorEmail implements ProvedorEmail {
  private final DataSource dataSource;
  private final EmailClient client;
  private final String remetente;
  private final String destinatario;

  public AzureCommunicationProvedorEmail(
      DataSource dataSource,
      EmailClient client,
      String remetente,
      String destinatario) {
    this.dataSource = Objects.requireNonNull(dataSource);
    this.client = Objects.requireNonNull(client);
    this.remetente = obrigatorio(remetente, "EMAIL_SENDER_ADDRESS");
    this.destinatario = obrigatorio(destinatario, "EMAIL_RECIPIENT_ADDRESS");
  }

  @Override
  public boolean enviarSeAusente(Email email) {
    if (!reservar(email)) {
      return false;
    }
    String operationId;
    try {
      EmailMessage message = new EmailMessage()
          .setSenderAddress(remetente)
          .setToRecipients(destinatario)
          .setSubject(email.assunto())
          .setBodyPlainText(email.corpo());
      operationId = client.beginSend(message).waitForCompletion().getValue().getId();
    } catch (RuntimeException falha) {
      liberarReserva(email.eventoId());
      throw new IllegalStateException("Não foi possível enviar o e-mail pelo provedor Azure.", falha);
    }
    // Depois que o provedor aceitou a mensagem, nunca apagamos a reserva: isso
    // evita reenvio duplicado se apenas a confirmação no PostgreSQL falhar.
    marcarEnviado(email.eventoId(), operationId, email.solicitadoEm());
    return true;
  }

  private boolean reservar(Email email) {
    String sql = """
        INSERT INTO notificacoes.email_capturado
          (evento_id, abastecimento_id, assunto, corpo, status, solicitado_em,
           destinatario, provedor)
        VALUES (?, ?, ?, ?, 'PENDENTE', ?, ?, 'AZURE_COMMUNICATION_SERVICES')
        ON CONFLICT (evento_id) DO NOTHING
        """;
    try (var conexao = dataSource.getConnection(); var comando = conexao.prepareStatement(sql)) {
      comando.setObject(1, email.eventoId());
      comando.setObject(2, email.abastecimentoId());
      comando.setString(3, email.assunto());
      comando.setString(4, email.corpo());
      comando.setObject(5, OffsetDateTime.ofInstant(email.solicitadoEm(), ZoneOffset.UTC));
      comando.setString(6, destinatario);
      return comando.executeUpdate() == 1;
    } catch (SQLException falha) {
      throw new IllegalStateException("Não foi possível reservar o envio de e-mail.", falha);
    }
  }

  private void marcarEnviado(UUID eventoId, String operationId, java.time.Instant enviadoEm) {
    String sql = """
        UPDATE notificacoes.email_capturado
           SET status = 'ENVIADO', provedor_mensagem_id = ?, enviado_em = ?
         WHERE evento_id = ? AND status = 'PENDENTE'
        """;
    try (var conexao = dataSource.getConnection(); var comando = conexao.prepareStatement(sql)) {
      comando.setString(1, operationId);
      comando.setObject(2, OffsetDateTime.ofInstant(enviadoEm, ZoneOffset.UTC));
      comando.setObject(3, eventoId);
      if (comando.executeUpdate() != 1) {
        throw new IllegalStateException("Reserva do e-mail não encontrada após o envio.");
      }
    } catch (SQLException falha) {
      throw new IllegalStateException("Não foi possível confirmar o envio de e-mail.", falha);
    }
  }

  private void liberarReserva(UUID eventoId) {
    String sql = "DELETE FROM notificacoes.email_capturado WHERE evento_id = ? AND status = 'PENDENTE'";
    try (var conexao = dataSource.getConnection(); var comando = conexao.prepareStatement(sql)) {
      comando.setObject(1, eventoId);
      comando.executeUpdate();
    } catch (SQLException ignored) {
      // A exceção original do provedor é preservada; uma entrega repetida continua idempotente.
    }
  }

  private static String obrigatorio(String valor, String nome) {
    if (valor == null || valor.isBlank()) {
      throw new IllegalStateException("Configuração obrigatória ausente: " + nome);
    }
    return valor;
  }
}
