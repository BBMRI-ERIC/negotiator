package eu.bbmri_eric.negotiator.notification;

import eu.bbmri_eric.negotiator.common.FilterDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** DTO that defines the possible query filters for Notifications */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Parameters to filter the Notifications")
public class NotificationFilters implements FilterDTO {
  @Schema(description = "The page number required", example = "0")
  @Min(value = 0, message = "Page number must be greater than or equal to 0")
  private int page = 0;

  @Schema(description = "The size of the pages required", example = "50")
  @Min(value = 1, message = "Page size must be greater than or equal to 1")
  private int size = 50;
}
