package eu.bbmri_eric.negotiator.mappers;

import eu.bbmri_eric.negotiator.dto.resource.ResourceStateMetadataDto;
import lombok.NonNull;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
public class ResourceStateAssembler
    implements RepresentationModelAssembler<
        ResourceStateMetadataDto, EntityModel<ResourceStateMetadataDto>> {
  @Override
  public @NonNull EntityModel<ResourceStateMetadataDto> toModel(
      @NonNull ResourceStateMetadataDto entity) {
    return EntityModel.of(entity);
  }

  @Override
  public @NonNull CollectionModel<EntityModel<ResourceStateMetadataDto>> toCollectionModel(
      @NonNull Iterable<? extends ResourceStateMetadataDto> entities) {
    return RepresentationModelAssembler.super.toCollectionModel(entities);
  }
}
