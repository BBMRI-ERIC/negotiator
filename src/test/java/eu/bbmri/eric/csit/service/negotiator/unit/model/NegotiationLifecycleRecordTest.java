package eu.bbmri.eric.csit.service.negotiator.unit.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationLifecycleRecord;
import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationState;
import eu.bbmri.eric.csit.service.negotiator.database.model.Person;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

public class NegotiationLifecycleRecordTest {
  @Test
  void build_allParameters_OK() {
    NegotiationLifecycleRecord negotiationLifecycleRecord =
        new NegotiationLifecycleRecord(ZonedDateTime.now(), new Person(), NegotiationState.ONGOING);
    assertEquals(NegotiationState.ONGOING, negotiationLifecycleRecord.newState());
  }
}
