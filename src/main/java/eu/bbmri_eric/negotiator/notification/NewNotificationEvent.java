package eu.bbmri_eric.negotiator.notification;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/** Published when a New notification is created. */
@Getter
public class NewNotificationEvent extends ApplicationEvent {
  private final Long notificationId;
  private String emailTemplateName = "";

  public NewNotificationEvent(Object source, Long notificationId, String emailTemplateName) {
    super(source);
    this.notificationId = notificationId;
    this.emailTemplateName = emailTemplateName;
  }

  public NewNotificationEvent(Object source, Long notificationId) {
    super(source);
    this.notificationId = notificationId;
  }
}
