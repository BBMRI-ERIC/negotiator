package eu.bbmri_eric.negotiator.governance.organization;

import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceWithRepsDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;
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
public class OrganizationWithResourcesDTO extends OrganizationDTO {
  Set<ResourceWithRepsDTO> resources;
}
