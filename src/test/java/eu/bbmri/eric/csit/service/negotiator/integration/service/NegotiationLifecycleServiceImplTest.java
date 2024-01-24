package eu.bbmri.eric.csit.service.negotiator.integration.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.bbmri.eric.csit.service.negotiator.NegotiatorApplication;
import eu.bbmri.eric.csit.service.negotiator.configuration.state_machine.negotiation.NegotiationEvent;
import eu.bbmri.eric.csit.service.negotiator.configuration.state_machine.negotiation.NegotiationState;
import eu.bbmri.eric.csit.service.negotiator.configuration.state_machine.resource.NegotiationResourceEvent;
import eu.bbmri.eric.csit.service.negotiator.configuration.state_machine.resource.NegotiationResourceState;
import eu.bbmri.eric.csit.service.negotiator.database.model.Negotiation;
import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationLifecycleRecord;
import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationResourceLifecycleRecord;
import eu.bbmri.eric.csit.service.negotiator.database.repository.NegotiationRepository;
import eu.bbmri.eric.csit.service.negotiator.dto.negotiation.NegotiationCreateDTO;
import eu.bbmri.eric.csit.service.negotiator.dto.negotiation.NegotiationDTO;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri.eric.csit.service.negotiator.integration.api.v3.TestUtils;
import eu.bbmri.eric.csit.service.negotiator.service.NegotiationLifecycleServiceImpl;
import eu.bbmri.eric.csit.service.negotiator.service.NegotiationService;
import eu.bbmri.eric.csit.service.negotiator.service.ResourceLifecycleService;
import eu.bbmri.eric.csit.service.negotiator.unit.context.WithMockNegotiatorUser;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
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
@Transactional
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
        NegotiationState.IN_PROGRESS,
        negotiationLifecycleService.sendEvent(negotiationDTO.getId(), NegotiationEvent.APPROVE));
    assertEquals(
        NegotiationState.IN_PROGRESS,
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
        NegotiationState.IN_PROGRESS,
        negotiationLifecycleService.sendEvent(negotiationDTO.getId(), NegotiationEvent.APPROVE));
    assertTrue(negotiationService.findById(negotiationDTO.getId(), false).getPostsEnabled());
  }

  @Test
  @WithMockUser(authorities = "ROLE_ADMIN")
  void sendEvent_approveCorrectly_historyIsUpdated() throws IOException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    negotiationLifecycleService.sendEvent(negotiationDTO.getId(), NegotiationEvent.APPROVE);
    Set<NegotiationLifecycleRecord> history =
        negotiationRepository.findDetailedById(negotiationDTO.getId()).get().getLifecycleHistory();
    assertEquals(2, history.size());
    assertTrue(
        history.stream()
            .anyMatch(record -> record.getChangedTo().equals(NegotiationState.IN_PROGRESS)));
  }

  @Test
  void createNegotiation_statePerResource_isEmpty() throws IOException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    Map<String, NegotiationResourceState> statePerResource =
        negotiationRepository.findById(negotiationDTO.getId()).get().getCurrentStatePerResource();
    assertTrue(statePerResource.isEmpty());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createNegotiation_approve_eachResourceHasState() throws IOException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    negotiationLifecycleService.sendEvent(negotiationDTO.getId(), NegotiationEvent.APPROVE);
    Map<String, NegotiationResourceState> states =
        negotiationRepository.findById(negotiationDTO.getId()).get().getCurrentStatePerResource();
    assertTrue(states.containsKey("biobank:1:collection:2"));
    assertEquals(
        NegotiationResourceState.REPRESENTATIVE_CONTACTED, states.get("biobank:1:collection:2"));
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
  @WithMockNegotiatorUser(authorities = "ROLE_ADMIN", id = 109L)
  void sendEventForResource_approvedNegotiation_Ok() throws IOException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    negotiationLifecycleService.sendEvent(negotiationDTO.getId(), NegotiationEvent.APPROVE);
    assertEquals(
        NegotiationResourceState.REPRESENTATIVE_CONTACTED,
        resourceLifecycleService.sendEvent(
            negotiationDTO.getId(), "biobank:1:collection:2", NegotiationResourceEvent.CONTACT));
    // check that the negotition has a resource record with the assigned state
    Negotiation negotiation = negotiationRepository.findDetailedById(negotiationDTO.getId()).get();
    Set<NegotiationResourceLifecycleRecord> records =
        negotiation.getNegotiationResourceLifecycleRecords();
    Assertions.assertNotNull(
        records.stream()
            .filter(
                r ->
                    r.getChangedTo()
                        .equals(NegotiationResourceState.valueOf("REPRESENTATIVE_CONTACTED")))
            .findFirst());
  }

  @Test
  @WithMockNegotiatorUser(authorities = "ROLE_ADMIN", id = 109L)
  void sendEventForResource_approvedNegotiationWrongEvent_noChange() throws IOException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    negotiationLifecycleService.sendEvent(negotiationDTO.getId(), NegotiationEvent.APPROVE);
    assertEquals(
        NegotiationResourceState.REPRESENTATIVE_CONTACTED,
        resourceLifecycleService.sendEvent(
            negotiationDTO.getId(),
            "biobank:1:collection:2",
            NegotiationResourceEvent.INDICATE_ACCESS_CONDITIONS));
  }

  @Test
  @WithMockNegotiatorUser(authorities = "ROLE_ADMIN", id = 109L)
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
    Negotiation negotiation = negotiationRepository.findDetailedById(negotiationDTO.getId()).get();
    Set<NegotiationResourceLifecycleRecord> records =
        negotiation.getNegotiationResourceLifecycleRecords();
    assertEquals(4, records.size());
  }

  @Test
  void getCurrentStateForResource_newNegotiation_isNull() throws IOException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    assertNull(
        negotiationService
            .findById(negotiationDTO.getId(), false)
            .getStatusForResource("biobank:1:collection:2"));
  }

  @Test
  @WithMockUser(authorities = {"ROLE_ADMIN", "ROLE_REPRESENTATIVE_biobank:1:collection:2"})
  void getCurrentStateForResource_approvedNegotiation_isSubmitted() throws IOException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    negotiationLifecycleService.sendEvent(negotiationDTO.getId(), NegotiationEvent.APPROVE);
    assertEquals(
        NegotiationResourceState.REPRESENTATIVE_CONTACTED,
        NegotiationResourceState.valueOf(
            negotiationService
                .findById(negotiationDTO.getId(), false)
                .getStatusForResource("biobank:1:collection:2")));
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
  @WithMockNegotiatorUser(authorities = "ROLE_ADMIN", id = 109L)
  void getPossibleEventsForResource_approvedNegotiation_Ok() throws IOException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    negotiationLifecycleService.sendEvent(negotiationDTO.getId(), NegotiationEvent.APPROVE);
    assertEquals(
        Set.of(
            NegotiationResourceEvent.STEP_AWAY,
            NegotiationResourceEvent.MARK_AS_CHECKING_AVAILABILITY),
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
