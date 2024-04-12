package eu.bbmri_eric.negotiator.mappers;

import eu.bbmri_eric.negotiator.dto.resource.ResourceEventMetadataDto;
import lombok.NonNull;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
public class ResourceEventAssembler
    implements RepresentationModelAssembler<
        ResourceEventMetadataDto, EntityModel<ResourceEventMetadataDto>> {
  @Override
  public @NonNull EntityModel<ResourceEventMetadataDto> toModel(
      @NonNull ResourceEventMetadataDto entity) {
    return EntityModel.of(entity);
  }

  @Override
  public @NonNull CollectionModel<EntityModel<ResourceEventMetadataDto>> toCollectionModel(
      @NonNull Iterable<? extends ResourceEventMetadataDto> entities) {
    return RepresentationModelAssembler.super.toCollectionModel(entities);
  }
}
