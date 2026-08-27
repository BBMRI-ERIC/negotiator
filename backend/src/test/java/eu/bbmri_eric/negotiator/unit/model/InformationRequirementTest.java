package eu.bbmri_eric.negotiator.unit.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.bbmri_eric.negotiator.form.AccessForm;
import eu.bbmri_eric.negotiator.info_requirement.InformationRequirement;
import org.junit.jupiter.api.Test;

public class InformationRequirementTest {

  @Test
  void createInformationRequirement_correctParameters_ok() {
    AccessForm accessForm = new AccessForm("test");
    new InformationRequirement(accessForm, "CONTACT");
  }

  @Test
  void requirementsWithEqualEventNames_areEqual() {
    AccessForm accessForm = new AccessForm("test");

    InformationRequirement first =
        new InformationRequirement(1L, accessForm, new String("CONTACT"));
    InformationRequirement second =
        new InformationRequirement(1L, accessForm, new String("CONTACT"));

    assertEquals(first, second);
  }
}
