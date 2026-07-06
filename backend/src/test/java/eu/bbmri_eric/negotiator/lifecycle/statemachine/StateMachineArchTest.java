package eu.bbmri_eric.negotiator.lifecycle.statemachine;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

class StateMachineArchTest {

  private static final JavaClasses CLASSES =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages("eu.bbmri_eric.negotiator");

  @Test
  void statemachinePackage_hasNoDomainImports() {
    ArchRule rule =
        classes()
            .that()
            .resideInAPackage("eu.bbmri_eric.negotiator.lifecycle.statemachine..")
            .should()
            .onlyDependOnClassesThat()
            .resideInAnyPackage(
                "eu.bbmri_eric.negotiator.lifecycle.statemachine..",
                "com.github.oxo42.stateless4j..",
                "java..",
                "javax..",
                "jakarta..",
                "org.springframework..",
                "lombok..");

    rule.check(CLASSES);
  }
}
