package eu.bbmri_eric.negotiator.dto;

import eu.bbmri_eric.negotiator.configuration.state_machine.resource.NegotiationResourceEvent;
import eu.bbmri_eric.negotiator.dto.access_form.AccessFormDTO;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.hateoas.server.core.Relation;

/** DTO for {@link eu.bbmri_eric.negotiator.database.model.InformationRequirement} */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Relation(collectionRelation = "info-requirements", itemRelation = "info-requirement")
public class InformationRequirementDTO {
  private Long id;
  private AccessFormDTO requiredAccessForm;
  private NegotiationResourceEvent forResourceEvent;

  @Override
  public final boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof InformationRequirementDTO that)) return false;

    return Objects.equals(id, that.id)
        && Objects.equals(requiredAccessForm, that.requiredAccessForm)
        && forResourceEvent == that.forResourceEvent;
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(id);
    result = 31 * result + Objects.hashCode(requiredAccessForm);
    result = 31 * result + Objects.hashCode(forResourceEvent);
    return result;
  }
}
