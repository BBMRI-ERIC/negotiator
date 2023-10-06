package eu.bbmri.eric.csit.service.negotiator.integration;

import static org.junit.jupiter.api.Assertions.*;

import eu.bbmri.eric.csit.service.negotiator.NegotiatorApplication;
import eu.bbmri.eric.csit.service.negotiator.configuration.state_machine.negotiation.NegotiationEvent;
import eu.bbmri.eric.csit.service.negotiator.configuration.state_machine.negotiation.NegotiationState;
import eu.bbmri.eric.csit.service.negotiator.configuration.state_machine.resource.NegotiationResourceEvent;
import eu.bbmri.eric.csit.service.negotiator.configuration.state_machine.resource.NegotiationResourceState;
import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationLifecycleRecord;
import eu.bbmri.eric.csit.service.negotiator.database.repository.NegotiationRepository;
import eu.bbmri.eric.csit.service.negotiator.dto.negotiation.NegotiationCreateDTO;
import eu.bbmri.eric.csit.service.negotiator.dto.negotiation.NegotiationDTO;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri.eric.csit.service.negotiator.integration.api.v3.TestUtils;
import eu.bbmri.eric.csit.service.negotiator.service.NegotiationLifecycleServiceImpl;
import eu.bbmri.eric.csit.service.negotiator.service.NegotiationService;
import eu.bbmri.eric.csit.service.negotiator.service.ResourceLifecycleService;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(classes = NegotiatorApplication.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class NegotiationLifecycleServiceImplTest {

  @Autowired NegotiationLifecycleServiceImpl negotiationLifecycleService;
  @Autowired ResourceLifecycleService resourceLifecycleService;
  @Autowired NegotiationRepository negotiationRepository;
  @Autowired NegotiationService negotiationService;
  @Autowired private WebApplicationContext context;

  @Test
  void getState_createNegotiation_isSubmitted() throws IOException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    assertEquals(NegotiationState.SUBMITTED, NegotiationState.valueOf(negotiationDTO.getStatus()));
  }

  @Test
  @WithMockUser(authorities = "ROLE_ADMIN")
  public void getPossibleEvents_existingNegotiationAndIsAdmin_Ok() throws IOException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    assertEquals(
        Set.of(NegotiationEvent.APPROVE, NegotiationEvent.DECLINE),
        negotiationLifecycleService.getPossibleEvents(negotiationDTO.getId()));
  }

  @Test
  public void getPossibleEvents_nonExistentId_throwsEntityNotFoundException() {
    assertThrows(
        EntityNotFoundException.class,
        () -> negotiationLifecycleService.getPossibleEvents("fakeId"));
  }

  @Test
  @WithUserDetails("researcher")
  public void getPossibleEvents_existingIdNotAdmin_returnsEmptySet() throws IOException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    assertEquals(Set.of(), negotiationLifecycleService.getPossibleEvents(negotiationDTO.getId()));
  }

  @Test
  @WithMockUser(authorities = "ROLE_ADMIN")
  void sendEvent_approveNewNegotiation_isOngoing() throws IOException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    assertEquals(
        NegotiationState.ONGOING,
        negotiationLifecycleService.sendEvent(negotiationDTO.getId(), NegotiationEvent.APPROVE));
    assertEquals(
        NegotiationState.ONGOING,
        NegotiationState.valueOf(
            negotiationService.findById(negotiationDTO.getId(), false).getStatus()));
  }

  private NegotiationDTO saveNegotiation() throws IOException {
    NegotiationCreateDTO negotiationCreateDTO = TestUtils.createNegotiation(Set.of("request-2"));
    return negotiationService.create(negotiationCreateDTO, 101L);
  }

  @Test
  void sendEvent_wrongEvent_noChangeInState() throws IOException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    assertEquals(
        NegotiationState.SUBMITTED,
        negotiationLifecycleService.sendEvent(negotiationDTO.getId(), NegotiationEvent.ABANDON));
    assertEquals(
        NegotiationState.SUBMITTED,
        NegotiationState.valueOf(
            negotiationService.findById(negotiationDTO.getId(), false).getStatus()));
  }

  @Test
  @WithMockUser(authorities = "ROLE_ADMIN")
  void sendEvent_approveCorrectly_calledActionEnablePost() throws IOException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    assertFalse(negotiationService.findById(negotiationDTO.getId(), false).getPostsEnabled());
    assertEquals(
        NegotiationState.ONGOING,
        negotiationLifecycleService.sendEvent(negotiationDTO.getId(), NegotiationEvent.APPROVE));
    assertTrue(negotiationService.findById(negotiationDTO.getId(), false).getPostsEnabled());
  }

  @Test
  @WithMockUser(authorities = "ROLE_ADMIN")
  void sendEvent_approveCorrectly_historyIsUpdated() throws IOException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    negotiationLifecycleService.sendEvent(negotiationDTO.getId(), NegotiationEvent.APPROVE);
    Set<NegotiationLifecycleRecord> history =
        negotiationRepository.findById(negotiationDTO.getId()).get().getLifecycleHistory();
    assertEquals(2, history.size());
    assertTrue(
        history.stream()
            .anyMatch(record -> record.getChangedTo().equals(NegotiationState.ONGOING)));
  }

  @Test
  void createNegotiation_statePerResource_isEmpty() throws IOException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    Map<String, NegotiationResourceState> statePerResource =
        negotiationRepository.findById(negotiationDTO.getId()).get().getCurrentStatePerResource();
    assertTrue(statePerResource.isEmpty());
  }

  @Test
  @WithMockUser(authorities = "ROLE_ADMIN")
  void createNegotiation_approve_eachResourceHasState() throws IOException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    negotiationLifecycleService.sendEvent(negotiationDTO.getId(), NegotiationEvent.APPROVE);
    Map<String, NegotiationResourceState> states =
        negotiationRepository.findById(negotiationDTO.getId()).get().getCurrentStatePerResource();
    assertTrue(states.containsKey("biobank:1:collection:2"));
    assertEquals(NegotiationResourceState.SUBMITTED, states.get("biobank:1:collection:2"));
  }

  @Test
  void sendEventForResource_notApprovedNegotiation_throwsEntityNotFoundException()
      throws IOException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    assertThrows(
        EntityNotFoundException.class,
        () ->
            resourceLifecycleService.sendEvent(
                negotiationDTO.getId(),
                "biobank:1:collection:2",
                NegotiationResourceEvent.CONTACT));
  }

  @Test
  @WithMockUser(authorities = "ROLE_ADMIN")
  void sendEventForResource_approvedNegotiation_Ok() throws IOException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    negotiationLifecycleService.sendEvent(negotiationDTO.getId(), NegotiationEvent.APPROVE);
    assertEquals(
        NegotiationResourceState.SUBMITTED,
        negotiationRepository
            .findById(negotiationDTO.getId())
            .get()
            .getCurrentStatePerResource()
            .get("biobank:1:collection:2"));
    assertEquals(
        NegotiationResourceState.REPRESENTATIVE_CONTACTED,
        resourceLifecycleService.sendEvent(
            negotiationDTO.getId(), "biobank:1:collection:2", NegotiationResourceEvent.CONTACT));
  }

  @Test
  @WithMockUser(authorities = "ROLE_ADMIN")
  void sendEventForResource_approvedNegotiationWrongEvent_noChange() throws IOException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    negotiationLifecycleService.sendEvent(negotiationDTO.getId(), NegotiationEvent.APPROVE);
    assertEquals(
        NegotiationResourceState.SUBMITTED,
        resourceLifecycleService.sendEvent(
            negotiationDTO.getId(),
            "biobank:1:collection:2",
            NegotiationResourceEvent.INDICATE_ACCESS_CONDITIONS));
  }

  @Test
  @WithMockUser(authorities = "ROLE_ADMIN")
  void sendEventForResource_approvedNegotiationMultipleCorrectEvents_ok() throws IOException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    negotiationLifecycleService.sendEvent(negotiationDTO.getId(), NegotiationEvent.APPROVE);
    assertEquals(
        NegotiationResourceState.REPRESENTATIVE_CONTACTED,
        resourceLifecycleService.sendEvent(
            negotiationDTO.getId(), "biobank:1:collection:2", NegotiationResourceEvent.CONTACT));
    assertEquals(
        NegotiationResourceState.CHECKING_AVAILABILITY,
        resourceLifecycleService.sendEvent(
            negotiationDTO.getId(),
            "biobank:1:collection:2",
            NegotiationResourceEvent.MARK_AS_CHECKING_AVAILABILITY));
    assertEquals(
        NegotiationResourceState.RESOURCE_AVAILABLE,
        resourceLifecycleService.sendEvent(
            negotiationDTO.getId(),
            "biobank:1:collection:2",
            NegotiationResourceEvent.MARK_AS_AVAILABLE));
  }

  @Test
  void getCurrentStateForResource_newNegotiation_isNull() throws IOException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    assertNull(
        negotiationService
            .findById(negotiationDTO.getId(), false)
            .getResourceStatus()
            .get("biobank:1:collection:2"));
  }

  @Test
  @WithMockUser(authorities = "ROLE_ADMIN")
  void getCurrentStateForResource_approvedNegotiation_isSubmitted() throws IOException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    negotiationLifecycleService.sendEvent(negotiationDTO.getId(), NegotiationEvent.APPROVE);
    assertEquals(
        NegotiationResourceState.SUBMITTED,
        NegotiationResourceState.valueOf(
            negotiationService
                .findById(negotiationDTO.getId(), false)
                .getResourceStatus()
                .get("biobank:1:collection:2")
                .textValue()));
  }

  @Test
  void getPossibleEventsForResource_nonApprovedNegotiation_throwsEntityNotFound()
      throws IOException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    assertThrows(
        EntityNotFoundException.class,
        () ->
            resourceLifecycleService.getPossibleEvents(
                negotiationDTO.getId(), "biobank:1:collection:2"));
  }

  @Test
  @WithMockUser(authorities = "ROLE_ADMIN")
  void getPossibleEventsForResource_approvedNegotiation_Ok() throws IOException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    negotiationLifecycleService.sendEvent(negotiationDTO.getId(), NegotiationEvent.APPROVE);
    assertEquals(
        Set.of(NegotiationResourceEvent.MARK_AS_UNREACHABLE, NegotiationResourceEvent.CONTACT),
        resourceLifecycleService.getPossibleEvents(
            negotiationDTO.getId(), "biobank:1:collection:2"));
  }

  @Test
  void newNegotiation_findAllWithState_oneWithSubmitted() throws IOException {
    saveNegotiation();
    assertTrue(
        negotiationService.findAllWithCurrentState(NegotiationState.SUBMITTED).stream()
            .allMatch(dto -> Objects.equals(dto.getStatus(), NegotiationState.SUBMITTED.name())));
  }
}
