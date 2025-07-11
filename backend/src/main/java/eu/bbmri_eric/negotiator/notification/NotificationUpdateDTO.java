package eu.bbmri_eric.negotiator.notification;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
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
@Schema(description = "Data Transfer Object for updating notification read status")
public class NotificationUpdateDTO {

  @Schema(description = "Unique identifier of the notification", example = "12345")
  @NotNull(message = "Notification ID is required")
  private Long id;

  @Schema(description = "Read status of the notification", example = "true")
  @NotNull(message = "Read status is required")
  private Boolean read;
}
