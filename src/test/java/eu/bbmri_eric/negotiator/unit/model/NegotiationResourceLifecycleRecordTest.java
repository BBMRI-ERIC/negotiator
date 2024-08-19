package eu.bbmri_eric.negotiator.unit.model;

import static org.junit.jupiter.api.Assertions.*;

import eu.bbmri_eric.negotiator.configuration.state_machine.resource.NegotiationResourceState;
import eu.bbmri_eric.negotiator.database.model.NegotiationResourceLifecycleRecord;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
import org.junit.jupiter.api.Test;

public class NegotiationResourceLifecycleRecordTest {

  @Test
  void buildAllParametersOK() {
    Resource resource = new Resource();

    NegotiationResourceLifecycleRecord negotiationResourceLifecycleRecord =
        NegotiationResourceLifecycleRecord.builder()
            .resource(resource)
            .changedTo(NegotiationResourceState.RESOURCE_UNAVAILABLE)
            .build();
    assertSame(resource, negotiationResourceLifecycleRecord.getResource());
    assertEquals(
        NegotiationResourceState.RESOURCE_UNAVAILABLE,
        negotiationResourceLifecycleRecord.getChangedTo());
  }
}
