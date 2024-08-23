package eu.bbmri_eric.negotiator.notification;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class NotificationDTO {
  private Long id;
  private String negotiationId;

  private LocalDateTime creationDate;

  private String message;
}
