package eu.bbmri.eric.csit.service.negotiator.mappers;

import eu.bbmri.eric.csit.service.negotiator.dto.negotiation.NegotiationDTO;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;

public class NegotiationModelAssembler
    implements RepresentationModelAssembler<NegotiationDTO, EntityModel<NegotiationDTO>> {
  @Override
  public EntityModel<NegotiationDTO> toModel(NegotiationDTO entity) {
    return EntityModel.of(entity);
  }
}
