package eu.bbmri_eric.negotiator.lifecycle.negotiation;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.common.exceptions.ForbiddenRequestException;
import eu.bbmri_eric.negotiator.governance.resource.ResourceRepository;
import eu.bbmri_eric.negotiator.governance.resource.ResourceService;
import eu.bbmri_eric.negotiator.governance.resource.ResourceViewDTO;
import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceWithStatusDTO;
import eu.bbmri_eric.negotiator.integration.api.v3.TestUtils;
import eu.bbmri_eric.negotiator.lifecycle.SpringBeanResolver;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationEvent;
import eu.bbmri_eric.negotiator.negotiation.NegotiationLifecycleRecord;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.NegotiationResourceState;
import eu.bbmri_eric.negotiator.negotiation.NegotiationService;
import eu.bbmri_eric.negotiator.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.negotiation.NegotiationStateChangeEvent;
import eu.bbmri_eric.negotiator.negotiation.dto.NegotiationCreateDTO;
import eu.bbmri_eric.negotiator.negotiation.dto.NegotiationDTO;
import eu.bbmri_eric.negotiator.negotiation.dto.UpdateResourcesDTO;
import eu.bbmri_eric.negotiator.post.Post;
import eu.bbmri_eric.negotiator.post.PostRepository;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import eu.bbmri_eric.negotiator.util.WithMockNegotiatorUser;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.support.TransactionTemplate;

@IntegrationTest(loadTestData = true)
@RecordApplicationEvents
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class NegotiationLifecycleServiceImplTest {

  @Autowired NegotiationRepository negotiationRepository;
  @Autowired NegotiationService negotiationService;
  @Autowired SpringBeanResolver beanResolver;
  @Autowired NegotiationPersistListener persistListener;
  @Autowired PostRepository postRepository;
  @Autowired ApplicationEvents events;
  @Autowired TransactionTemplate transactionTemplate;
  @Autowired ResourceService resourceService;
  @Autowired ResourceRepository resourceRepository;

  private NegotiationLifecycleServiceImpl negotiationLifecycleService;

  @BeforeEach
  void setUp() {
    negotiationLifecycleService =
        new NegotiationLifecycleServiceImpl(negotiationRepository, beanResolver, List.of(persistListener));
  }

  NegotiationDTO saveNegotiation() throws IOException {
    return saveNegotiation(false);
  }

  private NegotiationDTO saveNegotiation(boolean draft) throws IOException {
    NegotiationCreateDTO negotiationCreateDTO = TestUtils.createNegotiation("request-2", draft);
    return negotiationService.create(negotiationCreateDTO, 109L);
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
  void getPossibleEvents_existingNegotiationAndIsAdmin_Ok() throws IOException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    assertEquals(
        Set.of(NegotiationEvent.APPROVE, NegotiationEvent.DECLINE),
        negotiationLifecycleService.getPossibleEvents(negotiationDTO.getId()));
  }

  @Test
  @WithMockNegotiatorUser(id = 109L, authorities = "ROLE_ADMIN")
  @Transactional
  void getPossibleEvents_nonExistentId_throwsEntityNotFoundException() {
    assertThrows(
        EntityNotFoundException.class,
        () -> negotiationLifecycleService.getPossibleEvents("fakeId"));
  }

  @Test
  @WithUserDetails("researcher")
  @Transactional
  void getPossibleEvents_existingIdNotAdmin_returnsEmptySet() throws IOException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    assertEquals(Set.of(), negotiationLifecycleService.getPossibleEvents(negotiationDTO.getId()));
  }

  @Test
  @WithMockNegotiatorUser(id = 105L)
  @Transactional
  void getPossibleStatesForNegotiation_notAuthorized_isEmpty() {
    Negotiation negotiation = negotiationRepository.findById("negotiation-1").get();
    assertEquals(Set.of(), negotiationLifecycleService.getPossibleEvents(negotiation.getId()));
  }

  @Test
  @Transactional
  void newNegotiation_findAllWithState_oneWithSubmitted() throws IOException {
    saveNegotiation();
    assertEquals(
        true,
        negotiationService.findAllWithCurrentState(NegotiationState.SUBMITTED).stream()
            .allMatch(dto -> dto.getStatus().equals(NegotiationState.SUBMITTED.name())));
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
  void createNegotiation_approve_eachResourceHasState() throws IOException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    NegotiationState state =
        negotiationLifecycleService.sendEvent(negotiationDTO.getId(), NegotiationEvent.APPROVE);
    assertEquals(NegotiationState.IN_PROGRESS, state);
    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(
            () ->
                transactionTemplate.executeWithoutResult(
                    status -> {
                      Negotiation negotiation =
                          negotiationRepository.findById(negotiationDTO.getId()).get();
                      negotiation
                          .getResources()
                          .forEach(
                              resource ->
                                  assertEquals(
                                      NegotiationResourceState.REPRESENTATIVE_CONTACTED,
                                      negotiation.getCurrentStateForResource(
                                          resource.getSourceId())));
                    }));
  }

  @Test
  @WithMockNegotiatorUser(authorities = "ROLE_ADMIN", id = 109L)
  @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
  void successfulNegotiation_2finishedResources_closedAutomatically() throws IOException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    assertEquals(NegotiationState.SUBMITTED, NegotiationState.valueOf(negotiationDTO.getStatus()));
    negotiationLifecycleService.sendEvent(negotiationDTO.getId(), NegotiationEvent.APPROVE);

    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(
            () -> {
              Negotiation negotiation = negotiationRepository.findById(negotiationDTO.getId()).get();
              assertEquals(NegotiationState.IN_PROGRESS, negotiation.getCurrentState());
            });

    Negotiation negotiation = negotiationRepository.findById(negotiationDTO.getId()).get();
    List<ResourceWithStatusDTO> resources = resourceService.findAllInNegotiation(negotiation.getId());
    assertEquals(2, resources.size());
    resourceService.updateResourcesInANegotiation(
        negotiation.getId(),
        new UpdateResourcesDTO(
            resources.stream().map(ResourceWithStatusDTO::getId).collect(Collectors.toList()),
            NegotiationResourceState.RESOURCE_MADE_AVAILABLE));

    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(
            () -> {
              List<ResourceViewDTO> foundResources = resourceRepository.findByNegotiation(negotiation.getId());
              foundResources.forEach(
                  resource ->
                      assertEquals(
                          NegotiationResourceState.RESOURCE_MADE_AVAILABLE, resource.getCurrentState()));
            });

    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(
            () ->
                assertEquals(
                    NegotiationState.CONCLUDED,
                    negotiationRepository.findNegotiationStateById(negotiation.getId()).get()));
  }
}
