package br.com.nexfleet.notificacoes.calculareficiencia.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Regras de eficiência. Distância = km máximo − km mínimo dos abastecimentos; consumo = distância
 * / litros dos abastecimentos posteriores ao primeiro (por km); custo por km = (abastecimento +
 * manutenção) / distância. Sem distância (menos de dois abastecimentos ou km igual), consumo e
 * custo por km ficam {@code null}. Valores com duas casas, arredondamento HALF_EVEN.
 */
public final class CalculadoraEficiencia {
  private static final int CASAS = 2;
  private static final RoundingMode ARREDONDAMENTO = RoundingMode.HALF_EVEN;

  public record Relatorio(List<EficienciaVeiculo> veiculos, EficienciaFrota frota) {}

  private record Parcial(EficienciaVeiculo eficiencia, BigDecimal litrosConsumidos, BigDecimal gasto) {}

  private CalculadoraEficiencia() {}

  public static Relatorio calcular(List<Veiculo> veiculos) {
    List<EficienciaVeiculo> eficiencias = new ArrayList<>();
    long distanciaFrota = 0;
    BigDecimal litrosFrota = BigDecimal.ZERO;
    BigDecimal gastoRodado = BigDecimal.ZERO;
    BigDecimal gastoFrota = BigDecimal.ZERO;
    for (Veiculo veiculo : veiculos) {
      Parcial parcial = calcular(veiculo);
      eficiencias.add(parcial.eficiencia());
      gastoFrota = gastoFrota.add(parcial.gasto());
      if (parcial.eficiencia().distanciaKm() > 0) {
        distanciaFrota += parcial.eficiencia().distanciaKm();
        litrosFrota = litrosFrota.add(parcial.litrosConsumidos());
        gastoRodado = gastoRodado.add(parcial.gasto());
      }
    }
    var frota = new EficienciaFrota(
        consumo(distanciaFrota, litrosFrota),
        custoPorKm(gastoRodado, distanciaFrota),
        dinheiro(gastoFrota),
        distanciaFrota);
    return new Relatorio(List.copyOf(eficiencias), frota);
  }

  private static Parcial calcular(Veiculo veiculo) {
    List<Abastecimento> porKm = veiculo.abastecimentos().stream()
        .sorted(Comparator.comparingLong(Abastecimento::quilometragem))
        .toList();
    long distancia = porKm.size() < 2
        ? 0
        : porKm.get(porKm.size() - 1).quilometragem() - porKm.get(0).quilometragem();
    BigDecimal litrosConsumidos = porKm.stream().skip(1)
        .map(Abastecimento::litros)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal gastoAbastecimento = porKm.stream()
        .map(Abastecimento::valor)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal gastoManutencao = veiculo.manutencoes().stream()
        .map(Manutencao::valor)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal gasto = gastoAbastecimento.add(gastoManutencao);
    var eficiencia = new EficienciaVeiculo(
        veiculo.veiculoId(),
        veiculo.placa(),
        consumo(distancia, litrosConsumidos),
        custoPorKm(gasto, distancia),
        dinheiro(gastoAbastecimento),
        dinheiro(gastoManutencao),
        dinheiro(gasto),
        distancia);
    return new Parcial(eficiencia, litrosConsumidos, gasto);
  }

  private static BigDecimal consumo(long distancia, BigDecimal litros) {
    if (distancia <= 0 || litros.signum() == 0) {
      return null;
    }
    return BigDecimal.valueOf(distancia).divide(litros, CASAS, ARREDONDAMENTO);
  }

  private static BigDecimal custoPorKm(BigDecimal gasto, long distancia) {
    if (distancia <= 0) {
      return null;
    }
    return gasto.divide(BigDecimal.valueOf(distancia), CASAS, ARREDONDAMENTO);
  }

  private static BigDecimal dinheiro(BigDecimal valor) {
    return valor.setScale(CASAS, ARREDONDAMENTO);
  }
}
