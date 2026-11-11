package br.com.nexfleet.notificacoes;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

class ArchitectureTest {
  private final com.tngtech.archunit.core.domain.JavaClasses classes =
      new ClassFileImporter()
          .withImportOption(new ImportOption.DoNotIncludeTests())
          .importPackages("br.com.nexfleet.notificacoes");

  @Test
  void aplicacaoNaoDependeDeFrameworkOuAdaptadores() {
    noClasses().that().resideInAPackage("..application..")
        .should().dependOnClassesThat().resideInAnyPackage(
            "com.microsoft..", "com.fasterxml..", "com.zaxxer..", "org.flywaydb..",
            "java.sql..", "..infrastructure..", "..api..", "..config..")
        .check(classes);
  }

  @Test
  void entradaNaoAcessaPersistenciaDiretamente() {
    noClasses().that().resideInAPackage("..api..")
        .should().dependOnClassesThat().resideInAnyPackage(
            "java.sql..", "com.zaxxer..", "org.flywaydb..", "..infrastructure..")
        .check(classes);
  }

  @Test
  void adaptadoresNaoDependemDaEntrada() {
    noClasses().that().resideInAPackage("..infrastructure..")
        .should().dependOnClassesThat().resideInAPackage("..api..")
        .check(classes);
  }
}
