package eu.bbmri_eric.negotiator.webhook;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** DTO for creating a new Delivery. */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@Schema(description = "DTO for creating a new Delivery")
public class DeliveryCreateDTO {

  /** JSON content of the delivery. */
  @NotNull
  @Schema(description = "JSON content of the delivery", example = "{\"key\":\"value\"}")
  private String content;
}
