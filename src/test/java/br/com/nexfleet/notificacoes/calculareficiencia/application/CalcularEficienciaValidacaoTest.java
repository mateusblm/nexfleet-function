package br.com.nexfleet.notificacoes.calculareficiencia.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.nexfleet.notificacoes.calculareficiencia.domain.Abastecimento;
import br.com.nexfleet.notificacoes.calculareficiencia.domain.EntradaInvalidaException;
import br.com.nexfleet.notificacoes.calculareficiencia.domain.Manutencao;
import br.com.nexfleet.notificacoes.calculareficiencia.domain.Veiculo;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CalcularEficienciaValidacaoTest {
  private static final Instant DATA = Instant.parse("2026-10-01T10:00:00Z");

  private final CalcularEficiencia casoDeUso = new CalcularEficiencia();

  @Test
  void rejeitaLitrosNegativos() {
    assertThatThrownBy(() -> new Abastecimento(DATA, new BigDecimal("-1"), BigDecimal.TEN, 100L))
        .isInstanceOf(EntradaInvalidaException.class);
  }

  @Test
  void rejeitaValorDeAbastecimentoNegativo() {
    assertThatThrownBy(() -> new Abastecimento(DATA, BigDecimal.TEN, new BigDecimal("-0.01"), 100L))
        .isInstanceOf(EntradaInvalidaException.class);
  }

  @Test
  void rejeitaQuilometragemNegativa() {
    assertThatThrownBy(() -> new Abastecimento(DATA, BigDecimal.TEN, BigDecimal.TEN, -1L))
        .isInstanceOf(EntradaInvalidaException.class);
  }

  @Test
  void rejeitaValorDeManutencaoNegativo() {
    assertThatThrownBy(() -> new Manutencao(new BigDecimal("-5")))
        .isInstanceOf(EntradaInvalidaException.class);
  }

  @Test
  void rejeitaCamposObrigatoriosAusentes() {
    assertThatThrownBy(() -> new Abastecimento(DATA, null, BigDecimal.TEN, 100L))
        .isInstanceOf(EntradaInvalidaException.class);
    assertThatThrownBy(() -> new Abastecimento(DATA, BigDecimal.TEN, BigDecimal.TEN, null))
        .isInstanceOf(EntradaInvalidaException.class);
    assertThatThrownBy(() -> new Manutencao(null))
        .isInstanceOf(EntradaInvalidaException.class);
    assertThatThrownBy(() -> new Veiculo(null, "ABC1D23", List.of(), List.of()))
        .isInstanceOf(EntradaInvalidaException.class);
  }

  @Test
  void aceitaExatamenteMilItensNoTotal() {
    // 1 veículo + 600 abastecimentos + 399 manutenções = 1.000 itens
    var veiculo = veiculo(600, 399);

    assertThatCode(() -> casoDeUso.executar(List.of(veiculo))).doesNotThrowAnyException();
  }

  @Test
  void rejeitaMaisDeMilItensSomandoVeiculosAbastecimentosEManutencoes() {
    // 2 veículos + 2 * (300 abastecimentos + 200 manutenções) = 1.002 itens
    var veiculos = List.of(veiculo(300, 200), veiculo(300, 200));

    assertThatThrownBy(() -> casoDeUso.executar(veiculos))
        .isInstanceOf(EntradaInvalidaException.class)
        .hasMessageContaining("1000");
  }

  @Test
  void rejeitaListaDeVeiculosAusente() {
    assertThatThrownBy(() -> casoDeUso.executar(null))
        .isInstanceOf(EntradaInvalidaException.class);
  }

  private static Veiculo veiculo(int abastecimentos, int manutencoes) {
    var lista = new java.util.ArrayList<Abastecimento>();
    for (int i = 0; i < abastecimentos; i++) {
      lista.add(new Abastecimento(DATA, BigDecimal.ONE, BigDecimal.ONE, (long) i * 10));
    }
    return new Veiculo(UUID.randomUUID(), "ABC1D23", lista,
        Collections.nCopies(manutencoes, new Manutencao(BigDecimal.ONE)));
  }
}
