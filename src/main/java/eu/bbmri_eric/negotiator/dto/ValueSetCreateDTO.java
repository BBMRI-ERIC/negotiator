package eu.bbmri_eric.negotiator.dto;

import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO for {@link ValueSetDTO} */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ValueSetCreateDTO implements Serializable {
  private String name;
  private String externalDocumentation;
  private List<String> availableValues;
}
