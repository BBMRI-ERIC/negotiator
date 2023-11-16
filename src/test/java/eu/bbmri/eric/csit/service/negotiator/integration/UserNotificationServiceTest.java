package eu.bbmri.eric.csit.service.negotiator.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.bbmri.eric.csit.service.negotiator.configuration.state_machine.resource.NegotiationResourceState;
import eu.bbmri.eric.csit.service.negotiator.database.model.Negotiation;
import eu.bbmri.eric.csit.service.negotiator.database.model.Notification;
import eu.bbmri.eric.csit.service.negotiator.database.model.NotificationEmailStatus;
import eu.bbmri.eric.csit.service.negotiator.database.model.Person;
import eu.bbmri.eric.csit.service.negotiator.database.model.Resource;
import eu.bbmri.eric.csit.service.negotiator.database.repository.NegotiationRepository;
import eu.bbmri.eric.csit.service.negotiator.database.repository.NotificationRepository;
import eu.bbmri.eric.csit.service.negotiator.database.repository.PersonRepository;
import eu.bbmri.eric.csit.service.negotiator.database.repository.ResourceRepository;
import eu.bbmri.eric.csit.service.negotiator.service.NegotiationLifecycleService;
import eu.bbmri.eric.csit.service.negotiator.service.UserNotificationService;
import eu.bbmri.eric.csit.service.negotiator.unit.context.WithMockNegotiatorUser;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class UserNotificationServiceTest {

  @Autowired UserNotificationService userNotificationService;
  @Autowired PersonRepository personRepository;
  @Autowired NotificationRepository notificationRepository;
  @Autowired NegotiationRepository negotiationRepository;
  @Autowired ResourceRepository resourceRepository;
  @Autowired NegotiationLifecycleService negotiationLifecycleService;

  @Test
  void getNotifications_nonExistentUser_0() {
    assertEquals(0, userNotificationService.getNotificationsForUser(19999L).size());
  }

  @Test
  void getNotifications_1_ok() {
    Person person =
        personRepository.save(
            Person.builder()
                .authSubject("823")
                .authName("John")
                .authEmail("test@test.com")
                .build());
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
  @WithMockNegotiatorUser(id = 109L, roles = "ADMIN")
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
  @WithMockNegotiatorUser(id = 109L, roles = "ADMIN")
  void notifyRepresentatives_sameRepFor2Resources_oneNotification() {
    Negotiation negotiation = negotiationRepository.findAll().get(0);
    Resource resource1 =
        negotiation.getRequests().iterator().next().getResources().iterator().next();
    Person representative = resource1.getRepresentatives().iterator().next();
    Resource resource2 = resourceRepository.findBySourceId("biobank:1:collection:2").get();
    Set<Resource> resources = negotiation.getRequests().iterator().next().getResources();
    resources.add(resource2);
    negotiation.getRequests().iterator().next().setResources(resources);
    negotiation = negotiationRepository.save(negotiation);
    assertEquals(2, negotiation.getResources().size());
    assertTrue(resource2.getRepresentatives().contains(representative));
    assertTrue(notificationRepository.findByRecipientId(representative.getId()).isEmpty());
    userNotificationService.notifyRepresentativesAboutNewNegotiation(negotiation);
    assertEquals(1, notificationRepository.findByRecipientId(representative.getId()).size());
    Negotiation updatedNegotiation = negotiationRepository.findById(negotiation.getId()).get();
    assertEquals(
        NegotiationResourceState.REPRESENTATIVE_CONTACTED,
        updatedNegotiation.getCurrentStatePerResource().get("biobank:1:collection:2"));
  }

  @Test
  @WithMockNegotiatorUser(id = 109L, roles = "ADMIN")
  void notifyRepresentatives_resWithNoRep_markedAsUnreachable() {
    Negotiation negotiation = negotiationRepository.findById("negotiation-1").get();
    Resource resource = resourceRepository.findBySourceId("biobank:1:collection:1").get();
    resource.setRepresentatives(Set.of());
    Resource resource2 = resourceRepository.save(resource);
    System.out.println(resource2.getRepresentatives().size());
    negotiation = negotiationRepository.findById(negotiation.getId()).get();
    Resource resourceWithoutReps =
        negotiation.getResources().stream()
            .filter(res -> res.getSourceId().equals(resource.getSourceId()))
            .toList()
            .get(0);
    System.out.println(resourceWithoutReps.getSourceId());
    assertTrue(resourceWithoutReps.getRepresentatives().isEmpty());
    userNotificationService.notifyRepresentativesAboutNewNegotiation(negotiation);
    assertEquals(
        NegotiationResourceState.REPRESENTATIVE_UNREACHABLE,
        negotiationRepository
            .findById(negotiation.getId())
            .get()
            .getCurrentStatePerResource()
            .get(resourceWithoutReps.getSourceId()));
  }
}
