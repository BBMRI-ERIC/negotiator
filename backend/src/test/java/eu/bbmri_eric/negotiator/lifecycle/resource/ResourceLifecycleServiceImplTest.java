package eu.bbmri_eric.negotiator.lifecycle.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import eu.bbmri_eric.negotiator.lifecycle.TransitionPreconditionException;
import eu.bbmri_eric.negotiator.lifecycle.negotiation.NegotiationLifecycleService;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationEvent;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.NegotiationResourceEvent;
import eu.bbmri_eric.negotiator.negotiation.NegotiationResourceLifecycleRecord;
import eu.bbmri_eric.negotiator.negotiation.NegotiationResourceState;
import eu.bbmri_eric.negotiator.negotiation.NegotiationService;
import eu.bbmri_eric.negotiator.negotiation.dto.NegotiationCreateDTO;
import eu.bbmri_eric.negotiator.negotiation.dto.NegotiationDTO;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import eu.bbmri_eric.negotiator.util.WithMockNegotiatorUser;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.support.TransactionTemplate;

@IntegrationTest(loadTestData = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ResourceLifecycleServiceImplTest {

  @Autowired ResourceLifecycleService resourceLifecycleService;
  @Autowired NegotiationLifecycleService negotiationLifecycleService;
  @Autowired NegotiationService negotiationService;
  @Autowired NegotiationRepository negotiationRepository;
  @Autowired AccessFormRepository accessFormRepository;
  @Autowired InformationRequirementRepository requirementRepository;
  @Autowired InformationSubmissionRepository informationSubmissionRepository;
  @Autowired ResourceRepository resourceRepository;
  @Autowired TransactionTemplate transactionTemplate;

  private NegotiationDTO saveNegotiation() throws IOException {
    NegotiationCreateDTO negotiationCreateDTO = TestUtils.createNegotiation("request-2", false);
    return negotiationService.create(negotiationCreateDTO, 109L);
  }

  private void checkNegotiationResourceRecordPresenceWithAssignedState(
      String negotiationId, NegotiationResourceState state) {
    Negotiation negotiation = negotiationRepository.findDetailedById(negotiationId).get();
    Set<NegotiationResourceLifecycleRecord> records =
        negotiation.getNegotiationResourceLifecycleRecords();
    assertTrue(records.stream().anyMatch(r -> r.getChangedTo().equals(state)));
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
    negotiationLifecycleService.sendEvent(negotiationDTO.getId(), NegotiationEvent.APPROVE);
    AccessForm accessForm = accessFormRepository.findAll().stream().findFirst().get();
    requirementRepository.save(
        new InformationRequirement(accessForm, NegotiationResourceEvent.CONTACT));
    assertTrue(requirementRepository.existsByForEvent(NegotiationResourceEvent.CONTACT));
    assertThrows(
        TransitionPreconditionException.class,
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
}
