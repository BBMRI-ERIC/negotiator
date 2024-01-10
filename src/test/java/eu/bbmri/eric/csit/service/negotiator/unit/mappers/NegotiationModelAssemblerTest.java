package eu.bbmri.eric.csit.service.negotiator.unit.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import eu.bbmri.eric.csit.service.negotiator.dto.negotiation.NegotiationDTO;
import eu.bbmri.eric.csit.service.negotiator.mappers.NegotiationModelAssembler;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;

public class NegotiationModelAssemblerTest {
  NegotiationModelAssembler negotiationModelAssembler = new NegotiationModelAssembler();

  @Test
  void toModel_null_nullPointer() {
    assertThrows(NullPointerException.class, () -> negotiationModelAssembler.toModel(null));
  }

  @Test
  void toModel_validEntity_ok() {
    assertNotNull(negotiationModelAssembler.toModel(new NegotiationDTO()));
    assertNull(negotiationModelAssembler.toModel(new NegotiationDTO()).getContent().getId());
    assertEquals(
        "1",
        negotiationModelAssembler
            .toModel(NegotiationDTO.builder().id("1").build())
            .getContent()
            .getId());
    assertEquals(
        "/v3/negotiations",
        negotiationModelAssembler
            .toModel(new NegotiationDTO())
            .getLink("negotiations")
            .get()
            .getHref());
  }

  @Test
  void toPagedModel_1Entity_ok() {
    assertEquals(
        1,
        negotiationModelAssembler
            .toPagedModel(new PageImpl<>(List.of(new NegotiationDTO())))
            .getContent()
            .size());
  }
}
