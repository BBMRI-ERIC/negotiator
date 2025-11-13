package eu.bbmri_eric.negotiator.notification;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nonnull;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Data Transfer Object for creating new notifications")
public class NotificationCreateDTO {

  @Schema(
      description = "List of user IDs who will receive the notification",
      example = "[12345, 67890, 54321]",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @Nonnull
  private List<Long> userIds;

  @Schema(
      description = "Title of the notification",
      example = "New negotiation request",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @Nonnull
  private String title;

  @Schema(
      description = "Message body of the notification",
      example =
          "A new negotiation request has been submitted for your review. Please check the details and respond accordingly.",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @Nonnull
  private String body;

  @Schema(description = "Optional identifier of the associated negotiation", example = "2024-001")
  private String negotiationId;

  public NotificationCreateDTO(
      @Nonnull List<Long> userIds, @Nonnull String title, @Nonnull String body) {
    this.userIds = userIds;
    this.title = title;
    this.body = body;
  }

  public NotificationCreateDTO(
      @Nonnull List<Long> userIds,
      @Nonnull String title,
      @Nonnull String body,
      String negotiationId) {
    this.userIds = userIds;
    this.title = title;
    this.body = body;
    this.negotiationId = negotiationId;
  }
}
