package eu.bbmri_eric.negotiator.form.dto;

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
  @NotNull Long id;

  @NotNull private List<AccessFormUpdateElementDTO> elements;
}
