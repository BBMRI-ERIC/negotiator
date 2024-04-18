package eu.bbmri_eric.negotiator.mappers;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import eu.bbmri_eric.negotiator.api.controller.v3.AccessFormController;
import eu.bbmri_eric.negotiator.dto.access_form.ElementMetaDTO;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
public class AccessFormElementAssembler
    implements RepresentationModelAssembler<ElementMetaDTO, EntityModel<ElementMetaDTO>> {

  @Override
  public @NotNull EntityModel<ElementMetaDTO> toModel(@NotNull ElementMetaDTO entity) {
    List<Link> links = new ArrayList<>();
    links.add(linkTo(methodOn(AccessFormController.class).getAllElements()).withRel("elements"));
    links.add(
        linkTo(methodOn(AccessFormController.class).getElementById(entity.getId())).withSelfRel());
    return EntityModel.of(entity).add(links);
  }

  @Override
  public @NotNull CollectionModel<EntityModel<ElementMetaDTO>> toCollectionModel(
      @NotNull Iterable<? extends ElementMetaDTO> entities) {
    return RepresentationModelAssembler.super.toCollectionModel(entities);
  }
}
