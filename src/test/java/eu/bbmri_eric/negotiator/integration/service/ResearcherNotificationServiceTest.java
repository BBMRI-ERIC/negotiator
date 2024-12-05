package eu.bbmri_eric.negotiator.integration.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationEvent;
import eu.bbmri_eric.negotiator.notification.Notification;
import eu.bbmri_eric.negotiator.notification.NotificationRepository;
import eu.bbmri_eric.negotiator.notification.email.NotificationEmailStatus;
import eu.bbmri_eric.negotiator.notification.researcher.ResearcherNotificationService;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest(loadTestData = true)
public class ResearcherNotificationServiceTest {
  @Autowired ResearcherNotificationService service;
  @Autowired NotificationRepository notificationRepository;
  @Autowired NegotiationRepository negotiationRepository;

  @Test
  @Transactional
  void handleCreateConfirmation_Ok() {
    long count = notificationRepository.count();
    service.createConfirmationNotification("negotiation-1");
    assertEquals(count + 1, notificationRepository.count());

    Notification notification =
        notificationRepository.findAll().stream()
            .min((n1, n2) -> n2.getCreationDate().compareTo(n1.getCreationDate()))
            .orElse(null);

    Negotiation negotiation = negotiationRepository.findById("negotiation-1").orElse(null);

    assertNotNull(negotiation);
    assertNotNull(notification);
    assertEquals(negotiation.getCreatedBy(), notification.getRecipient());
    assertEquals(negotiation, notification.getNegotiation());
    assertEquals("Request Confirmation", notification.getTitle());
    assertTrue(
        notification
            .getMessage()
            .startsWith("Request %s was successfully submitted".formatted(negotiation.getId())));
    assertEquals(NotificationEmailStatus.EMAIL_NOT_SENT, notification.getEmailStatus());
  }

  @Test
  void handleCreateConfirmation_noNotificationWhenNegotiationNotFound() {
    long countNotification = notificationRepository.count();
    service.createConfirmationNotification("unknown");
    assertEquals(countNotification, notificationRepository.count());
  }

  @Test
  @Transactional
  void handleStatusChange_ApproveNoMessageOk() {
    long count = notificationRepository.count();
    service.statusChangeNotification("negotiation-1", NegotiationEvent.APPROVE);
    assertEquals(count + 1, notificationRepository.count());

    Notification notification =
        notificationRepository.findAll().stream()
            .min((n1, n2) -> n2.getCreationDate().compareTo(n1.getCreationDate()))
            .orElse(null);

    Negotiation negotiation = negotiationRepository.findById("negotiation-1").orElse(null);

    assertNotNull(negotiation);
    assertNotNull(notification);
    assertEquals(negotiation.getCreatedBy(), notification.getRecipient());
    assertEquals(negotiation, notification.getNegotiation());
    assertEquals("Request status update", notification.getTitle());
    assertTrue(
        notification.getMessage().startsWith("The request was approved by an Administrator"));
    assertEquals(NotificationEmailStatus.EMAIL_NOT_SENT, notification.getEmailStatus());
  }

  @Test
  @Transactional
  void handleStatusChange_DeclineWithMessageOk() {
    long countNotification = notificationRepository.count();
    service.statusChangeNotification("negotiation-1", NegotiationEvent.DECLINE, "not acceptable");
    assertEquals(countNotification + 1, notificationRepository.count());

    Notification notification =
        notificationRepository.findAll().stream()
            .min((n1, n2) -> n2.getCreationDate().compareTo(n1.getCreationDate()))
            .orElse(null);

    Negotiation negotiation = negotiationRepository.findById("negotiation-1").orElse(null);

    assertNotNull(negotiation);
    assertNotNull(notification);
    assertEquals(negotiation.getCreatedBy(), notification.getRecipient());
    assertEquals(negotiation, notification.getNegotiation());
    assertEquals("Request status update", notification.getTitle());
    assertTrue(
        notification
            .getMessage()
            .contains("The Administrator added the following comment: not acceptable"));
    assertEquals(NotificationEmailStatus.EMAIL_NOT_SENT, notification.getEmailStatus());
  }

  @Test
  void handleStatusChange_noNotificationWhenNegotiationNotFound() {
    long countNotification = notificationRepository.count();
    service.statusChangeNotification("unknown", NegotiationEvent.DECLINE, "not acceptable");
    assertEquals(countNotification, notificationRepository.count());
  }
}
