package eu.bbmri_eric.negotiator.governance.organization;

import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceState;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.hateoas.server.core.Relation;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Relation(collectionRelation = "organizations", itemRelation = "organization")
@Schema(description = "An organization")
public class OrganizationForNegotiationDTO extends OrganizationDTO {
  boolean updatable;

  NegotiationResourceState status;

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    OrganizationForNegotiationDTO that = (OrganizationForNegotiationDTO) o;
    return updatable == that.updatable && status == that.status;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), updatable, status);
  }
}
