package br.com.nexfleet.notificacoes.calculareficiencia.domain;

import java.math.BigDecimal;

public record EficienciaFrota(
    BigDecimal consumoMedioKmL, BigDecimal custoPorKm, BigDecimal gastoTotal, long distanciaKm) {}
