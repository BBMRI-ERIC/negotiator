package eu.bbmri_eric.negotiator.integration.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.bbmri_eric.negotiator.NegotiatorApplication;
import eu.bbmri_eric.negotiator.configuration.state_machine.negotiation.NegotiationEvent;
import eu.bbmri_eric.negotiator.configuration.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.configuration.state_machine.resource.NegotiationResourceEvent;
import eu.bbmri_eric.negotiator.configuration.state_machine.resource.NegotiationResourceState;
import eu.bbmri_eric.negotiator.database.model.AccessForm;
import eu.bbmri_eric.negotiator.database.model.InformationRequirement;
import eu.bbmri_eric.negotiator.database.model.InformationSubmission;
import eu.bbmri_eric.negotiator.database.model.Negotiation;
import eu.bbmri_eric.negotiator.database.model.NegotiationLifecycleRecord;
import eu.bbmri_eric.negotiator.database.model.NegotiationResourceLifecycleRecord;
import eu.bbmri_eric.negotiator.database.model.Person;
import eu.bbmri_eric.negotiator.database.model.Request;
import eu.bbmri_eric.negotiator.database.repository.AccessFormRepository;
import eu.bbmri_eric.negotiator.database.repository.InformationRequirementRepository;
import eu.bbmri_eric.negotiator.database.repository.InformationSubmissionRepository;
import eu.bbmri_eric.negotiator.database.repository.NegotiationRepository;
import eu.bbmri_eric.negotiator.database.repository.RequestRepository;
import eu.bbmri_eric.negotiator.dto.negotiation.NegotiationCreateDTO;
import eu.bbmri_eric.negotiator.dto.negotiation.NegotiationDTO;
import eu.bbmri_eric.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.governance.resource.ResourceRepository;
import eu.bbmri_eric.negotiator.integration.api.v3.TestUtils;
import eu.bbmri_eric.negotiator.service.NegotiationLifecycleServiceImpl;
import eu.bbmri_eric.negotiator.service.NegotiationService;
import eu.bbmri_eric.negotiator.service.ResourceLifecycleService;
import eu.bbmri_eric.negotiator.unit.context.WithMockNegotiatorUser;
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
import org.springframework.statemachine.StateMachineException;
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
  @Autowired RequestRepository requestRepository;
  @Autowired InformationRequirementRepository requirementRepository;
  @Autowired AccessFormRepository accessFormRepository;
  @Autowired private InformationSubmissionRepository informationSubmissionRepository;
  @Autowired private ResourceRepository resourceRepository;

  private void checkNegotiationResourceRecordPresenceWithAssignedState(
      String negotiationId, NegotiationResourceState negotiationResourceState) {
    Negotiation negotiation = negotiationRepository.findDetailedById(negotiationId).get();
    Set<NegotiationResourceLifecycleRecord> records =
        negotiation.getNegotiationResourceLifecycleRecords();
    Assertions.assertNotNull(
        records.stream()
            .filter(r -> r.getChangedTo().equals(negotiationResourceState))
            .findFirst());
  }

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
  @WithMockUser(roles = "ADMIN")
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

  @Test
  @WithMockUser(authorities = "ROLE_ADMIN")
  void sendEvent_abandonNegotiation_to_inProcess_Negotiation() throws IOException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    assertEquals(
        NegotiationState.IN_PROGRESS,
        negotiationLifecycleService.sendEvent(negotiationDTO.getId(), NegotiationEvent.APPROVE));
    assertEquals(
        NegotiationState.ABANDONED,
        negotiationLifecycleService.sendEvent(negotiationDTO.getId(), NegotiationEvent.ABANDON));
  }

  private NegotiationDTO saveNegotiation() throws IOException {
    NegotiationCreateDTO negotiationCreateDTO = TestUtils.createNegotiation(Set.of("request-2"));
    Request request = requestRepository.findById("request-2").get();
    Negotiation negotiation =
        Negotiation.builder()
            .requests(Set.of(request))
            .payload(negotiationCreateDTO.getPayload().toString())
            .build();
    negotiation.setCreatedBy(Person.builder().id(101L).name("TheBuilder").build());
    negotiationRepository.save(negotiation);
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
    assertFalse(negotiationService.findById(negotiationDTO.getId(), false).isPrivatePostsEnabled());
    assertTrue(negotiationService.findById(negotiationDTO.getId(), false).isPublicPostsEnabled());
    assertEquals(
        NegotiationState.IN_PROGRESS,
        negotiationLifecycleService.sendEvent(negotiationDTO.getId(), NegotiationEvent.APPROVE));
    assertTrue(negotiationService.findById(negotiationDTO.getId(), false).isPrivatePostsEnabled());
    assertTrue(negotiationService.findById(negotiationDTO.getId(), false).isPublicPostsEnabled());
  }

  @Test
  @WithMockUser(authorities = "ROLE_ADMIN")
  void sendEvent_approveCorrectly_calledActionDisablePosts() throws IOException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    assertEquals(
        NegotiationState.IN_PROGRESS,
        negotiationLifecycleService.sendEvent(negotiationDTO.getId(), NegotiationEvent.APPROVE));
    assertEquals(
        NegotiationState.ABANDONED,
        negotiationLifecycleService.sendEvent(negotiationDTO.getId(), NegotiationEvent.ABANDON));
    assertFalse(negotiationService.findById(negotiationDTO.getId(), false).isPrivatePostsEnabled());
    assertFalse(negotiationService.findById(negotiationDTO.getId(), false).isPublicPostsEnabled());
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
    assertNotEquals(NegotiationResourceState.SUBMITTED, states.get("biobank:1:collection:2"));
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

    checkNegotiationResourceRecordPresenceWithAssignedState(
        negotiationDTO.getId(), NegotiationResourceState.valueOf("REPRESENTATIVE_CONTACTED"));
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
    Negotiation before = negotiationRepository.findDetailedById(negotiationDTO.getId()).get();
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
  void sendEventForResource_notAuthorized_noChange() {
    Negotiation negotiation = negotiationRepository.findById("negotiation-1").get();
    assertEquals(
        negotiation.getCurrentStatePerResource().get("biobank:1:collection:1"),
        resourceLifecycleService.sendEvent(
            negotiation.getId(),
            "biobank:1:collection:1",
            NegotiationResourceEvent.INDICATE_ACCESS_CONDITIONS));
  }

  @Test
  void sendEventForNegotiation_notAuthorized_noChange() throws IOException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    assertEquals(
        NegotiationState.SUBMITTED,
        negotiationLifecycleService.sendEvent(negotiationDTO.getId(), NegotiationEvent.APPROVE));
  }

  @Test
  void getCurrentStateForResource_newNegotiation_isNull() throws IOException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    assertEquals(
        "",
        negotiationService
            .findById(negotiationDTO.getId(), false)
            .getStatusForResource("biobank:1:collection:2"));
  }

  @Test
  @WithMockUser
  void getPossibleStatesForResource_notAuthorized_isEmpty() {
    Negotiation negotiation = negotiationRepository.findById("negotiation-1").get();
    assertEquals(
        Set.of(),
        resourceLifecycleService.getPossibleEvents(negotiation.getId(), "biobank:1:collection:1"));
    negotiation.setStateForResource(
        "biobank:1:collection:1", NegotiationResourceState.RESOURCE_AVAILABLE);
    assertEquals(
        Set.of(),
        resourceLifecycleService.getPossibleEvents(negotiation.getId(), "biobank:1:collection:1"));
  }

  @Test
  @WithMockUser
  void getPossibleStatesForNegotiation_notAuthorized_isEmpty() {
    Negotiation negotiation = negotiationRepository.findById("negotiation-1").get();
    assertEquals(Set.of(), negotiationLifecycleService.getPossibleEvents(negotiation.getId()));
  }

  @Test
  @WithMockUser(authorities = {"ROLE_ADMIN", "ROLE_REPRESENTATIVE_biobank:1:collection:2"})
  void sendEventForResource_notFulfilledRequirement_throwsStateMachineException()
      throws IOException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    negotiationLifecycleService.sendEvent(negotiationDTO.getId(), NegotiationEvent.APPROVE);
    AccessForm accessForm = accessFormRepository.findAll().stream().findFirst().get();
    requirementRepository.save(
        new InformationRequirement(accessForm, NegotiationResourceEvent.CONTACT));
    assertTrue(requirementRepository.existsByForEvent(NegotiationResourceEvent.CONTACT));
    assertThrows(
        StateMachineException.class,
        () ->
            resourceLifecycleService.sendEvent(
                negotiationDTO.getId(),
                "biobank:1:collection:2",
                NegotiationResourceEvent.CONTACT));
  }

  @Test
  @WithMockNegotiatorUser(authorities = "ROLE_ADMIN", id = 109L)
  @Transactional
  void sendEventForResource_fulfilledRequirement_ok() throws IOException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    negotiationLifecycleService.sendEvent(negotiationDTO.getId(), NegotiationEvent.APPROVE);
    assertEquals(
        NegotiationResourceState.REPRESENTATIVE_CONTACTED,
        resourceRepository.findByNegotiation(negotiationDTO.getId()).stream()
            .filter(
                resourceViewDTO -> resourceViewDTO.getSourceId().equals("biobank:1:collection:2"))
            .findFirst()
            .get()
            .getCurrentState());
    AccessForm accessForm = accessFormRepository.findAll().stream().findFirst().get();
    InformationRequirement requirement =
        requirementRepository.save(
            new InformationRequirement(
                accessForm, NegotiationResourceEvent.MARK_AS_CHECKING_AVAILABILITY));
    Negotiation negotiation = negotiationRepository.findById(negotiationDTO.getId()).get();
    Resource resource = resourceRepository.findBySourceId("biobank:1:collection:2").get();
    informationSubmissionRepository.saveAndFlush(
        new InformationSubmission(requirement, resource, negotiation, "{}"));
    assertTrue(
        requirementRepository.existsByForEvent(
            NegotiationResourceEvent.MARK_AS_CHECKING_AVAILABILITY));
    assertTrue(
        informationSubmissionRepository.existsByResource_SourceIdAndNegotiation_Id(
            resource.getSourceId(), negotiation.getId()));
    assertEquals(
        NegotiationResourceState.CHECKING_AVAILABILITY,
        resourceLifecycleService.sendEvent(
            negotiation.getId(),
            "biobank:1:collection:2",
            NegotiationResourceEvent.MARK_AS_CHECKING_AVAILABILITY));
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
