package eu.bbmri_eric.negotiator.lifecycle;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

class FlowableBoundaryTest {

  @Test
  void negotiationDomainPackage_hasNoFlowableEngineImports() {
    JavaClasses classes =
        new ClassFileImporter().importPackages("eu.bbmri_eric.negotiator.negotiation");

    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("eu.bbmri_eric.negotiator.negotiation..")
            .should()
            .dependOnClassesThat(resideInAPackage("org.flowable.."));

    rule.check(classes);
  }
}
