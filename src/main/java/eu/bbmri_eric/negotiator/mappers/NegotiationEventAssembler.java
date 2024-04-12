package eu.bbmri_eric.negotiator.mappers;

import eu.bbmri_eric.negotiator.dto.negotiation.NegotiationEventMetadataDto;
import lombok.NonNull;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
public class NegotiationEventAssembler
    implements RepresentationModelAssembler<
        NegotiationEventMetadataDto, EntityModel<NegotiationEventMetadataDto>> {

  @Override
  public @NonNull EntityModel<NegotiationEventMetadataDto> toModel(
      @NonNull NegotiationEventMetadataDto entity) {
    return EntityModel.of(entity);
  }

  @Override
  public @NonNull CollectionModel<EntityModel<NegotiationEventMetadataDto>> toCollectionModel(
      @NonNull Iterable<? extends NegotiationEventMetadataDto> entities) {
    return RepresentationModelAssembler.super.toCollectionModel(entities);
  }
}
