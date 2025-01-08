package eu.bbmri_eric.negotiator.form.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.hateoas.server.core.Relation;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Relation(collectionRelation = "access-forms", itemRelation = "access-form")
public class AccessFormDTO {
  private Long id;
  private String name;

  List<AccessFormSectionDTO> sections = new ArrayList<>();

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AccessFormDTO that = (AccessFormDTO) o;
    return Objects.equals(sections, that.sections);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sections);
  }
}
