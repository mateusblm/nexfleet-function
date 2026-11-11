package br.com.nexfleet.notificacoes.calculareficiencia.application;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.nexfleet.notificacoes.calculareficiencia.domain.Abastecimento;
import br.com.nexfleet.notificacoes.calculareficiencia.domain.EficienciaVeiculo;
import br.com.nexfleet.notificacoes.calculareficiencia.domain.Manutencao;
import br.com.nexfleet.notificacoes.calculareficiencia.domain.Veiculo;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CalcularEficienciaTest {
  private static final UUID ID_A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
  private static final UUID ID_B = UUID.fromString("00000000-0000-0000-0000-00000000000b");
  private static final UUID ID_C = UUID.fromString("00000000-0000-0000-0000-00000000000c");

  private final CalcularEficiencia casoDeUso = new CalcularEficiencia();

  @Test
  void calculaConsumoCustoEGastoDoVeiculoOrdenandoAbastecimentosPorQuilometragem() {
    var resultado = casoDeUso.executar(List.of(veiculoA()));

    EficienciaVeiculo a = resultado.veiculos().get(0);
    assertThat(a.veiculoId()).isEqualTo(ID_A);
    assertThat(a.placa()).isEqualTo("ABC1D23");
    assertThat(a.distanciaKm()).isEqualTo(500);
    assertThat(a.consumoMedioKmL()).isEqualTo(new BigDecimal("6.25"));
    assertThat(a.gastoAbastecimento()).isEqualTo(new BigDecimal("600.75"));
    assertThat(a.gastoManutencao()).isEqualTo(new BigDecimal("149.25"));
    assertThat(a.gastoTotal()).isEqualTo(new BigDecimal("750.00"));
    assertThat(a.custoPorKm()).isEqualTo(new BigDecimal("1.50"));
  }

  @Test
  void arredondaConsumoECustoPorKmComDuasCasasHalfEven() {
    var resultado = casoDeUso.executar(List.of(veiculoB()));

    EficienciaVeiculo b = resultado.veiculos().get(0);
    assertThat(b.consumoMedioKmL()).isEqualTo(new BigDecimal("12.50"));
    assertThat(b.custoPorKm()).isEqualTo(new BigDecimal("0.12"));
  }

  @Test
  void veiculoComMenosDeDoisAbastecimentosTemConsumoECustoNulosMasGastoCalculado() {
    var resultado = casoDeUso.executar(List.of(veiculoC()));

    EficienciaVeiculo c = resultado.veiculos().get(0);
    assertThat(c.distanciaKm()).isZero();
    assertThat(c.consumoMedioKmL()).isNull();
    assertThat(c.custoPorKm()).isNull();
    assertThat(c.gastoAbastecimento()).isEqualTo(new BigDecimal("100.00"));
    assertThat(c.gastoManutencao()).isEqualTo(new BigDecimal("30.00"));
    assertThat(c.gastoTotal()).isEqualTo(new BigDecimal("130.00"));
  }

  @Test
  void veiculoComDistanciaZeroTemConsumoECustoNulos() {
    var parado = new Veiculo(ID_C, "PAR0D00", List.of(
        abastecimento("2026-10-01T10:00:00Z", "10", "50.00", 7000),
        abastecimento("2026-10-02T10:00:00Z", "10", "50.00", 7000)), List.of());

    EficienciaVeiculo v = casoDeUso.executar(List.of(parado)).veiculos().get(0);

    assertThat(v.distanciaKm()).isZero();
    assertThat(v.consumoMedioKmL()).isNull();
    assertThat(v.custoPorKm()).isNull();
    assertThat(v.gastoTotal()).isEqualTo(new BigDecimal("100.00"));
  }

  @Test
  void agregaFrotaComVeiculosQueRodaramEGastoTotalDeTodos() {
    var resultado = casoDeUso.executar(List.of(veiculoA(), veiculoB(), veiculoC()));

    assertThat(resultado.veiculos()).extracting(EficienciaVeiculo::veiculoId)
        .containsExactly(ID_A, ID_B, ID_C);
    var frota = resultado.frota();
    assertThat(frota.distanciaKm()).isEqualTo(600);
    assertThat(frota.consumoMedioKmL()).isEqualTo(new BigDecimal("6.82"));
    assertThat(frota.custoPorKm()).isEqualTo(new BigDecimal("1.27"));
    assertThat(frota.gastoTotal()).isEqualTo(new BigDecimal("892.50"));
  }

  @Test
  void frotaSemDistanciaTemConsumoECustoNulos() {
    var frota = casoDeUso.executar(List.of(veiculoC())).frota();

    assertThat(frota.distanciaKm()).isZero();
    assertThat(frota.consumoMedioKmL()).isNull();
    assertThat(frota.custoPorKm()).isNull();
    assertThat(frota.gastoTotal()).isEqualTo(new BigDecimal("130.00"));
  }

  @Test
  void frotaVaziaTemTotaisZeradosEIndicadoresNulos() {
    var resultado = casoDeUso.executar(List.of());

    assertThat(resultado.veiculos()).isEmpty();
    assertThat(resultado.frota().gastoTotal()).isEqualTo(new BigDecimal("0.00"));
    assertThat(resultado.frota().consumoMedioKmL()).isNull();
  }

  static Veiculo veiculoA() {
    return new Veiculo(ID_A, "ABC1D23", List.of(
        abastecimento("2026-10-01T10:00:00Z", "40", "200.00", 10000),
        abastecimento("2026-10-20T10:00:00Z", "50", "250.50", 10500),
        abastecimento("2026-10-10T10:00:00Z", "30", "150.25", 10300)),
        List.of(new Manutencao(new BigDecimal("99.25")), new Manutencao(new BigDecimal("50"))));
  }

  static Veiculo veiculoB() {
    return new Veiculo(ID_B, "BCD2E34", List.of(
        abastecimento("2026-10-01T10:00:00Z", "5", "10.00", 0),
        abastecimento("2026-10-05T10:00:00Z", "8", "2.50", 100)), List.of());
  }

  static Veiculo veiculoC() {
    return new Veiculo(ID_C, "CDE3F45", List.of(
        abastecimento("2026-10-01T10:00:00Z", "20", "100", 5000)),
        List.of(new Manutencao(new BigDecimal("30"))));
  }

  static Abastecimento abastecimento(String data, String litros, String valor, long km) {
    return new Abastecimento(Instant.parse(data), new BigDecimal(litros), new BigDecimal(valor), km);
  }
}
