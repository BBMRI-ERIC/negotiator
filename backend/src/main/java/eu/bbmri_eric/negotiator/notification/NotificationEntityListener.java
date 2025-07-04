package eu.bbmri_eric.negotiator.notification;

import jakarta.persistence.PostPersist;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.stereotype.Component;

@Component
@CommonsLog
public class NotificationEntityListener {
  @PostPersist
  private void afterNewNotification(Notification notification) {
    log.debug("New notification: " + notification.getId());
  }
}
