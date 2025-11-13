package eu.bbmri_eric.negotiator.form.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;
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
public class AccessFormUpdateSectionDTO {
  @Schema(description = "The ID of the section to add in the access form", example = "1")
  @NotNull
  Long id;

  @Schema(
      description =
          "The list of element that will be part of the section in the updated access form, in the order they should appear")
  @NotNull
  private List<AccessFormUpdateElementDTO> elements;
}
