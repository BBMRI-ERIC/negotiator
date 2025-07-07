package eu.bbmri_eric.negotiator.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import eu.bbmri_eric.negotiator.user.Person;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest(loadTestData = true)
public class AdminNotificationServiceTest {
  @Autowired PersonRepository personRepository;
  @Autowired NotificationRepository notificationRepository;
  @Autowired
  AdminNotificationService adminNotificationService;

  @Test
  void notifyAllAdmins_ok() {
    Set<Person> admins = new HashSet<>(personRepository.findAllByAdminIsTrue());
    Set<Person> notAdmins = new HashSet<>(personRepository.findAllByAdminIsFalse());
    assertFalse(admins.isEmpty());
    assertFalse(notAdmins.isEmpty());
    Map<Long, Integer> adminCounts = new HashMap<>();
    Map<Long, Integer> notAdminCounts = new HashMap<>();
    admins.forEach(
        admin -> {
          int count = notificationRepository.findAllByRecipientId(admin.getId()).size();
          adminCounts.put(admin.getId(), count);
        });
    notAdmins.forEach(
        person -> {
          int count = notificationRepository.findAllByRecipientId(person.getId()).size();
          notAdminCounts.put(person.getId(), count);
        });
    adminNotificationService.notifyAllAdmins("Important Message", "message");
    adminCounts.forEach(
        (id, count) -> {
          int newCount = notificationRepository.findAllByRecipientId(id).size();
          assertEquals(count + 1, newCount);
        });
    notAdminCounts.forEach(
        (id, count) -> {
          int newCount = notificationRepository.findAllByRecipientId(id).size();
          assertEquals(count, newCount);
        });
  }
}
