package br.com.nexfleet.notificacoes.calculareficiencia.domain;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Veículo com seu histórico; listas ausentes valem como vazias. */
public record Veiculo(
    UUID veiculoId, String placa, List<Abastecimento> abastecimentos, List<Manutencao> manutencoes) {
  public Veiculo {
    if (veiculoId == null) {
      throw new EntradaInvalidaException("Veículo sem veiculoId.");
    }
    abastecimentos = semNulos(abastecimentos);
    manutencoes = semNulos(manutencoes);
  }

  /** Quantidade de itens que este veículo soma ao limite da requisição. */
  public int itens() {
    return 1 + abastecimentos.size() + manutencoes.size();
  }

  private static <T> List<T> semNulos(List<T> itens) {
    if (itens == null) {
      return List.of();
    }
    if (itens.stream().anyMatch(Objects::isNull)) {
      throw new EntradaInvalidaException("Lista com item nulo.");
    }
    return List.copyOf(itens);
  }
}
