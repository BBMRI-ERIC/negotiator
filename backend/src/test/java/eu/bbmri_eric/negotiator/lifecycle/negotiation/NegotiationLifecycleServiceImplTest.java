package eu.bbmri_eric.negotiator.lifecycle.negotiation;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.common.exceptions.ForbiddenRequestException;
import eu.bbmri_eric.negotiator.integration.api.v3.TestUtils;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationEvent;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.NegotiationService;
import eu.bbmri_eric.negotiator.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.negotiation.NegotiationStateChangeEvent;
import eu.bbmri_eric.negotiator.negotiation.dto.NegotiationCreateDTO;
import eu.bbmri_eric.negotiator.negotiation.dto.NegotiationDTO;
import eu.bbmri_eric.negotiator.post.Post;
import eu.bbmri_eric.negotiator.post.PostRepository;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import eu.bbmri_eric.negotiator.util.WithMockNegotiatorUser;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

@IntegrationTest(loadTestData = true)
@RecordApplicationEvents
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class NegotiationLifecycleServiceImplTest {

  @Autowired NegotiationLifecycleService negotiationLifecycleService;
  @Autowired NegotiationService negotiationService;
  @Autowired NegotiationRepository negotiationRepository;
  @Autowired PostRepository postRepository;
  @Autowired ApplicationEvents events;

  private NegotiationDTO saveNegotiation() throws IOException {
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
  public void getPossibleEvents_existingNegotiationAndIsAdmin_Ok() throws IOException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    assertEquals(
        Set.of(NegotiationEvent.APPROVE, NegotiationEvent.DECLINE),
        negotiationLifecycleService.getPossibleEvents(negotiationDTO.getId()));
  }

  @Test
  @WithMockNegotiatorUser(id = 109L, authorities = "ROLE_ADMIN")
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
  @WithMockNegotiatorUser(id = 109L, authorities = "ROLE_ADMIN")
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
  @WithMockNegotiatorUser(id = 105L)
  void getPossibleStatesForNegotiation_notAuthorized_isEmpty() {
    Negotiation negotiation = negotiationRepository.findById("negotiation-1").get();
    assertEquals(Set.of(), negotiationLifecycleService.getPossibleEvents(negotiation.getId()));
  }

  @Test
  @WithMockNegotiatorUser(id = 109L, authorities = "ROLE_ADMIN")
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
    var history =
        negotiationRepository.findDetailedById(negotiationDTO.getId()).get().getLifecycleHistory();
    assertEquals(2, history.size());
    assertTrue(
        history.stream()
            .anyMatch(record -> record.getChangedTo().equals(NegotiationState.IN_PROGRESS)));
  }

  @Test
  @Transactional
  void newNegotiation_findAllWithState_oneWithSubmitted() throws IOException {
    saveNegotiation();
    assertTrue(
        negotiationService.findAllWithCurrentState(NegotiationState.SUBMITTED).stream()
            .allMatch(
                dto ->
                    java.util.Objects.equals(dto.getStatus(), NegotiationState.SUBMITTED.name())));
  }
}
