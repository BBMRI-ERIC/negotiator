package eu.bbmri_eric.negotiator.lifecycle.resource;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.form.AccessForm;
import eu.bbmri_eric.negotiator.form.repository.AccessFormRepository;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.governance.resource.ResourceRepository;
import eu.bbmri_eric.negotiator.info_requirement.InformationRequirement;
import eu.bbmri_eric.negotiator.info_requirement.InformationRequirementRepository;
import eu.bbmri_eric.negotiator.info_submission.InformationSubmission;
import eu.bbmri_eric.negotiator.info_submission.InformationSubmissionRepository;
import eu.bbmri_eric.negotiator.integration.api.v3.TestUtils;
import eu.bbmri_eric.negotiator.lifecycle.SpringBeanResolver;
import eu.bbmri_eric.negotiator.lifecycle.TransitionPreconditionException;
import eu.bbmri_eric.negotiator.lifecycle.negotiation.NegotiationLifecycleServiceImpl;
import eu.bbmri_eric.negotiator.lifecycle.negotiation.NegotiationPersistListener;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationEvent;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.NegotiationResourceEvent;
import eu.bbmri_eric.negotiator.negotiation.NegotiationResourceLifecycleRecord;
import eu.bbmri_eric.negotiator.negotiation.NegotiationResourceState;
import eu.bbmri_eric.negotiator.negotiation.NegotiationService;
import eu.bbmri_eric.negotiator.negotiation.dto.NegotiationCreateDTO;
import eu.bbmri_eric.negotiator.negotiation.dto.NegotiationDTO;
import eu.bbmri_eric.negotiator.user.PersonService;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import eu.bbmri_eric.negotiator.util.WithMockNegotiatorUser;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.support.TransactionTemplate;

@IntegrationTest(loadTestData = true)
@RecordApplicationEvents
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ResourceLifecycleServiceImplTest {

  @Autowired NegotiationRepository negotiationRepository;
  @Autowired NegotiationService negotiationService;
  @Autowired SpringBeanResolver beanResolver;
  @Autowired NegotiationPersistListener negotiationPersistListener;
  @Autowired ResourcePersistListener resourcePersistListener;
  @Autowired InformationRequirementRepository requirementRepository;
  @Autowired InformationSubmissionRepository informationSubmissionRepository;
  @Autowired AccessFormRepository accessFormRepository;
  @Autowired ResourceRepository resourceRepository;
  @Autowired PersonService personService;
  @Autowired TransactionTemplate transactionTemplate;

  private NegotiationLifecycleServiceImpl negotiationLifecycleService;
  private ResourceLifecycleServiceImpl resourceLifecycleService;

  @BeforeEach
  void setUp() {
    negotiationLifecycleService =
        new NegotiationLifecycleServiceImpl(
            negotiationRepository, beanResolver, List.of(negotiationPersistListener));
    resourceLifecycleService =
        new ResourceLifecycleServiceImpl(
            negotiationRepository,
            requirementRepository,
            informationSubmissionRepository,
            personService,
            beanResolver,
            List.of(resourcePersistListener));
  }

  private NegotiationDTO saveNegotiation() throws IOException {
    NegotiationCreateDTO negotiationCreateDTO = TestUtils.createNegotiation("request-2", false);
    return negotiationService.create(negotiationCreateDTO, 109L);
  }

  private void approveAndAwaitInProgress(String negotiationId) {
    negotiationLifecycleService.sendEvent(negotiationId, NegotiationEvent.APPROVE);
    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(
            () ->
                transactionTemplate.executeWithoutResult(
                    status ->
                        assertEquals(
                            eu.bbmri_eric.negotiator.negotiation.NegotiationState.IN_PROGRESS,
                            negotiationRepository.findById(negotiationId).get().getCurrentState())));
  }

  void checkNegotiationResourceRecordPresenceWithAssignedState(
      String negotiationId, NegotiationResourceState negotiationResourceState) {
    Negotiation negotiation = negotiationRepository.findDetailedById(negotiationId).get();
    Set<NegotiationResourceLifecycleRecord> records =
        negotiation.getNegotiationResourceLifecycleRecords();
    Assertions.assertNotNull(
        records.stream().filter(r -> r.getChangedTo().equals(negotiationResourceState)).findFirst());
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
                negotiationDTO.getId(), "biobank:1:collection:2", NegotiationResourceEvent.CONTACT));
  }

  @Test
  @WithMockNegotiatorUser(authorities = "ROLE_ADMIN", id = 109L)
  void sendEventForResource_approvedNegotiation_Ok() throws IOException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    approveAndAwaitInProgress(negotiationDTO.getId());
    assertEquals(
        NegotiationResourceState.REPRESENTATIVE_CONTACTED,
        resourceLifecycleService.sendEvent(
            negotiationDTO.getId(), "biobank:1:collection:2", NegotiationResourceEvent.CONTACT));
    transactionTemplate.executeWithoutResult(
        status ->
            checkNegotiationResourceRecordPresenceWithAssignedState(
                negotiationDTO.getId(), NegotiationResourceState.REPRESENTATIVE_CONTACTED));
  }

  @Test
  @WithMockNegotiatorUser(authorities = "ROLE_ADMIN", id = 109L)
  void sendEventForResource_approvedNegotiationWrongEvent_noChange() throws IOException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    approveAndAwaitInProgress(negotiationDTO.getId());
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
    approveAndAwaitInProgress(negotiationDTO.getId());
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
          Negotiation negotiation = negotiationRepository.findDetailedById(negotiationDTO.getId()).get();
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
  @WithMockNegotiatorUser(id = 109L, authorities = "ROLE_ADMIN")
  void sendEventForResource_notFulfilledRequirement_throwsTransitionPreconditionException()
      throws IOException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    approveAndAwaitInProgress(negotiationDTO.getId());
    AccessForm accessForm = accessFormRepository.findAll().stream().findFirst().get();
    requirementRepository.save(new InformationRequirement(accessForm, NegotiationResourceEvent.CONTACT));
    assertTrue(requirementRepository.existsByForEvent(NegotiationResourceEvent.CONTACT));
    assertThrows(
        TransitionPreconditionException.class,
        () ->
            resourceLifecycleService.sendEvent(
                negotiationDTO.getId(), "biobank:1:collection:2", NegotiationResourceEvent.CONTACT));
  }

  @Test
  @WithMockNegotiatorUser(authorities = "ROLE_ADMIN", id = 109L)
  void sendEventForResource_fulfilledRequirement_ok() throws IOException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    approveAndAwaitInProgress(negotiationDTO.getId());
    AccessForm accessForm = accessFormRepository.findAll().stream().findFirst().get();
    InformationRequirement requirement =
        requirementRepository.save(
            new InformationRequirement(accessForm, NegotiationResourceEvent.MARK_AS_CHECKING_AVAILABILITY));
    Negotiation negotiation = negotiationRepository.findById(negotiationDTO.getId()).get();
    Resource resource = resourceRepository.findBySourceId("biobank:1:collection:2").get();
    informationSubmissionRepository.saveAndFlush(
        new InformationSubmission(requirement, resource, negotiation, "{}"));
    assertTrue(
        requirementRepository.existsByForEvent(NegotiationResourceEvent.MARK_AS_CHECKING_AVAILABILITY));
    assertTrue(
        informationSubmissionRepository.existsByResource_SourceIdAndNegotiation_Id(
            resource.getSourceId(), negotiation.getId()));
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
        resourceLifecycleService.getPossibleEvents(negotiationDTO.getId(), "biobank:1:collection:2"));
  }

  @Test
  @WithMockNegotiatorUser(authorities = "ROLE_ADMIN", id = 109L)
  void getPossibleEventsForResource_approvedNegotiation_Ok() throws IOException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    approveAndAwaitInProgress(negotiationDTO.getId());
    assertEquals(
        Set.of(NegotiationResourceEvent.STEP_AWAY, NegotiationResourceEvent.MARK_AS_CHECKING_AVAILABILITY),
        resourceLifecycleService.getPossibleEvents(negotiationDTO.getId(), "biobank:1:collection:2"));
  }

  @Test
  @SuppressWarnings("unchecked")
  void getStateMachineDiagram_returnsNestedTransitionTree() {
    Map<String, Object> diagram = resourceLifecycleService.getStateMachineDiagram();

    assertTrue(diagram.containsKey(NegotiationResourceState.SUBMITTED.name()));
    Map<String, Object> fromSubmitted =
        (Map<String, Object>) diagram.get(NegotiationResourceState.SUBMITTED.name());
    assertTrue(fromSubmitted.containsKey(NegotiationResourceEvent.CONTACT.name()));
    Map<String, Object> contactTransition =
        (Map<String, Object>) fromSubmitted.get(NegotiationResourceEvent.CONTACT.name());
    assertEquals(
        NegotiationResourceState.REPRESENTATIVE_CONTACTED.name(), contactTransition.get("target"));
    assertEquals(NegotiationResourceEvent.CONTACT.name(), contactTransition.get("event"));
    assertFalse(diagram.containsKey(NegotiationResourceState.RESOURCE_MADE_AVAILABLE.name()));
  }
}
