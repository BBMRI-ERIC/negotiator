package eu.bbmri_eric.negotiator.unit.model;

import eu.bbmri_eric.negotiator.form.AccessForm;
import eu.bbmri_eric.negotiator.info_requirement.InformationRequirement;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceEvent;
import org.junit.jupiter.api.Test;

public class InformationRequirementTest {

  @Test
  void createInformationRequirement_correctParameters_ok() {
    AccessForm accessForm = new AccessForm("test");
    new InformationRequirement(accessForm, NegotiationResourceEvent.CONTACT);
  }
}
