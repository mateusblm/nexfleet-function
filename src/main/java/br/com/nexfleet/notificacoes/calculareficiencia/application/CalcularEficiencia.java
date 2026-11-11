package br.com.nexfleet.notificacoes.calculareficiencia.application;

import br.com.nexfleet.notificacoes.calculareficiencia.domain.CalculadoraEficiencia;
import br.com.nexfleet.notificacoes.calculareficiencia.domain.EficienciaFrota;
import br.com.nexfleet.notificacoes.calculareficiencia.domain.EficienciaVeiculo;
import br.com.nexfleet.notificacoes.calculareficiencia.domain.EntradaInvalidaException;
import br.com.nexfleet.notificacoes.calculareficiencia.domain.Veiculo;
import java.util.List;
import java.util.Objects;

/** Caso de uso sem estado: calcula a eficiência por veículo e da frota. */
public final class CalcularEficiencia {
  /** Teto de itens (veículos + abastecimentos + manutenções) por requisição. */
  public static final int LIMITE_ITENS = 1000;

  public record Resultado(List<EficienciaVeiculo> veiculos, EficienciaFrota frota) {}

  public Resultado executar(List<Veiculo> veiculos) {
    if (veiculos == null || veiculos.stream().anyMatch(Objects::isNull)) {
      throw new EntradaInvalidaException("Lista de veículos ausente ou com item nulo.");
    }
    long itens = veiculos.stream().mapToLong(Veiculo::itens).sum();
    if (itens > LIMITE_ITENS) {
      throw new EntradaInvalidaException(
          "Requisição com " + itens + " itens; o limite é " + LIMITE_ITENS + ".");
    }
    var relatorio = CalculadoraEficiencia.calcular(veiculos);
    return new Resultado(relatorio.veiculos(), relatorio.frota());
  }
}
