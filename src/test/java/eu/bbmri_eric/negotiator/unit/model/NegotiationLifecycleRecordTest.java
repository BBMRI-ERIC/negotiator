package eu.bbmri_eric.negotiator.unit.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.bbmri_eric.negotiator.configuration.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.database.model.NegotiationLifecycleRecord;
import org.junit.jupiter.api.Test;

public class NegotiationLifecycleRecordTest {
  @Test
  void build_allParameters_OK() {
    NegotiationLifecycleRecord negotiationLifecycleRecord =
        NegotiationLifecycleRecord.builder().changedTo(NegotiationState.APPROVED).build();
    assertEquals(NegotiationState.APPROVED, negotiationLifecycleRecord.getChangedTo());
  }

  @Test
  void test_abandon_negotiation() {
    NegotiationLifecycleRecord negotiationLifecycleRecord =
        NegotiationLifecycleRecord.builder().changedTo(NegotiationState.ABANDONED).build();
    assertEquals(NegotiationState.ABANDONED, negotiationLifecycleRecord.getChangedTo());
  }
}
