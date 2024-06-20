package eu.bbmri_eric.negotiator.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** DTO for {@link eu.bbmri_eric.negotiator.database.model.ValueSet} */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ValueSetDTO {
  private Long id;
  private String name;
  private String externalDocumentation;
  private List<String> availableValues;
}
