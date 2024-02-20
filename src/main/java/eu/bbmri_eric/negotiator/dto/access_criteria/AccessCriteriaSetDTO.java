package eu.bbmri_eric.negotiator.dto.access_criteria;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class AccessCriteriaSetDTO {

  String name;

  List<AccessCriteriaSectionDTO> sections = new ArrayList<>();

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AccessCriteriaSetDTO that = (AccessCriteriaSetDTO) o;
    return Objects.equals(sections, that.sections);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sections);
  }
}
