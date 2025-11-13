package eu.bbmri_eric.negotiator.form.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccessFormUpdateElementDTO {
  @Schema(description = "The ID of the element to add in the access form", example = "1")
  @NotNull
  private Long id;

  @NotNull private Boolean required;
}
