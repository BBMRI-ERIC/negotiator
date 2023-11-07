package eu.bbmri.eric.csit.service.negotiator.unit.model;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import eu.bbmri.eric.csit.service.negotiator.database.model.Notification;
import eu.bbmri.eric.csit.service.negotiator.database.model.Person;
import org.junit.jupiter.api.Test;

public class NotificationTest {

  @Test
  void buildNotification_ok() {
    Notification notification = Notification.builder().id(1L).recipient(new Person()).build();
    assertInstanceOf(Notification.class, notification);
  }
}
