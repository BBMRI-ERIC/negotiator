package eu.bbmri_eric.negotiator.form.dto;

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

  List<AccessFormUpdateSectionDTO> sections = new ArrayList<>();
}
