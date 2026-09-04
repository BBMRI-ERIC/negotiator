package eu.bbmri_eric.negotiator.governance.organization;

import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
public class OrganizationForNegotiationModelAssembler
    implements RepresentationModelAssembler<
        OrganizationForNegotiationDTO, EntityModel<OrganizationForNegotiationDTO>> {

  @Override
  public EntityModel<OrganizationForNegotiationDTO> toModel(
      OrganizationForNegotiationDTO organization) {

    return EntityModel.of(organization);
  }
}
