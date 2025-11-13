package eu.bbmri_eric.negotiator.form.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
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
public class AccessFormUpdateDTO {
  @Schema(
      description =
          "The list of sections that will be part of the updated access form, in the order they should appear")
  @NotNull
  @NotEmpty
  List<AccessFormUpdateSectionDTO> sections = new ArrayList<>();

  @Schema(description = "The new name of the access form", example = "Changed name")
  @NotNull
  private String name;
}
