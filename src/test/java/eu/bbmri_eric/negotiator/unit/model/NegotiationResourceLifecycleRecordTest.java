package eu.bbmri_eric.negotiator.unit.model;

import static org.junit.jupiter.api.Assertions.*;

import eu.bbmri_eric.negotiator.configuration.state_machine.resource.NegotiationResourceState;
import eu.bbmri_eric.negotiator.database.model.Negotiation;
import eu.bbmri_eric.negotiator.database.model.NegotiationResourceLifecycleRecord;
import eu.bbmri_eric.negotiator.database.model.Resource;
import org.junit.jupiter.api.Test;

public class NegotiationResourceLifecycleRecordTest {

  @Test
  void buildAllParametersOK() {
    Resource resource = new Resource();
    Negotiation negotiation = new Negotiation();

    NegotiationResourceLifecycleRecord negotiationResourceLifecycleRecord =
        NegotiationResourceLifecycleRecord.builder()
            .resource(resource)
            .negotiation(negotiation)
            .changedTo(NegotiationResourceState.RESOURCE_UNAVAILABLE)
            .build();
    assertSame(negotiation, negotiationResourceLifecycleRecord.getNegotiation());
    assertSame(resource, negotiationResourceLifecycleRecord.getResource());
    assertEquals(
        NegotiationResourceState.RESOURCE_UNAVAILABLE,
        negotiationResourceLifecycleRecord.getChangedTo());
  }
}
