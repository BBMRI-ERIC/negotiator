package eu.bbmri.eric.csit.service.negotiator.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.bbmri.eric.csit.service.negotiator.database.model.Negotiation;
import eu.bbmri.eric.csit.service.negotiator.database.model.Notification;
import eu.bbmri.eric.csit.service.negotiator.database.model.NotificationEmailStatus;
import eu.bbmri.eric.csit.service.negotiator.database.model.Person;
import eu.bbmri.eric.csit.service.negotiator.database.model.Resource;
import eu.bbmri.eric.csit.service.negotiator.database.repository.NegotiationRepository;
import eu.bbmri.eric.csit.service.negotiator.database.repository.NotificationRepository;
import eu.bbmri.eric.csit.service.negotiator.database.repository.PersonRepository;
import eu.bbmri.eric.csit.service.negotiator.database.repository.ResourceRepository;
import eu.bbmri.eric.csit.service.negotiator.service.UserNotificationService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public class UserNotificationServiceTest {

  @Autowired UserNotificationService userNotificationService;
  @Autowired PersonRepository personRepository;
  @Autowired NotificationRepository notificationRepository;
  @Autowired NegotiationRepository negotiationRepository;
  @Autowired ResourceRepository resourceRepository;

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
  void notifyRepresentatives_sameRepFor2Resources_oneNotification() {
    Negotiation negotiation = negotiationRepository.findAll().get(0);
    Resource resource1 = resourceRepository.findBySourceId("biobank:1:collection:2").get();
    Set<Resource> resources = negotiation.getRequests().iterator().next().getResources();
    resources.add(resource1);
    negotiation.getRequests().iterator().next().setResources(resources);
    negotiation = negotiationRepository.save(negotiation);
    assertEquals(2, negotiation.getResources().size());
    // TODO finish this test
  }
}
