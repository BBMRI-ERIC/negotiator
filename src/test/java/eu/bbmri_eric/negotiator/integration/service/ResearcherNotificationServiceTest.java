package eu.bbmri_eric.negotiator.integration.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationEvent;
import eu.bbmri_eric.negotiator.notification.Notification;
import eu.bbmri_eric.negotiator.notification.NotificationRepository;
import eu.bbmri_eric.negotiator.notification.researcher.ResearcherNotificationService;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest(loadTestData = true)
public class ResearcherNotificationServiceTest {
  @Autowired ResearcherNotificationService service;
  @Autowired NotificationRepository notificationRepository;

  @Test
  void handleStatusChange_ok() {
    long count = notificationRepository.count();
    service.statusChangeNotification("negotiation-1", NegotiationEvent.APPROVE);
    assertEquals(count + 1, notificationRepository.count());
  }

  @Test
  void handleStatusChangeWithPost_ok() {
    long countNotification = notificationRepository.count();
    service.statusChangeNotification("negotiation-1", NegotiationEvent.DECLINE, "not acceptable");
    assertEquals(countNotification + 1, notificationRepository.count());
    Notification n =
        notificationRepository.findAll().stream()
            .min((n1, n2) -> n2.getId().intValue() - n1.getId().intValue())
            .orElse(null);
    assertTrue(n.getMessage().contains("not acceptable"));
  }
}
