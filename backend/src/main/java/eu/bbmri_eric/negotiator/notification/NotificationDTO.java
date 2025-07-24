package eu.bbmri_eric.negotiator.notification;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Data Transfer Object for Notification entity")
public class NotificationDTO {

  @Schema(description = "Unique identifier of the notification", example = "12345")
  private Long id;

  @Schema(description = "Unique identifier of the notification recipient", example = "67890")
  private Long recipientId;

  @Schema(description = "Identifier of the associated negotiation", example = "NEG-2024-001")
  private String negotiationId;

  @Schema(
      description = "Date and time when the notification was created",
      example = "2024-07-08T10:30:00")
  private LocalDateTime createdAt;

  @Schema(description = "Title of the notification", example = "New negotiation request")
  private String title;

  @Schema(
      description = "Message content of the notification",
      example = "A new negotiation request has been submitted for your review.")
  private String message;

  @Schema(description = "Whether the notification has been read", example = "false")
  private boolean read;
}
