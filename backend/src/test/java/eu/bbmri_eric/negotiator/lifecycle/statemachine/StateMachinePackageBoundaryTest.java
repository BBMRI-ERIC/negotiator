package eu.bbmri_eric.negotiator.lifecycle.statemachine;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideOutsideOfPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

class StateMachinePackageBoundaryTest {

  @Test
  void statemachinePackage_hasNoDomainImports() {
    JavaClasses classes =
        new ClassFileImporter().importPackages("eu.bbmri_eric.negotiator.lifecycle.statemachine");

    ArchRule rule =
        classes()
            .that()
            .resideInAPackage("eu.bbmri_eric.negotiator.lifecycle.statemachine..")
            .should()
            .onlyDependOnClassesThat(
                resideInAPackage("eu.bbmri_eric.negotiator.lifecycle.statemachine..")
                    .or(resideOutsideOfPackage("eu.bbmri_eric.negotiator..")));

    rule.check(classes);
  }
}
