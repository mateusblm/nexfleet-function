package br.com.nexfleet.notificacoes.calculareficiencia.domain;

import java.util.List;
import java.util.UUID;

public record Veiculo(
    UUID veiculoId, String placa, List<Abastecimento> abastecimentos, List<Manutencao> manutencoes) {}
