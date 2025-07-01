package eu.bbmri_eric.negotiator.notification;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.hateoas.server.core.Relation;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Relation(itemRelation = "notification", collectionRelation = "notifications")
public class NotificationDTO {
  private Long id;
  private String negotiationId;

  private LocalDateTime creationDate;
  private String title;
  private String message;
}
