package br.com.nexfleet.notificacoes.calculareficiencia.domain;

import java.math.BigDecimal;

public record Manutencao(BigDecimal valor) {
  public Manutencao {
    Valores.naoNegativo(valor, "valor da manutenção");
  }
}
