package br.com.nexfleet.notificacoes.calculareficiencia.domain;

import java.math.BigDecimal;
import java.util.UUID;

/** Eficiência de um veículo; consumo e custo por km são {@code null} sem distância mensurável. */
public record EficienciaVeiculo(
    UUID veiculoId,
    String placa,
    BigDecimal consumoMedioKmL,
    BigDecimal custoPorKm,
    BigDecimal gastoAbastecimento,
    BigDecimal gastoManutencao,
    BigDecimal gastoTotal,
    long distanciaKm) {}
