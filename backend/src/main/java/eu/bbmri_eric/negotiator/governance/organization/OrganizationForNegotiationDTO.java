package eu.bbmri_eric.negotiator.governance.organization;

import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceState;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.springframework.hateoas.server.core.Relation;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Relation(collectionRelation = "organizations", itemRelation = "organization")
@Schema(description = "An organization")
public class OrganizationForNegotiationDTO extends OrganizationDTO {
  boolean updatable;
  NegotiationResourceState status;
}
