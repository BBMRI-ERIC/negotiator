package eu.bbmri_eric.negotiator.unit.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.info_requirement.InformationRequirement;
import eu.bbmri_eric.negotiator.info_submission.InformationSubmission;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class InformationSubmissionTest {
  @Test
  public void testPublicConstructor() {
    InformationRequirement requirement = Mockito.mock(InformationRequirement.class);
    Resource resource = Mockito.mock(Resource.class);
    Negotiation negotiation = Mockito.mock(Negotiation.class);
    String payload = "Test Payload";

    InformationSubmission infoSub =
        new InformationSubmission(requirement, resource, negotiation, payload);
    assertNull(infoSub.getId());
    assertEquals(requirement, infoSub.getRequirement());
    assertEquals(resource, infoSub.getResource());
    assertEquals(negotiation, infoSub.getNegotiation());
    assertEquals(payload, infoSub.getPayload());
  }

  @Test
  public void testSettersAndGetters() {
    InformationRequirement requirement = Mockito.mock(InformationRequirement.class);
    Resource resource = Mockito.mock(Resource.class);
    Negotiation negotiation = Mockito.mock(Negotiation.class);
    String payload = "Test Payload";

    InformationSubmission infoSub =
        new InformationSubmission(requirement, resource, negotiation, payload);
    Long id = 1L;

    infoSub.setId(id);
    infoSub.setRequirement(requirement);
    infoSub.setResource(resource);
    infoSub.setNegotiation(negotiation);
    infoSub.setPayload(payload);

    assertEquals(id, infoSub.getId());
    assertEquals(requirement, infoSub.getRequirement());
    assertEquals(resource, infoSub.getResource());
    assertEquals(negotiation, infoSub.getNegotiation());
    assertEquals(payload, infoSub.getPayload());
  }
}
