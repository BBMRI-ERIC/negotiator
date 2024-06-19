package eu.bbmri_eric.negotiator.mappers;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import eu.bbmri_eric.negotiator.api.controller.v3.AccessFormController;
import eu.bbmri_eric.negotiator.dto.ValueSetDTO;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
public class ValueSetAssembler
    implements RepresentationModelAssembler<ValueSetDTO, EntityModel<ValueSetDTO>> {

  @Override
  public @NonNull EntityModel<ValueSetDTO> toModel(@NonNull ValueSetDTO entity) {
    List<Link> links = new ArrayList<>();
    links.add(linkTo(methodOn(AccessFormController.class).getAllValueSets()).withRel("value-sets"));
    links.add(
        linkTo(methodOn(AccessFormController.class).getValueSetById(entity.getId())).withSelfRel());
    return EntityModel.of(entity, links);
  }

  @Override
  public @NonNull CollectionModel<EntityModel<ValueSetDTO>> toCollectionModel(
      @NonNull Iterable<? extends ValueSetDTO> entities) {
    return RepresentationModelAssembler.super.toCollectionModel(entities);
  }
}
