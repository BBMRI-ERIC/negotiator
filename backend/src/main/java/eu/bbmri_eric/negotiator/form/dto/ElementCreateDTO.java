package eu.bbmri_eric.negotiator.form.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import eu.bbmri_eric.negotiator.form.FormElementType;
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

  private String placeholder;

  @NotNull private FormElementType type;
  private Long valueSetId;
}
