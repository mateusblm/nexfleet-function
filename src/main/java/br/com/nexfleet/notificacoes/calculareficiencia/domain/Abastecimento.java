package br.com.nexfleet.notificacoes.calculareficiencia.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record Abastecimento(Instant data, BigDecimal litros, BigDecimal valor, Long quilometragem) {
  public Abastecimento {
    if (data == null) {
      throw new EntradaInvalidaException("Abastecimento sem data.");
    }
    Valores.naoNegativo(litros, "litros");
    Valores.naoNegativo(valor, "valor");
    if (quilometragem == null || quilometragem < 0) {
      throw new EntradaInvalidaException("Quilometragem ausente ou negativa.");
    }
  }
}
