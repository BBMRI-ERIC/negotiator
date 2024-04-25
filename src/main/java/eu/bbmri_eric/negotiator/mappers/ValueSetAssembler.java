package eu.bbmri_eric.negotiator.mappers;

import eu.bbmri_eric.negotiator.dto.ValueSetDto;
import lombok.NonNull;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
public class ValueSetAssembler
    implements RepresentationModelAssembler<ValueSetDto, EntityModel<ValueSetDto>> {

  @Override
  public @NonNull EntityModel<ValueSetDto> toModel(@NonNull ValueSetDto entity) {
    return EntityModel.of(entity);
  }

  @Override
  public @NonNull CollectionModel<EntityModel<ValueSetDto>> toCollectionModel(
      @NonNull Iterable<? extends ValueSetDto> entities) {
    return RepresentationModelAssembler.super.toCollectionModel(entities);
  }
}
