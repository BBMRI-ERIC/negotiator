package eu.bbmri.eric.csit.service.negotiator.unit.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.bbmri.eric.csit.service.negotiator.configuration.state_machine.resource.NegotiationResourceState;
import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationResourceLifecycleRecord;
import eu.bbmri.eric.csit.service.negotiator.database.model.Resource;
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
