package eu.bbmri_eric.negotiator.integration.service;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.governance.resource.ResourceRepository;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.NegotiationService;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceState;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.ResourceLifecycleService;
import eu.bbmri_eric.negotiator.notification.Notification;
import eu.bbmri_eric.negotiator.notification.NotificationRepository;
import eu.bbmri_eric.negotiator.notification.UserNotificationService;
import eu.bbmri_eric.negotiator.notification.email.NotificationEmailRepository;
import eu.bbmri_eric.negotiator.notification.email.NotificationEmailStatus;
import eu.bbmri_eric.negotiator.post.PostCreateDTO;
import eu.bbmri_eric.negotiator.post.PostService;
import eu.bbmri_eric.negotiator.post.PostType;
import eu.bbmri_eric.negotiator.user.Person;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import eu.bbmri_eric.negotiator.util.WithMockNegotiatorUser;
import jakarta.transaction.Transactional;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.opentest4j.TestAbortedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.reactive.function.UnsupportedMediaTypeException;
import org.springframework.test.annotation.DirtiesContext;

@IntegrationTest(loadTestData = true)
@Transactional
class UserNotificationServiceTest {

  @Autowired UserNotificationService userNotificationService;
  @Autowired PersonRepository personRepository;
  @Autowired NotificationRepository notificationRepository;
  @Autowired NegotiationRepository negotiationRepository;
  @Autowired ResourceRepository resourceRepository;
  @Autowired ResourceLifecycleService resourceLifecycleService;
  @Autowired NotificationEmailRepository notificationEmailRepository;
  @Autowired PostService postService;
  @Autowired NegotiationService negotiationService;

  @Test
  void getNotifications_nonExistentUser_0() {
    assertEquals(0, userNotificationService.getNotificationsForUser(19999L).size());
  }

  @Test
  void getNotifications_1_ok() {
    Person person =
        personRepository.save(
            Person.builder().subjectId("823").name("John").email("test@test.com").build());
    Negotiation negotiation = negotiationRepository.findAll().get(0);
    notificationRepository.save(
        Notification.builder()
            .negotiation(negotiation)
            .emailStatus(NotificationEmailStatus.EMAIL_NOT_SENT)
            .recipient(person)
            .message("New")
            .build());
    assertEquals(1, userNotificationService.getNotificationsForUser(person.getId()).size());
  }

  @Test
  void notifyAdmins_atLeastOneAdminInDB_ok() {
    assertFalse(personRepository.findAllByAdminIsTrue().isEmpty());
    List<Person> admins = personRepository.findAllByAdminIsTrue();
    Negotiation negotiation = negotiationRepository.findAll().get(0);
    userNotificationService.notifyAdmins(negotiation);
    for (Person admin : admins) {
      assertTrue(
          notificationRepository.findByRecipientId(admin.getId()).stream()
              .anyMatch(
                  notification ->
                      notification.getNegotiation().getId().equals(negotiation.getId())));
    }
  }

  @Test
  @WithMockNegotiatorUser(
      id = 109L,
      authorities = {"ROLE_ADMIN"})
  void notifyRepresentativesAboutNewNegotiation_atLeastOne_ok() {
    Negotiation negotiation = negotiationRepository.findAll().get(0);
    assertTrue(
        negotiation.getResources().stream()
            .anyMatch(resource -> !resource.getRepresentatives().isEmpty()));
    List<Person> representatives =
        negotiation.getResources().stream()
            .map(Resource::getRepresentatives)
            .flatMap(Set::stream)
            .toList();
    for (Person rep : representatives) {
      assertTrue(notificationRepository.findByRecipientId(rep.getId()).isEmpty());
    }
    userNotificationService.notifyRepresentativesAboutNewNegotiation(negotiation);
    for (Person rep : representatives) {
      assertFalse(notificationRepository.findByRecipientId(rep.getId()).isEmpty());
      assertTrue(
          notificationRepository.findByRecipientId(rep.getId()).stream()
              .anyMatch(
                  notification ->
                      notification.getNegotiation().getId().equals(negotiation.getId())));
    }
  }

  @Test
  @WithMockNegotiatorUser(
      id = 109L,
      authorities = {"ROLE_ADMIN"})
  void notifyRepresentatives_sameRepFor2Resources_oneNotification() {
    Resource resource1 =
        resourceRepository
            .findBySourceId("biobank:1:collection:2")
            .orElseThrow(TestAbortedException::new);
    Person representative =
        resource1.getRepresentatives().stream().findFirst().orElseThrow(TestAbortedException::new);
    Resource resource2 =
        resourceRepository
            .findBySourceId("biobank:2:collection:1")
            .orElseThrow(TestAbortedException::new);
    resource2.setRepresentatives(Set.of(representative));

    Negotiation negotiation =
        Negotiation.builder()
            .resources(Set.of(resource1, resource2))
            .humanReadable("query")
            .discoveryService(resource1.getDiscoveryService())
            .payload("{\"project\":{\"title\":\"A Project 3\",\"description\":\"Project 3 desc\"}}")
            .build();
    negotiation.setStateForResource(resource2.getSourceId(), NegotiationResourceState.SUBMITTED);
    negotiation = negotiationRepository.save(negotiation);

    assertEquals(2, negotiation.getResources().size());
    assertTrue(resource2.getRepresentatives().contains(representative));
    assertTrue(notificationRepository.findByRecipientId(representative.getId()).isEmpty());
    userNotificationService.notifyRepresentativesAboutNewNegotiation(negotiation);
    assertEquals(1, notificationRepository.findByRecipientId(representative.getId()).size());
    Negotiation updatedNegotiation = negotiationRepository.findById(negotiation.getId()).get();
    assertEquals(
        NegotiationResourceState.REPRESENTATIVE_CONTACTED,
        updatedNegotiation.getCurrentStateForResource("biobank:1:collection:2"));
  }

  @Test
  @WithMockNegotiatorUser(
      id = 109L,
      authorities = {"ROLE_ADMIN"})
  void notifyRepresentatives_resWithNoRep_markedAsUnreachable() {
    Negotiation negotiation = negotiationRepository.findById("negotiation-1").get();
    Resource resource = resourceRepository.findBySourceId("biobank:1:collection:1").get();
    resource.setRepresentatives(Collections.emptySet());
    Resource resource2 = resourceRepository.save(resource);
    negotiation = negotiationRepository.findById(negotiation.getId()).get();
    Resource resourceWithoutReps =
        negotiation.getResources().stream()
            .filter(res -> res.getSourceId().equals(resource.getSourceId()))
            .toList()
            .get(0);
    assertTrue(resourceWithoutReps.getRepresentatives().isEmpty());
    userNotificationService.notifyRepresentativesAboutNewNegotiation(negotiation);
    assertEquals(
        NegotiationResourceState.REPRESENTATIVE_UNREACHABLE,
        negotiationRepository
            .findById(negotiation.getId())
            .get()
            .getCurrentStateForResource(resourceWithoutReps.getSourceId()));
  }

  @Test
  @WithMockNegotiatorUser(
      id = 109L,
      authorities = {"ROLE_ADMIN"})
  @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
  void notifyRepresentatives_called2Times_noNewEmailsSent() {
    notificationEmailRepository.deleteAll();
    assertTrue(notificationEmailRepository.findAll().isEmpty());
    Negotiation negotiation = negotiationRepository.findAll().get(0);
    assertTrue(
        negotiation.getResources().stream()
            .anyMatch(resource -> !resource.getRepresentatives().isEmpty()));
    userNotificationService.notifyRepresentativesAboutNewNegotiation(negotiation);
    userNotificationService.sendEmailsForNewNotifications();
    await()
        .atMost(1, TimeUnit.SECONDS)
        .until(() -> !notificationEmailRepository.findAll().isEmpty());
    int numOfEmails = notificationEmailRepository.findAll().size();
    userNotificationService.sendEmailsForNewNotifications();
    await()
        .atMost(2, TimeUnit.SECONDS)
        .until(() -> notificationEmailRepository.findAll().size() == numOfEmails);
    assertEquals(numOfEmails, notificationEmailRepository.findAll().size());
  }

  @Test
  @WithMockNegotiatorUser(
      id = 109L,
      authorities = {"ROLE_ADMIN"})
  void notifyRequester_resStatusChanged_ok() {
    Negotiation negotiation = negotiationRepository.findAll().get(0);
    assertTrue(
        notificationRepository.findByRecipientId(negotiation.getCreatedBy().getId()).isEmpty());
    Resource resource = negotiation.getResources().iterator().next();
    resourceLifecycleService.sendEvent(
        negotiation.getId(), resource.getSourceId(), NegotiationResourceEvent.MARK_AS_UNREACHABLE);
    assertFalse(
        notificationRepository.findByRecipientId(negotiation.getCreatedBy().getId()).isEmpty());
  }

  @Test
  @WithMockNegotiatorUser(id = 109L)
  void notifyUsersForNewPost_publicPost_authorIsNotNotified() {
    assertTrue(notificationRepository.findByRecipientId(109L).isEmpty());
    postService.create(
        PostCreateDTO.builder().type(PostType.PUBLIC).text("I know").build(), "negotiation-1");
    assertTrue(notificationRepository.findByRecipientId(109L).isEmpty());
  }

  @Test
  @WithMockNegotiatorUser(id = 109L)
  void notifyUsersForNewPost_publicPost_repsAreNotified() {
    Negotiation negotiation =
        negotiationRepository.findById("negotiation-1").orElseThrow(TestAbortedException::new);

    Resource resource1 =
        negotiation.getResources().stream().findFirst().orElseThrow(TestAbortedException::new);

    Person representative =
        resource1.getRepresentatives().stream()
            .filter(person -> !person.getId().equals(109L))
            .findFirst()
            .get();
    assertTrue(notificationRepository.findByRecipientId(representative.getId()).isEmpty());
    postService.create(
        PostCreateDTO.builder().type(PostType.PUBLIC).text("I know").build(), "negotiation-1");
    assertFalse(notificationRepository.findByRecipientId(representative.getId()).isEmpty());
  }

  @Test
  @WithMockNegotiatorUser(id = 109L)
  void notifyUsersForNewPost_privatePost_onlyRepsOfOrgAreNotified() {
    Negotiation negotiation =
        negotiationRepository.findById("negotiation-1").orElseThrow(TestAbortedException::new);
    Resource resource1 = negotiation.getResources().iterator().next();
    Person representative = resource1.getRepresentatives().stream().findFirst().get();
    assertTrue(Objects.nonNull(representative.getId()));
    assertTrue(notificationRepository.findByRecipientId(representative.getId()).isEmpty());
    postService.create(
        PostCreateDTO.builder()
            .type(PostType.PRIVATE)
            .organizationId(resource1.getOrganization().getExternalId())
            .text("I know")
            .build(),
        negotiation.getId());
    assertFalse(notificationRepository.findByRecipientId(representative.getId()).isEmpty());
  }

  @Test
  @WithMockNegotiatorUser(id = 103L)
  void notifyUsersForNewPost_createdByRep_requesterIsNotified() {
    Negotiation negotiation = negotiationRepository.findAll().get(0);
    assertTrue(
        notificationRepository.findByRecipientId(negotiation.getCreatedBy().getId()).isEmpty());
    Person postAuthor = personRepository.findById(103L).get();
    assertTrue(postAuthor.getResources().stream().anyMatch(negotiation.getResources()::contains));
    postService.create(
        PostCreateDTO.builder()
            .type(PostType.PRIVATE)
            .organizationId(
                postAuthor.getResources().iterator().next().getOrganization().getExternalId())
            .text("I know")
            .build(),
        negotiation.getId());
    assertFalse(
        notificationRepository.findByRecipientId(negotiation.getCreatedBy().getId()).isEmpty());
  }
}
