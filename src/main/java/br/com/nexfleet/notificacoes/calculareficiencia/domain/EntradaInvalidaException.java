package br.com.nexfleet.notificacoes.calculareficiencia.domain;

/** Entrada do cálculo de eficiência que viola as regras do domínio. */
public final class EntradaInvalidaException extends RuntimeException {
  public EntradaInvalidaException(String mensagem) {
    super(mensagem);
  }
}
