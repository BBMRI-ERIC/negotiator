package eu.bbmri.eric.csit.service.negotiator.integration;

import static org.junit.jupiter.api.Assertions.*;

import eu.bbmri.eric.csit.service.negotiator.NegotiatorApplication;
import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationEvent;
import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationLifecycleRecord;
import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationState;
import eu.bbmri.eric.csit.service.negotiator.database.repository.NegotiationRepository;
import eu.bbmri.eric.csit.service.negotiator.dto.negotiation.NegotiationCreateDTO;
import eu.bbmri.eric.csit.service.negotiator.dto.negotiation.NegotiationDTO;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri.eric.csit.service.negotiator.integration.api.v3.TestUtils;
import eu.bbmri.eric.csit.service.negotiator.service.NegotiationLifecycleServiceImpl;
import eu.bbmri.eric.csit.service.negotiator.service.NegotiationService;
import java.io.IOException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = NegotiatorApplication.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class NegotiationLifecycleServiceImplTest {

  @Autowired NegotiationLifecycleServiceImpl negotiationStateService;
  @Autowired NegotiationRepository negotiationRepository;
  @Autowired NegotiationService negotiationService;

  @Test
  void getState_createNegotiation_isSubmitted() throws IOException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    assertEquals(NegotiationState.SUBMITTED, NegotiationState.valueOf(negotiationDTO.getStatus()));
  }

  @Test
  void getState_fakeNegotiation_ThrowsEntityNotFoundException() {
    assertThrows(
        EntityNotFoundException.class, () -> negotiationStateService.getCurrentState("fake"));
  }

  @Test
  public void getPossibleEvents_existingNegotiation_Ok() throws IOException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    assertEquals(
        Set.of(NegotiationEvent.APPROVE, NegotiationEvent.DECLINE),
        negotiationStateService.getPossibleEvents(negotiationDTO.getId()));
  }

  @Test
  public void getPossibleEvents_nonExistentId_throwsEntityNotFoundException() {
    assertThrows(
        EntityNotFoundException.class, () -> negotiationStateService.getPossibleEvents("fakeId"));
  }

  @Test
  void sendEvent_approveNewNegotiation_isOngoing() throws IOException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    assertEquals(
        NegotiationState.ONGOING,
        negotiationStateService.sendEvent(negotiationDTO.getId(), NegotiationEvent.APPROVE));
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
        negotiationStateService.sendEvent(negotiationDTO.getId(), NegotiationEvent.ABANDON));
    assertEquals(
        NegotiationState.SUBMITTED,
        NegotiationState.valueOf(
            negotiationService.findById(negotiationDTO.getId(), false).getStatus()));
  }

  @Test
  void sendEvent_approveCorrectly_calledActionEnablePost() throws IOException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    assertFalse(negotiationService.findById(negotiationDTO.getId(), false).getPostsEnabled());
    assertEquals(
        NegotiationState.ONGOING,
        negotiationStateService.sendEvent(negotiationDTO.getId(), NegotiationEvent.APPROVE));
    assertTrue(negotiationService.findById(negotiationDTO.getId(), false).getPostsEnabled());
  }

  @Test
  void sendEvent_approveCorrectly_historyIsUpdated() throws IOException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    negotiationStateService.sendEvent(negotiationDTO.getId(), NegotiationEvent.APPROVE);
    Set<NegotiationLifecycleRecord> history =
        negotiationRepository.findById(negotiationDTO.getId()).get().getLifecycleHistory();
    assertEquals(2, history.size());
    assertTrue(
        history.stream()
            .anyMatch(record -> record.getChangedTo().equals(NegotiationState.ONGOING)));
  }
}
