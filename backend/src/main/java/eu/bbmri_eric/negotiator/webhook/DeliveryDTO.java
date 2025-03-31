package eu.bbmri_eric.negotiator.webhook;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** DTO representing a response for a Delivery. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO representing a Delivery response")
public class DeliveryDTO {

  /** Unique identifier of the delivery. */
  @Schema(
      description = "Unique identifier of the delivery",
      example = "123e4567-e89b-12d3-a456-426614174000")
  private String id;

  /** ID of the associated webhook. */
  @Schema(description = "ID of the associated webhook", example = "1")
  private Long webhookId;

  /** Indicates if the delivery was successful. */
  @Schema(description = "Indicates if the delivery was successful", example = "false")
  private boolean successful;

  /** JSON content of the delivery. */
  @Schema(description = "JSON content of the delivery", example = "{\"key\":\"value\"}")
  private String content;

  /** Optional HTTP status code from the delivery attempt. */
  private Integer httpStatusCode;

  /** Optional error message if the delivery attempt failed. */
  private String errorMessage;

  /** Timestamp when the delivery was created. */
  @Schema(description = "Timestamp when the delivery was created", example = "2023-10-01T12:00:00")
  private LocalDateTime at;
}
