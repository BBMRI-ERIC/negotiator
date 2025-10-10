package eu.bbmri_eric.negotiator.form.dto;

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

  private String name;

  @NotNull @NotEmpty List<AccessFormUpdateSectionDTO> sections = new ArrayList<>();
}
