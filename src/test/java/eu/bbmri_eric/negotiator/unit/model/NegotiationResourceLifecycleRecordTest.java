package eu.bbmri_eric.negotiator.unit.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.bbmri_eric.negotiator.configuration.state_machine.resource.NegotiationResourceState;
import eu.bbmri_eric.negotiator.database.model.NegotiationResourceLifecycleRecord;
import eu.bbmri_eric.negotiator.database.model.Resource;
import org.junit.jupiter.api.Test;

public class NegotiationResourceLifecycleRecordTest {

  @Test
  void buildAllParametersOK() {
    NegotiationResourceLifecycleRecord negotiationResourceLifecycleRecord =
        NegotiationResourceLifecycleRecord.builder()
            .resource(new Resource())
            .changedTo(NegotiationResourceState.RESOURCE_UNAVAILABLE)
            .build();
    assertEquals(
        NegotiationResourceState.RESOURCE_UNAVAILABLE,
        negotiationResourceLifecycleRecord.getChangedTo());
  }
}
