package br.com.nexfleet.notificacoes.calculareficiencia.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record Abastecimento(Instant data, BigDecimal litros, BigDecimal valor, Long quilometragem) {}
