package eu.bbmri.eric.csit.service.negotiator.unit.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.bbmri.eric.csit.service.negotiator.configuration.state_machine.negotiation.NegotiationState;
import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationLifecycleRecord;
import org.junit.jupiter.api.Test;

public class NegotiationLifecycleRecordTest {
  @Test
  void build_allParameters_OK() {
    NegotiationLifecycleRecord negotiationLifecycleRecord =
        NegotiationLifecycleRecord.builder().changedTo(NegotiationState.APPROVED).build();
    assertEquals(NegotiationState.APPROVED, negotiationLifecycleRecord.getChangedTo());
  }
}
