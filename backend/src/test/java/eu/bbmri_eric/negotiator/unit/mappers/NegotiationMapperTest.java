package eu.bbmri_eric.negotiator.unit.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import eu.bbmri_eric.negotiator.discovery.DiscoveryService;
import eu.bbmri_eric.negotiator.governance.organization.Organization;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.integration.api.v3.TestUtils;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.dto.NegotiationCreateDTO;
import eu.bbmri_eric.negotiator.negotiation.dto.NegotiationDTO;
import eu.bbmri_eric.negotiator.negotiation.mappers.NegotiationModelMapper;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceState;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.modelmapper.ModelMapper;

public class NegotiationMapperTest {
  @Spy public ModelMapper mapper = new ModelMapper();

  @InjectMocks
  public NegotiationModelMapper negotiationModelMapper = new NegotiationModelMapper(mapper);

  private static Negotiation buildNegotiation() {
    Set<Resource> resources =
        Set.of(
            Resource.builder()
                .sourceId("collection:1")
                .organization(
                    Organization.builder().name("Test Biobank").externalId("biobank:1").build())
                .discoveryService(DiscoveryService.builder().build())
                .build());
    Negotiation negotiation =
        Negotiation.builder()
            .humanReadable("#1 Material Type: DNA")
            .resources(resources)
            .currentState(NegotiationState.SUBMITTED)
            .build();
    negotiation.setCreationDate(LocalDateTime.of(2023, Month.SEPTEMBER, 19, 00, 00));
    negotiation.setStateForResource("collection:1", NegotiationResourceState.SUBMITTED);
    return negotiation;
  }

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
    this.negotiationModelMapper.addMappings();
  }

  @Test
  void map_NegotiationToDTOid_Ok() {
    Negotiation negotiation = buildNegotiation();
    NegotiationDTO negotiationDTO = this.mapper.map(negotiation, NegotiationDTO.class);
    assertEquals(negotiation.getId(), negotiationDTO.getId());
  }

  @Test
  void map_currentState_ok() {
    Negotiation negotiation = buildNegotiation();
    negotiation.setId("newNegotiation");
    NegotiationDTO negotiationDTO = this.mapper.map(negotiation, NegotiationDTO.class);
    assertEquals("SUBMITTED", negotiationDTO.getStatus());
  }

  @Test
  void map_entityToDtoPayload_throwsRuntimeException() {
    Negotiation negotiation = buildNegotiation();
    negotiation.setPayload("Wrong json string");
    assertThrows(RuntimeException.class, () -> this.mapper.map(negotiation, NegotiationDTO.class));
  }

  @Test
  void map_creationDate_ok() {
    Negotiation negotiation = buildNegotiation();
    negotiation.setId("newNegotiation");
    NegotiationDTO negotiationDTO = this.mapper.map(negotiation, NegotiationDTO.class);
    assertEquals(
        LocalDateTime.of(2023, Month.SEPTEMBER, 19, 00, 00), negotiationDTO.getCreationDate());
  }

  @Test
  void map_statePerResource_Ok() {
    Negotiation negotiation = buildNegotiation();
    negotiation.setStateForResource("collection:1", NegotiationResourceState.SUBMITTED);
  }

  @Test
  void map_fromNegotiationDTO_notDraft_Ok() throws IOException {
    NegotiationCreateDTO negotiationCreateDTO = TestUtils.createNegotiation("requestID", false);

    Negotiation negotiation = this.mapper.map(negotiationCreateDTO, Negotiation.class);
    assertEquals(NegotiationState.SUBMITTED, negotiation.getCurrentState());
  }

  @Test
  void map_stateFromNegotiationDTO_Draft_Ok() throws IOException {
    NegotiationCreateDTO negotiationCreateDTO = TestUtils.createNegotiation("requestID", true);

    Negotiation negotiation = this.mapper.map(negotiationCreateDTO, Negotiation.class);
    assertEquals(NegotiationState.DRAFT, negotiation.getCurrentState());
  }
}
