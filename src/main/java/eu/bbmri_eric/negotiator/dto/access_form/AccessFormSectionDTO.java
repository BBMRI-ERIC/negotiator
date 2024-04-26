package eu.bbmri_eric.negotiator.dto.access_form;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@JsonInclude(value = JsonInclude.Include.NON_NULL)
@Relation(collectionRelation = "sections", itemRelation = "section")
public class AccessFormSectionDTO extends RepresentationModel<AccessFormSectionDTO> {
  @NotNull private Long id;

  @NotNull private String name;

  @NotNull private String label;

  @NotNull private String description;

  @NotNull private List<AccessFormElementDTO> elements;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AccessFormSectionDTO that = (AccessFormSectionDTO) o;
    return Objects.equals(name, that.name)
        && Objects.equals(label, that.label)
        && Objects.equals(description, that.description)
        && Objects.equals(elements, that.elements);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, label, description, elements);
  }
}
