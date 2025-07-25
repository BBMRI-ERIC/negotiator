package eu.bbmri_eric.negotiator.email;

import eu.bbmri_eric.negotiator.common.FilterDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/** Filters for the NotificationEmail controller. */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Schema(description = "Filter and pagination parameters for notification emails")
@Builder
public class NotificationEmailFilterDTO implements FilterDTO {

  @Min(value = 0, message = "Page number must be greater than or equal to 0")
  @Schema(description = "Page number (0-based)", example = "0", defaultValue = "0")
  private int page = 0;

  @Min(value = 1, message = "Page size must be greater than or equal to 1")
  @Schema(description = "Page size", example = "20", defaultValue = "20")
  private int size = 20;

  @Schema(description = "Filter by email address (partial match)", example = "user@example.com")
  private String address;

  @Schema(
      description = "Filter emails sent after this date (ISO format)",
      example = "2024-01-01T00:00:00")
  private LocalDateTime sentAfter;

  @Schema(
      description = "Filter emails sent before this date (ISO format)",
      example = "2024-12-31T23:59:59")
  private LocalDateTime sentBefore;

  @Schema(
      description = "Sort criteria (property,direction)",
      example = "sentAt,desc",
      defaultValue = "sentAt,desc")
  private String sort = "sentAt,desc";

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    NotificationEmailFilterDTO that = (NotificationEmailFilterDTO) o;
    return page == that.page
        && size == that.size
        && Objects.equals(address, that.address)
        && Objects.equals(sentAfter, that.sentAfter)
        && Objects.equals(sentBefore, that.sentBefore)
        && Objects.equals(sort, that.sort);
  }

  @Override
  public int hashCode() {
    return Objects.hash(page, size, address, sentAfter, sentBefore, sort);
  }
}
