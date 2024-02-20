package eu.bbmri_eric.negotiator.dto.access_criteria;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import java.util.List;
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
public class AccessCriteriaSectionDTO {

  @NotNull private String name;

  @NotNull private String label;

  @NotNull private String description;

  @NotNull private List<AccessCriteriaDTO> accessCriteria;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AccessCriteriaSectionDTO that = (AccessCriteriaSectionDTO) o;
    return Objects.equals(name, that.name)
        && Objects.equals(label, that.label)
        && Objects.equals(description, that.description)
        && Objects.equals(accessCriteria, that.accessCriteria);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, label, description, accessCriteria);
  }
}
