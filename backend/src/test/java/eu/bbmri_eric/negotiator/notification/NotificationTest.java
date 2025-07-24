package eu.bbmri_eric.negotiator.notification;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;

public class NotificationTest {

  @Test
  void buildNotification_ok() {
    Notification notification =
        Notification.builder().id(1L).recipientId(1L).negotiationId("testId").build();
    assertInstanceOf(Notification.class, notification);
  }
}
