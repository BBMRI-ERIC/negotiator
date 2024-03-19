package eu.bbmri_eric.negotiator.unit.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import eu.bbmri_eric.negotiator.dto.negotiation.NegotiationDTO;
import eu.bbmri_eric.negotiator.dto.negotiation.NegotiationFilters;
import eu.bbmri_eric.negotiator.mappers.NegotiationModelAssembler;
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
  void toPagedModel_1Entity_withUser_ok() {
    assertEquals(
        1,
        negotiationModelAssembler
            .toPagedModel(
                new PageImpl<>(List.of(new NegotiationDTO())),
                NegotiationFilters.builder().build(),
                null,
                null,
                1L)
            .getContent()
            .size());
  }

  @Test
  void toPagedModel_1Entity_withoutUser_ok() {
    assertEquals(
        1,
        negotiationModelAssembler
            .toPagedModel(
                new PageImpl<>(List.of(new NegotiationDTO())),
                NegotiationFilters.builder().build(),
                null,
                null)
            .getContent()
            .size());
  }
}
