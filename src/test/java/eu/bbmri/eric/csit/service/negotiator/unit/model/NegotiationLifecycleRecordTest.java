package eu.bbmri.eric.csit.service.negotiator.unit.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationLifecycleRecord;
import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationState;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

public class NegotiationLifecycleRecordTest {
  @Test
  void build_allParameters_OK() {
    NegotiationLifecycleRecord negotiationLifecycleRecord =
        NegotiationLifecycleRecord.builder()
            .recordedAt(ZonedDateTime.now())
            .changedTo(NegotiationState.APPROVED)
            .build();
    assertEquals(NegotiationState.APPROVED, negotiationLifecycleRecord.getChangedTo());
  }

  @Test
  void equals_sameId_equal() {
    assertEquals(NegotiationLifecycleRecord.builder()
            .recordedAt(ZonedDateTime.now())
            .changedTo(NegotiationState.APPROVED)
            .id(1L)
            .build(), NegotiationLifecycleRecord.builder()
            .recordedAt(ZonedDateTime.now())
            .changedTo(NegotiationState.SUBMITTED)
            .id(1L)
            .build());
  }

  @Test
  void equals_differentId_equal() {
    assertNotEquals(NegotiationLifecycleRecord.builder()
            .recordedAt(ZonedDateTime.now())
            .changedTo(NegotiationState.APPROVED)
            .id(2L)
            .build(), NegotiationLifecycleRecord.builder()
            .recordedAt(ZonedDateTime.now())
            .changedTo(NegotiationState.APPROVED)
            .id(1L)
            .build());
  }
}