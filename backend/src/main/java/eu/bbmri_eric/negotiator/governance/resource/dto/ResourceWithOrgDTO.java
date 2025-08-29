package eu.bbmri_eric.negotiator.governance.resource.dto;

import eu.bbmri_eric.negotiator.governance.organization.OrganizationDTO;
import lombok.Getter;
import lombok.Setter;
import org.springframework.hateoas.server.core.Relation;

@Getter
@Setter
@Relation(collectionRelation = "resources", itemRelation = "resource")
public class ResourceWithOrgDTO extends ResourceResponseModel {
  private OrganizationDTO organization;
}
