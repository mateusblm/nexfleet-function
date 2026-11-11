package br.com.nexfleet.notificacoes.calculareficiencia.domain;

import java.math.BigDecimal;

final class Valores {
  private Valores() {}

  static void naoNegativo(BigDecimal valor, String campo) {
    if (valor == null || valor.signum() < 0) {
      throw new EntradaInvalidaException("Campo " + campo + " ausente ou negativo.");
    }
  }
}
