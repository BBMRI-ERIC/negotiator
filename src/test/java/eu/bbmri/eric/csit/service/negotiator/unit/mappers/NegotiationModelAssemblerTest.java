package eu.bbmri.eric.csit.service.negotiator.unit.mappers;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import eu.bbmri.eric.csit.service.negotiator.dto.negotiation.NegotiationDTO;
import eu.bbmri.eric.csit.service.negotiator.mappers.NegotiationModelAssembler;
import org.junit.jupiter.api.Test;

public class NegotiationModelAssemblerTest {
  NegotiationModelAssembler negotiationModelAssembler = new NegotiationModelAssembler();

  @Test
  void toModel_null_throwsIllegalArg() {
    assertThrows(IllegalArgumentException.class, () -> negotiationModelAssembler.toModel(null));
  }

  @Test
  void toModel_emptyEntity_ok() {
    assertNotNull(negotiationModelAssembler.toModel(new NegotiationDTO()));
    assertNull(negotiationModelAssembler.toModel(new NegotiationDTO()).getContent().getId());
  }
}
