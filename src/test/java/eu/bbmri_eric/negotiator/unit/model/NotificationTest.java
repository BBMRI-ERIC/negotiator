package eu.bbmri_eric.negotiator.unit.model;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import eu.bbmri_eric.negotiator.database.model.Notification;
import eu.bbmri_eric.negotiator.database.model.Person;
import eu.bbmri_eric.negotiator.database.model.negotiation.Negotiation;
import org.junit.jupiter.api.Test;

public class NotificationTest {

  @Test
  void buildNotification_ok() {
    Notification notification =
        Notification.builder()
            .id(1L)
            .recipient(new Person())
            .negotiation(new Negotiation())
            .build();
    assertInstanceOf(Notification.class, notification);
  }
}
