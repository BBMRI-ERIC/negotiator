package eu.bbmri_eric.negotiator.mappers;

import eu.bbmri_eric.negotiator.dto.InformationRequirementDTO;
import lombok.NonNull;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
public class InformationRequirementAssembler
    implements RepresentationModelAssembler<
        InformationRequirementDTO, EntityModel<InformationRequirementDTO>> {
  public InformationRequirementAssembler() {}

  @Override
  public @NonNull EntityModel<InformationRequirementDTO> toModel(
      @NonNull InformationRequirementDTO entity) {
    return EntityModel.of(entity);
  }

  @Override
  public @NonNull CollectionModel<EntityModel<InformationRequirementDTO>> toCollectionModel(
      @NonNull Iterable<? extends InformationRequirementDTO> entities) {
    return RepresentationModelAssembler.super.toCollectionModel(entities);
  }
}
