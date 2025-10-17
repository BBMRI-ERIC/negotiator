package eu.bbmri_eric.negotiator.notification;

import static org.junit.jupiter.api.Assertions.*;

import eu.bbmri_eric.negotiator.notification.internal.AddedRepresentativeHandler;
import eu.bbmri_eric.negotiator.user.PersonService;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
@IntegrationTest(loadTestData = true)
public class AddedRepresentativeHandlerTest {

  @Autowired private PersonService personService;

  @Autowired private AddedRepresentativeHandler addedRepresentativeHandler;

  @Autowired private NotificationRepository notificationRepository;

  @Test
  public void handleRepresentativeAddedEvents_whenSingleRepresentative_ok() {
    personService.assignAsRepresentativeForResource(109L, 9L);
    personService.assignAsRepresentativeForResource(109L, 10L);
    addedRepresentativeHandler.flushEventBuffer();
    int notifications = notificationRepository.findAllByRecipientId(109L).size();
    assertEquals(1, notifications);
    personService.removeAsRepresentativeForResource(109L, 9L);
    personService.removeAsRepresentativeForResource(109L, 10L);
  }

  @Test
  public void handleRepresentativeAddedEvents_whenDifferentRepresentatives_ok() {
    personService.assignAsRepresentativeForResource(105L, 9L);
    personService.assignAsRepresentativeForResource(109L, 10L);
    addedRepresentativeHandler.flushEventBuffer();
    List<Notification> person1Notifications = notificationRepository.findAllByRecipientId(105L);
    assertEquals(1, person1Notifications.size());
    List<Notification> person2Notifications = notificationRepository.findAllByRecipientId(109L);
    assertEquals(2, person2Notifications.size());
    personService.removeAsRepresentativeForResource(105L, 9L);
    personService.removeAsRepresentativeForResource(109L, 10L);
  }
}
