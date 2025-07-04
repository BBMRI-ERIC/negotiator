package eu.bbmri_eric.negotiator.unit.model;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import eu.bbmri_eric.negotiator.notification.Notification;
import org.junit.jupiter.api.Test;

public class NotificationTest {

  @Test
  void buildNotification_ok() {
    Notification notification =
        Notification.builder().id(1L).recipientId(1L).negotiationId("testId").build();
    assertInstanceOf(Notification.class, notification);
  }
}
