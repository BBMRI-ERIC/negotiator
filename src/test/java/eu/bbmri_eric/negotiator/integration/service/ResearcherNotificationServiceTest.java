package eu.bbmri_eric.negotiator.integration.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationEvent;
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
}
