package br.com.nexfleet.notificacoes.calculareficiencia.application;

import br.com.nexfleet.notificacoes.calculareficiencia.domain.CalculadoraEficiencia;
import br.com.nexfleet.notificacoes.calculareficiencia.domain.EficienciaFrota;
import br.com.nexfleet.notificacoes.calculareficiencia.domain.EficienciaVeiculo;
import br.com.nexfleet.notificacoes.calculareficiencia.domain.Veiculo;
import java.util.List;

/** Caso de uso sem estado: calcula a eficiência por veículo e da frota. */
public final class CalcularEficiencia {
  public record Resultado(List<EficienciaVeiculo> veiculos, EficienciaFrota frota) {}

  public Resultado executar(List<Veiculo> veiculos) {
    var relatorio = CalculadoraEficiencia.calcular(veiculos);
    return new Resultado(relatorio.veiculos(), relatorio.frota());
  }
}
