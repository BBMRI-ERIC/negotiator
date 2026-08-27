package eu.bbmri_eric.negotiator.unit.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.bbmri_eric.negotiator.discovery.DiscoveryService;
import eu.bbmri_eric.negotiator.governance.organization.Organization;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.integration.api.v3.TestUtils;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.dto.NegotiationCreateDTO;
import eu.bbmri_eric.negotiator.negotiation.dto.NegotiationDTO;
import eu.bbmri_eric.negotiator.negotiation.mappers.NegotiationModelMapper;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
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
            .currentState("SUBMITTED")
            .build();
    negotiation.setCreationDate(LocalDateTime.of(2023, Month.SEPTEMBER, 19, 00, 00));
    negotiation.setStateForResource("collection:1", "SUBMITTED");
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
    negotiation.setStateForResource("collection:1", "SUBMITTED");
  }

  @Test
  void map_absentCurrentState_statusIsEmptyString() {
    Negotiation negotiation = Negotiation.builder().humanReadable("#1 Material Type: DNA").build();
    NegotiationDTO negotiationDTO = this.mapper.map(negotiation, NegotiationDTO.class);
    assertEquals("", negotiationDTO.getStatus());
  }

  /**
   * The payload-updatable rule decides whether the Negotiation carries an {@code Update} link, and
   * it is a comparison against three State names rather than a lookup. Pinned over the whole closed
   * set while that set still exists, so that a name changing meaning shows up as a failure here.
   */
  @Test
  void payloadUpdatable_admitsExactlyDraftSubmittedAndInProgress() {
    Set<String> updatable =
        Arrays.stream(NegotiationState.values())
            .map(NegotiationState::name)
            .filter(name -> NegotiationDTO.builder().status(name).build().isPayloadUpdatable())
            .collect(Collectors.toSet());
    assertEquals(Set.of("DRAFT", "SUBMITTED", "IN_PROGRESS"), updatable);
  }

  @Test
  void payloadUpdatable_unknownOrAbsentStatus_isFalse() {
    assertFalse(NegotiationDTO.builder().status("UNKNOWN").build().isPayloadUpdatable());
    assertFalse(NegotiationDTO.builder().build().isPayloadUpdatable());
  }

  @Test
  void map_fromNegotiationDTO_notDraft_Ok() throws IOException {
    NegotiationCreateDTO negotiationCreateDTO = TestUtils.createNegotiation("requestID", false);

    Negotiation negotiation = this.mapper.map(negotiationCreateDTO, Negotiation.class);
    assertEquals("SUBMITTED", negotiation.getCurrentState());
    assertTrue(negotiation.isPublicPostsEnabled());
  }

  @Test
  void map_stateFromNegotiationDTO_Draft_Ok() throws IOException {
    NegotiationCreateDTO negotiationCreateDTO = TestUtils.createNegotiation("requestID", true);

    Negotiation negotiation = this.mapper.map(negotiationCreateDTO, Negotiation.class);
    assertEquals("DRAFT", negotiation.getCurrentState());
    assertFalse(negotiation.isPublicPostsEnabled());
  }
}
