package eu.bbmri_eric.negotiator.notification;

import eu.bbmri_eric.negotiator.notification.email.NotificationEmailStatus;
import eu.bbmri_eric.negotiator.user.Person;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NotificationViewDTO {
  private Long id;
  private String message;
  private NotificationEmailStatus emailStatus;
  private String negotiationId;
  private String negotiationTitle;
  private Person recipient;
}
