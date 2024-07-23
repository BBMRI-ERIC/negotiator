package eu.bbmri_eric.negotiator.unit.model;

import eu.bbmri_eric.negotiator.configuration.state_machine.resource.NegotiationResourceState;
import eu.bbmri_eric.negotiator.database.model.AccessForm;
import eu.bbmri_eric.negotiator.database.model.InformationRequirement;
import org.junit.jupiter.api.Test;

public class InformationRequirementTest {

  @Test
  void createInformationRequirement_correctParameters_ok() {
    AccessForm accessForm = new AccessForm("test");
    new InformationRequirement(accessForm, NegotiationResourceState.SUBMITTED);
  }
}
