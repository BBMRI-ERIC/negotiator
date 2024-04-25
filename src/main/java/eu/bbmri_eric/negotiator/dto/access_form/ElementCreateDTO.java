package eu.bbmri_eric.negotiator.dto.access_form;

import com.fasterxml.jackson.annotation.JsonInclude;
import eu.bbmri_eric.negotiator.dto.FormElementType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class ElementCreateDTO {
  @NotNull private String name;

  @NotNull private String label;

  @NotNull private String description;

  @NotNull private FormElementType type;
}
