package eu.bbmri_eric.negotiator.integration.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.common.exceptions.ForbiddenRequestException;
import eu.bbmri_eric.negotiator.discovery.DiscoveryServiceRepository;
import eu.bbmri_eric.negotiator.form.AccessForm;
import eu.bbmri_eric.negotiator.form.repository.AccessFormRepository;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.governance.resource.ResourceRepository;
import eu.bbmri_eric.negotiator.governance.resource.ResourceService;
import eu.bbmri_eric.negotiator.governance.resource.ResourceViewDTO;
import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceWithStatusDTO;
import eu.bbmri_eric.negotiator.info_requirement.InformationRequirement;
import eu.bbmri_eric.negotiator.info_requirement.InformationRequirementRepository;
import eu.bbmri_eric.negotiator.info_submission.InformationSubmission;
import eu.bbmri_eric.negotiator.info_submission.InformationSubmissionRepository;
import eu.bbmri_eric.negotiator.integration.api.v3.TestUtils;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.NegotiationService;
import eu.bbmri_eric.negotiator.negotiation.dto.NegotiationCreateDTO;
import eu.bbmri_eric.negotiator.negotiation.dto.NegotiationDTO;
import eu.bbmri_eric.negotiator.negotiation.dto.UpdateResourcesDTO;
import eu.bbmri_eric.negotiator.negotiation.request.RequestRepository;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationLifecycleRecord;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationLifecycleServiceImpl;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationStateChangeEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceLifecycleRecord;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceState;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.ResourceLifecycleService;
import eu.bbmri_eric.negotiator.post.Post;
import eu.bbmri_eric.negotiator.post.PostRepository;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import eu.bbmri_eric.negotiator.util.WithMockNegotiatorUser;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.statemachine.StateMachineException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.support.TransactionTemplate;

@IntegrationTest(loadTestData = true)
@RecordApplicationEvents
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class NegotiationLifecycleServiceImplTest {

  @Autowired NegotiationLifecycleServiceImpl negotiationLifecycleService;
  @Autowired ResourceLifecycleService resourceLifecycleService;
  @Autowired NegotiationRepository negotiationRepository;
  @Autowired NegotiationService negotiationService;
  @Autowired RequestRepository requestRepository;
  @Autowired PostRepository postRepository;
  @Autowired InformationRequirementRepository requirementRepository;
  @Autowired AccessFormRepository accessFormRepository;
  @Autowired ApplicationEvents events;
  @Autowired ResourceService resourceService;
  @Autowired private InformationSubmissionRepository informationSubmissionRepository;
  @Autowired private ResourceRepository resourceRepository;
  @Autowired private DiscoveryServiceRepository discoveryServiceRepository;
  @Autowired private ModelMapper modelMapper;
  @Autowired private TransactionTemplate transactionTemplate;

  void checkNegotiationResourceRecordPresenceWithAssignedState(
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
  @Transactional
  void getState_createNegotiation_isSubmitted() throws IOException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    assertEquals(NegotiationState.SUBMITTED, NegotiationState.valueOf(negotiationDTO.getStatus()));
  }

  @Test
  @WithMockNegotiatorUser(id = 109L, authorities = "ROLE_ADMIN")
  @Transactional
  public void getPossibleEvents_existingNegotiationAndIsAdmin_Ok() throws IOException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    assertEquals(
        Set.of(NegotiationEvent.APPROVE, NegotiationEvent.DECLINE),
        negotiationLifecycleService.getPossibleEvents(negotiationDTO.getId()));
  }

  @Test
  @WithMockNegotiatorUser(id = 109L, authorities = "ROLE_ADMIN")
  @Transactional
  public void getPossibleEvents_nonExistentId_throwsEntityNotFoundException() {
    assertThrows(
        EntityNotFoundException.class,
        () -> negotiationLifecycleService.getPossibleEvents("fakeId"));
  }

  @Test
  @WithUserDetails("researcher")
  @Transactional
  public void getPossibleEvents_existingIdNotAdmin_returnsEmptySet() throws IOException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    assertEquals(Set.of(), negotiationLifecycleService.getPossibleEvents(negotiationDTO.getId()));
  }

  @Test
  @WithMockNegotiatorUser(id = 109L, authorities = "ROLE_ADMIN")
  @Transactional
  void sendEvent_approveNewNegotiation_isOngoing() throws IOException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    assertEquals(
        NegotiationState.IN_PROGRESS,
        negotiationLifecycleService.sendEvent(negotiationDTO.getId(), NegotiationEvent.APPROVE));
    assertEquals(
        NegotiationState.IN_PROGRESS,
        NegotiationState.valueOf(
            negotiationService.findById(negotiationDTO.getId(), false).getStatus()));
    long numEvents = events.stream(NegotiationStateChangeEvent.class).count();
    assertThat(numEvents).isEqualTo(1);
  }

  @Test
  @WithMockNegotiatorUser(id = 101L, authorities = "ROLE_ADMIN")
  @Transactional
  void sendEvent_declineNegotiation_createPost() throws IOException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    List<Post> posts = postRepository.findByNegotiationId(negotiationDTO.getId());
    int numberOfPosts = posts.size();
    assertEquals(
        NegotiationState.DECLINED,
        negotiationLifecycleService.sendEvent(
            negotiationDTO.getId(), NegotiationEvent.DECLINE, "not acceptable"));
    long numEvents = events.stream(NegotiationStateChangeEvent.class).count();
    assertThat(numEvents).isEqualTo(1);
    posts = postRepository.findByNegotiationId(negotiationDTO.getId());
    assertThat(posts.size()).isEqualTo(numberOfPosts + 1);
  }

  @Test
  @WithMockNegotiatorUser(id = 109L, authorities = "ROLE_ADMIN")
  void sendEvent_declineNegotiation_failsIfNegotiationNotFound() {
    assertThrows(
        EntityNotFoundException.class,
        () ->
            negotiationLifecycleService.sendEvent(
                "unknownd", NegotiationEvent.DECLINE, "not acceptable"));
  }

  @Test
  @WithMockNegotiatorUser(id = 109L, authorities = "ROLE_ADMIN")
  @Transactional
  void sendEvent_abandonNegotiation_to_inProcess_Negotiation() throws IOException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    assertEquals(
        NegotiationState.IN_PROGRESS,
        negotiationLifecycleService.sendEvent(negotiationDTO.getId(), NegotiationEvent.APPROVE));
    assertEquals(
        NegotiationState.ABANDONED,
        negotiationLifecycleService.sendEvent(
            negotiationDTO.getId(), NegotiationEvent.ABANDON, "Not acceptable"));
  }

  NegotiationDTO saveNegotiation() throws IOException {
    return saveNegotiation(false);
  }

  private NegotiationDTO saveNegotiation(boolean draft) throws IOException {
    NegotiationCreateDTO negotiationCreateDTO = TestUtils.createNegotiation("request-2", draft);
    return negotiationService.create(negotiationCreateDTO, 109L);
  }

  @Test
  @WithMockNegotiatorUser(authorities = "ROLE_ADMIN", id = 101L)
  @Transactional
  void sendEvent_wrongEvent_noChangeInState() throws IOException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    assertThrows(
        ForbiddenRequestException.class,
        () ->
            negotiationLifecycleService.sendEvent(
                negotiationDTO.getId(), NegotiationEvent.ABANDON, "not acceptable"));
    assertEquals(
        NegotiationState.SUBMITTED,
        NegotiationState.valueOf(
            negotiationService.findById(negotiationDTO.getId(), false).getStatus()));
  }

  @Test
  @WithMockNegotiatorUser(id = 109L, authorities = "ROLE_ADMIN")
  @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
  void sendEvent_submitCorrectly_calledActionEnablePublicPost() throws IOException {
    NegotiationDTO negotiationDTO = saveNegotiation(true);
    assertFalse(negotiationService.findById(negotiationDTO.getId(), false).isPrivatePostsEnabled());
    assertFalse(negotiationService.findById(negotiationDTO.getId(), false).isPublicPostsEnabled());
    assertEquals(
        negotiationService.findById(negotiationDTO.getId(), false).getStatus(),
        NegotiationState.DRAFT.getValue());
    assertEquals(
        NegotiationState.SUBMITTED,
        negotiationLifecycleService.sendEvent(negotiationDTO.getId(), NegotiationEvent.SUBMIT));
    assertTrue(negotiationService.findById(negotiationDTO.getId(), false).isPublicPostsEnabled());
    assertFalse(negotiationService.findById(negotiationDTO.getId(), false).isPrivatePostsEnabled());
    long numEvents = events.stream(NegotiationStateChangeEvent.class).count();
    assertEquals(1, numEvents);
    assertEquals(
        negotiationService.findById(negotiationDTO.getId(), false).getStatus(),
        NegotiationState.SUBMITTED.getValue());
  }

  @Test
  @WithMockNegotiatorUser(id = 109L, authorities = "ROLE_ADMIN")
  @Transactional
  void sendEvent_approveCorrectly_calledActionEnablePost() throws IOException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    assertFalse(negotiationService.findById(negotiationDTO.getId(), false).isPrivatePostsEnabled());
    assertTrue(negotiationService.findById(negotiationDTO.getId(), false).isPublicPostsEnabled());
    assertEquals(
        NegotiationState.IN_PROGRESS,
        negotiationLifecycleService.sendEvent(
            negotiationDTO.getId(), NegotiationEvent.APPROVE, null));
    assertTrue(negotiationService.findById(negotiationDTO.getId(), false).isPrivatePostsEnabled());
    assertTrue(negotiationService.findById(negotiationDTO.getId(), false).isPublicPostsEnabled());
  }

  @Test
  @WithMockNegotiatorUser(id = 109L, authorities = "ROLE_ADMIN")
  @Transactional
  void sendEvent_abandon_calledActionDisablePosts() throws IOException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    assertEquals(
        NegotiationState.IN_PROGRESS,
        negotiationLifecycleService.sendEvent(negotiationDTO.getId(), NegotiationEvent.APPROVE));
    assertEquals(
        NegotiationState.ABANDONED,
        negotiationLifecycleService.sendEvent(
            negotiationDTO.getId(), NegotiationEvent.ABANDON, "not acceptable"));
    assertFalse(negotiationService.findById(negotiationDTO.getId(), false).isPrivatePostsEnabled());
    assertFalse(negotiationService.findById(negotiationDTO.getId(), false).isPublicPostsEnabled());
  }

  @Test
  @WithMockNegotiatorUser(authorities = "ROLE_ADMIN", id = 101L)
  @Transactional
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
  @WithMockNegotiatorUser(id = 109L, authorities = "ROLE_ADMIN")
  void createNegotiation_approve_eachResourceHasState() throws IOException, InterruptedException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    NegotiationState state =
        negotiationLifecycleService.sendEvent(negotiationDTO.getId(), NegotiationEvent.APPROVE);
    assertEquals(NegotiationState.IN_PROGRESS, state);
    Thread.sleep(1000);
    transactionTemplate.executeWithoutResult(
        status -> {
          Negotiation negotiation = negotiationRepository.findById(negotiationDTO.getId()).get();
          negotiation
              .getResources()
              .forEach(
                  resource -> {
                    assertEquals(
                        NegotiationResourceState.REPRESENTATIVE_CONTACTED,
                        negotiation.getCurrentStateForResource(resource.getSourceId()));
                  });
        });
  }

  @Test
  @Transactional
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
  void sendEventForResource_approvedNegotiation_Ok() throws IOException, InterruptedException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    negotiationLifecycleService.sendEvent(negotiationDTO.getId(), NegotiationEvent.APPROVE);
    Thread.sleep(1000);
    assertEquals(
        NegotiationResourceState.REPRESENTATIVE_CONTACTED,
        resourceLifecycleService.sendEvent(
            negotiationDTO.getId(), "biobank:1:collection:2", NegotiationResourceEvent.CONTACT));
    transactionTemplate.executeWithoutResult(
        status ->
            checkNegotiationResourceRecordPresenceWithAssignedState(
                negotiationDTO.getId(),
                NegotiationResourceState.valueOf("REPRESENTATIVE_CONTACTED")));
  }

  @Test
  @WithMockNegotiatorUser(authorities = "ROLE_ADMIN", id = 109L)
  void sendEventForResource_approvedNegotiationWrongEvent_noChange()
      throws IOException, InterruptedException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    negotiationLifecycleService.sendEvent(negotiationDTO.getId(), NegotiationEvent.APPROVE);
    Thread.sleep(1000);
    assertEquals(
        NegotiationResourceState.REPRESENTATIVE_CONTACTED,
        resourceLifecycleService.sendEvent(
            negotiationDTO.getId(),
            "biobank:1:collection:2",
            NegotiationResourceEvent.INDICATE_ACCESS_CONDITIONS));
  }

  @Test
  @WithMockNegotiatorUser(authorities = "ROLE_ADMIN", id = 109L)
  void sendEventForResource_approvedNegotiationMultipleCorrectEvents_ok()
      throws IOException, InterruptedException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    NegotiationState state =
        negotiationLifecycleService.sendEvent(negotiationDTO.getId(), NegotiationEvent.APPROVE);
    Thread.sleep(1000);
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
    transactionTemplate.executeWithoutResult(
        status -> {
          Negotiation negotiation =
              negotiationRepository.findDetailedById(negotiationDTO.getId()).get();
          Set<NegotiationResourceLifecycleRecord> records =
              negotiation.getNegotiationResourceLifecycleRecords();
          assertEquals(4, records.size());
        });
  }

  @Test
  @WithMockNegotiatorUser(id = 109L)
  @Transactional
  void sendEventForResource_notAuthorized_noChange() {
    Negotiation negotiation = negotiationRepository.findById("negotiation-1").get();
    assertEquals(
        negotiation.getCurrentStateForResource("biobank:1:collection:1"),
        resourceLifecycleService.sendEvent(
            negotiation.getId(),
            "biobank:1:collection:1",
            NegotiationResourceEvent.INDICATE_ACCESS_CONDITIONS));
  }

  @Test
  @WithMockNegotiatorUser(id = 105L)
  @Transactional
  void sendEventForNegotiation_notAuthorized_noChange() throws IOException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    assertThrows(
        ForbiddenRequestException.class,
        () ->
            negotiationLifecycleService.sendEvent(
                negotiationDTO.getId(), NegotiationEvent.ABANDON, "not acceptable"));
    assertEquals(
        NegotiationState.SUBMITTED,
        NegotiationState.valueOf(
            negotiationService.findById(negotiationDTO.getId(), false).getStatus()));
  }

  @Test
  @WithMockNegotiatorUser(id = 109L, authorities = "ROLE_ADMIN")
  @Transactional
  void getCurrentStateForResource_newNegotiation_isNull() throws IOException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    assertEquals(
        "",
        negotiationService
            .findById(negotiationDTO.getId(), false)
            .getStatusForResource("biobank:1:collection:2"));
  }

  @Test
  @WithMockNegotiatorUser(id = 102L)
  @Transactional
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
  @WithMockNegotiatorUser(id = 105L)
  @Transactional
  void getPossibleStatesForNegotiation_notAuthorized_isEmpty() {
    Negotiation negotiation = negotiationRepository.findById("negotiation-1").get();
    assertEquals(Set.of(), negotiationLifecycleService.getPossibleEvents(negotiation.getId()));
  }

  @Test
  @WithMockNegotiatorUser(id = 109L, authorities = "ROLE_ADMIN")
  @Transactional
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
  void sendEventForResource_fulfilledRequirement_ok() throws IOException, InterruptedException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    negotiationLifecycleService.sendEvent(negotiationDTO.getId(), NegotiationEvent.APPROVE);
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
    Thread.sleep(1000);
    assertEquals(
        NegotiationResourceState.CHECKING_AVAILABILITY,
        resourceLifecycleService.sendEvent(
            negotiationDTO.getId(),
            "biobank:1:collection:2",
            NegotiationResourceEvent.MARK_AS_CHECKING_AVAILABILITY));
  }

  @Test
  @Transactional
  void getPossibleEventsForResource_nonApprovedNegotiation_returnsEmptySet() throws IOException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    assertEquals(
        Set.of(),
        resourceLifecycleService.getPossibleEvents(
            negotiationDTO.getId(), "biobank:1:collection:2"));
  }

  @Test
  @WithMockNegotiatorUser(authorities = "ROLE_ADMIN", id = 109L)
  void getPossibleEventsForResource_approvedNegotiation_Ok()
      throws IOException, InterruptedException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    negotiationLifecycleService.sendEvent(negotiationDTO.getId(), NegotiationEvent.APPROVE);
    Thread.sleep(1000);
    assertEquals(
        Set.of(
            NegotiationResourceEvent.STEP_AWAY,
            NegotiationResourceEvent.MARK_AS_CHECKING_AVAILABILITY),
        resourceLifecycleService.getPossibleEvents(
            negotiationDTO.getId(), "biobank:1:collection:2"));
  }

  @Test
  @Transactional
  void newNegotiation_findAllWithState_oneWithSubmitted() throws IOException {
    saveNegotiation();
    assertTrue(
        negotiationService.findAllWithCurrentState(NegotiationState.SUBMITTED).stream()
            .allMatch(dto -> Objects.equals(dto.getStatus(), NegotiationState.SUBMITTED.name())));
  }

  @Test
  @WithMockNegotiatorUser(authorities = "ROLE_ADMIN", id = 109L)
  @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
  void successfulNegotiation_2finishedResources_closedAutomatically() throws IOException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    assertEquals(NegotiationState.SUBMITTED, NegotiationState.valueOf(negotiationDTO.getStatus()));
    negotiationLifecycleService.sendEvent(negotiationDTO.getId(), NegotiationEvent.APPROVE);

    // Wait for the approval handler to complete
    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(
            () -> {
              Negotiation negotiation =
                  negotiationRepository.findById(negotiationDTO.getId()).get();
              assertEquals(NegotiationState.IN_PROGRESS, negotiation.getCurrentState());
            });

    Negotiation negotiation = negotiationRepository.findById(negotiationDTO.getId()).get();
    List<ResourceWithStatusDTO> resources =
        resourceService.findAllInNegotiation(negotiation.getId());
    assertEquals(2, resources.size());
    resourceService.updateResourcesInANegotiation(
        negotiation.getId(),
        new UpdateResourcesDTO(
            resources.stream().map(ResourceWithStatusDTO::getId).collect(Collectors.toList()),
            NegotiationResourceState.RESOURCE_MADE_AVAILABLE));
    assertEquals(2, resources.size());

    // Wait for the resource state update handler to complete
    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(
            () -> {
              List<ResourceViewDTO> foundResources =
                  resourceRepository.findByNegotiation(negotiation.getId());
              foundResources.forEach(
                  resource ->
                      assertEquals(
                          NegotiationResourceState.RESOURCE_MADE_AVAILABLE,
                          resource.getCurrentState()));
            });

    // Wait for the automatic negotiation closure handler to complete
    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(
            () -> {
              assertEquals(
                  NegotiationState.CONCLUDED,
                  negotiationRepository.findNegotiationStateById(negotiation.getId()).get());
            });
  }
}
