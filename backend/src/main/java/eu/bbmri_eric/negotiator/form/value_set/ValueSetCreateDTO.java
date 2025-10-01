package eu.bbmri_eric.negotiator.form.value_set;

import java.io.Serializable;
import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO for {@link ValueSetDTO} */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ValueSetCreateDTO implements Serializable {
    @NotEmpty(message = "Name must be provided")
  private String name;
  private String externalDocumentation;
  @NotEmpty(message = "At least one value must be provided")
  private List<String> availableValues;
}
