package eu.bbmri_eric.negotiator.integration.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.bbmri_eric.negotiator.discovery.DiscoveryService;
import eu.bbmri_eric.negotiator.discovery.DiscoveryServiceRepository;
import eu.bbmri_eric.negotiator.form.repository.AccessFormRepository;
import eu.bbmri_eric.negotiator.governance.resource.ResourceRepository;
import eu.bbmri_eric.negotiator.info_requirement.InformationRequirementRepository;
import eu.bbmri_eric.negotiator.info_submission.InformationSubmissionRepository;
import eu.bbmri_eric.negotiator.integration.api.v3.TestUtils;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.NegotiationService;
import eu.bbmri_eric.negotiator.negotiation.dto.NegotiationCreateDTO;
import eu.bbmri_eric.negotiator.negotiation.dto.NegotiationDTO;
import eu.bbmri_eric.negotiator.negotiation.request.Request;
import eu.bbmri_eric.negotiator.negotiation.request.RequestRepository;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationLifecycleServiceImpl;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceState;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.ResourceLifecycleService;
import eu.bbmri_eric.negotiator.notification.NotificationRepository;
import eu.bbmri_eric.negotiator.notification.RepresentativeNotificationService;
import eu.bbmri_eric.negotiator.post.Post;
import eu.bbmri_eric.negotiator.post.PostRepository;
import eu.bbmri_eric.negotiator.post.PostType;
import eu.bbmri_eric.negotiator.user.Person;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import eu.bbmri_eric.negotiator.util.WithMockNegotiatorUser;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashSet;
import org.junit.jupiter.api.Test;
import org.opentest4j.TestAbortedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

@IntegrationTest(loadTestData = true)
@Transactional
@RecordApplicationEvents
public class RepresentativeNotificationsTest {

  @Autowired NegotiationLifecycleServiceImpl negotiationLifecycleService;
  @Autowired ResourceLifecycleService resourceLifecycleService;
  @Autowired NegotiationRepository negotiationRepository;
  @Autowired NegotiationService negotiationService;
  @Autowired RequestRepository requestRepository;
  @Autowired InformationRequirementRepository requirementRepository;
  @Autowired AccessFormRepository accessFormRepository;
  @Autowired private InformationSubmissionRepository informationSubmissionRepository;
  @Autowired private ResourceRepository resourceRepository;
  @Autowired private PostRepository postRepository;
  @Autowired ApplicationEvents events;
  @Autowired private DiscoveryServiceRepository discoveryServiceRepository;
  @Autowired RepresentativeNotificationService representativeNotificationService;
  @Autowired NotificationRepository notificationRepository;
  @Autowired private PersonRepository personRepository;

  @Test
  @WithMockNegotiatorUser(id = 109L, authorities = "ROLE_ADMIN")
  void notifyForPendingNegotiations_noPostOrUpdate_repNotifiedOnce() throws IOException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    Person person = personRepository.findAll().iterator().next();
    negotiationLifecycleService.sendEvent(negotiationDTO.getId(), NegotiationEvent.APPROVE);
    Negotiation negotiation = negotiationRepository.findById(negotiationDTO.getId()).get();
    negotiation.setCreationDate(LocalDateTime.now().minusDays(5));
    negotiation.setCreatedBy(person);
    negotiationRepository.saveAndFlush(negotiation);
    Person representative =
        negotiation.getResources().iterator().next().getRepresentatives().iterator().next();
    int count = notificationRepository.findAllByRecipientId(representative.getId()).size();
    representativeNotificationService.notifyAboutPendingNegotiations();
    assertEquals(
        NegotiationResourceState.REPRESENTATIVE_CONTACTED,
        negotiationRepository
            .findById(negotiationDTO.getId())
            .get()
            .getCurrentStateForResource("biobank:1:collection:2"));
    assertEquals(
        count + 1, notificationRepository.findAllByRecipientId(representative.getId()).size());
  }

  @Test
  @WithMockNegotiatorUser(id = 109L, authorities = "ROLE_ADMIN")
  void notifyForPendingNegotiations_statusUpdate_noNotification() throws IOException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    Person person = personRepository.findAll().iterator().next();
    negotiationLifecycleService.sendEvent(negotiationDTO.getId(), NegotiationEvent.APPROVE);
    Negotiation negotiation = negotiationRepository.findById(negotiationDTO.getId()).get();
    negotiation.setCreationDate(LocalDateTime.now().minusDays(5));
    negotiation.setCreatedBy(person);
    Person representative =
        negotiation.getResources().iterator().next().getRepresentatives().iterator().next();
    negotiation.setStateForResource(
        negotiation.getResources().iterator().next().getSourceId(),
        NegotiationResourceState.RESOURCE_UNAVAILABLE);
    representativeNotificationService.notifyAboutPendingNegotiations();
    int count = notificationRepository.findAllByRecipientId(representative.getId()).size();
    assertEquals(
        NegotiationResourceState.RESOURCE_UNAVAILABLE,
        negotiationRepository
            .findById(negotiationDTO.getId())
            .get()
            .getCurrentStateForResource("biobank:1:collection:2"));
    assertEquals(count, notificationRepository.findAllByRecipientId(representative.getId()).size());
  }

  @Test
  @WithMockNegotiatorUser(id = 109L, authorities = "ROLE_ADMIN")
  void notifyForPendingNegotiation_newPostByARep_noNotification() throws IOException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    Person person = personRepository.findAll().iterator().next();
    negotiationLifecycleService.sendEvent(negotiationDTO.getId(), NegotiationEvent.APPROVE);
    Negotiation negotiation = negotiationRepository.findById(negotiationDTO.getId()).get();
    negotiation.setCreationDate(LocalDateTime.now().minusDays(5));
    negotiation.setCreatedBy(person);
    Person representative =
        negotiation.getResources().iterator().next().getRepresentatives().iterator().next();
    Post post = new Post(negotiation, null, "", PostType.PUBLIC);
    postRepository.save(post);
    representativeNotificationService.notifyAboutPendingNegotiations();
    int count = notificationRepository.findAllByRecipientId(representative.getId()).size();
    assertEquals(
        NegotiationResourceState.REPRESENTATIVE_CONTACTED,
        negotiationRepository
            .findById(negotiationDTO.getId())
            .get()
            .getCurrentStateForResource("biobank:1:collection:2"));
    assertEquals(count, notificationRepository.findAllByRecipientId(representative.getId()).size());
  }

  @Test
  @WithMockNegotiatorUser(id = 109L, authorities = "ROLE_ADMIN")
  void notifyForPendingNegotiation_2OrganizationsOnlyOneIgnoring_oneIsNotified()
      throws IOException, InterruptedException {
    NegotiationDTO negotiationDTO = saveNegotiation();
    Person person = personRepository.findAll().iterator().next();
    negotiationLifecycleService.sendEvent(negotiationDTO.getId(), NegotiationEvent.APPROVE);
    Negotiation negotiation = negotiationRepository.findById(negotiationDTO.getId()).get();
    assertEquals(2, negotiation.getResources().size());
    assertEquals(2, negotiation.getOrganizations().size());
    negotiation
        .getResources()
        .forEach(
            resource ->
                assertEquals(
                    NegotiationResourceState.REPRESENTATIVE_CONTACTED,
                    negotiation.getCurrentStateForResource(resource.getSourceId())));
    negotiation.setCreationDate(LocalDateTime.now().minusDays(5));
    negotiation.setCreatedBy(person);
    negotiation.setStateForResource(
        negotiation.getResources().iterator().next().getSourceId(),
        NegotiationResourceState.RESOURCE_UNAVAILABLE);
    long before = notificationRepository.count();
    representativeNotificationService.notifyAboutPendingNegotiations();
    assertEquals(before + 1L, notificationRepository.count());
  }

  private NegotiationDTO saveNegotiation() throws IOException {
    NegotiationCreateDTO negotiationCreateDTO = TestUtils.createNegotiation("request-2", false);
    DiscoveryService discoveryService =
        discoveryServiceRepository.findById(1L).orElseThrow(TestAbortedException::new);
    Request request =
        requestRepository.findById("request-2").orElseThrow(TestAbortedException::new);
    Negotiation negotiation =
        Negotiation.builder()
            .resources(new HashSet<>(request.getResources()))
            .discoveryService(discoveryService)
            .humanReadable("#1 MaterialType: DNA")
            .payload(negotiationCreateDTO.getPayload().toString())
            .build();
    negotiation.setCreatedBy(Person.builder().id(101L).name("TheBuilder").build());
    negotiationRepository.saveAndFlush(negotiation);
    return negotiationService.create(negotiationCreateDTO, 101L);
  }
}
