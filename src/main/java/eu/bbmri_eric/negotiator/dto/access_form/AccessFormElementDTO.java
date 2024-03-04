package eu.bbmri_eric.negotiator.dto.access_form;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class AccessFormElementDTO {

  @NotNull private String name;

  @NotNull private String label;

  @NotNull private String description;

  @NotNull private String type;

  @NotNull private Boolean required;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AccessFormElementDTO that = (AccessFormElementDTO) o;
    return Objects.equals(name, that.name)
        && Objects.equals(label, that.label)
        && Objects.equals(description, that.description)
        && Objects.equals(type, that.type)
        && Objects.equals(required, that.required);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, label, description, type, required);
  }
}
