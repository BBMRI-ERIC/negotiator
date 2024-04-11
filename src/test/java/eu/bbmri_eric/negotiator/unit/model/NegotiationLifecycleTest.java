package eu.bbmri_eric.negotiator.unit.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.bbmri_eric.negotiator.configuration.state_machine.negotiation.NegotiationEvent;
import eu.bbmri_eric.negotiator.configuration.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.database.model.NegotiationEventMetadata;
import eu.bbmri_eric.negotiator.database.model.NegotiationStateMetadata;
import eu.bbmri_eric.negotiator.dto.negotiation.NegotiationEventMetadataDto;
import eu.bbmri_eric.negotiator.dto.negotiation.NegotiationStateMetadataDto;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

public class NegotiationLifecycleTest {
  private final ModelMapper mapper = new ModelMapper();

  @Test
  void init_allParameters_ok() {
    new NegotiationStateMetadata(
        NegotiationState.SUBMITTED, "Under review", "Negotiation is currently under review.");
    new NegotiationEventMetadata(
        NegotiationEvent.APPROVE, "Approve", "Approve the submitted negotiation.");
  }

  @Test
  void mapState_ok_ok() {
    NegotiationStateMetadata negotiationStateMetadata =
        new NegotiationStateMetadata(
            NegotiationState.SUBMITTED, "Under review", "Negotiation is currently under review.");
    NegotiationStateMetadataDto dto =
        mapper.map(negotiationStateMetadata, NegotiationStateMetadataDto.class);
    assertEquals(negotiationStateMetadata.getValue(), dto.getValue());
  }

  @Test
  void mapEvent_ok() {
    NegotiationEventMetadata negotiationEventMetadata =
        new NegotiationEventMetadata(
            NegotiationEvent.APPROVE, "Approve", "Approve the submitted negotiation.");
    NegotiationEventMetadataDto dto =
        mapper.map(negotiationEventMetadata, NegotiationEventMetadataDto.class);
    assertEquals(negotiationEventMetadata.getValue(), dto.getValue());
  }
}
