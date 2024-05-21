package eu.bbmri_eric.negotiator.database.model.views;

import eu.bbmri_eric.negotiator.database.model.NotificationEmailStatus;
import eu.bbmri_eric.negotiator.database.model.Person;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
