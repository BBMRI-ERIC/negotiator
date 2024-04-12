package eu.bbmri_eric.negotiator.mappers;

import eu.bbmri_eric.negotiator.dto.negotiation.NegotiationStateMetadataDto;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
public class LifecycleModelAssembler
    implements RepresentationModelAssembler<
        NegotiationStateMetadataDto, EntityModel<NegotiationStateMetadataDto>> {
  @Override
  public @NotNull EntityModel<NegotiationStateMetadataDto> toModel(
      @NonNull NegotiationStateMetadataDto entity) {
    return EntityModel.of(entity);
  }

  @Override
  public @NotNull CollectionModel<EntityModel<NegotiationStateMetadataDto>> toCollectionModel(
      @NonNull Iterable<? extends NegotiationStateMetadataDto> entities) {
    return RepresentationModelAssembler.super.toCollectionModel(entities);
  }
}
