package eu.bbmri_eric.negotiator.mappers;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import eu.bbmri_eric.negotiator.api.controller.v3.AccessFormController;
import eu.bbmri_eric.negotiator.dto.access_form.SectionMetaDTO;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
public class AccessFormSectionAssembler
    implements RepresentationModelAssembler<SectionMetaDTO, EntityModel<SectionMetaDTO>> {
  @Override
  public @NotNull EntityModel<SectionMetaDTO> toModel(SectionMetaDTO entity) {
    List<Link> links = new ArrayList<>();
    links.add(linkTo(methodOn(AccessFormController.class).getAllSections()).withRel("sections"));
    links.add(
        linkTo(methodOn(AccessFormController.class).getSectionById(entity.getId())).withSelfRel());
    return EntityModel.of(entity).add(links);
  }

  @Override
  public @NotNull CollectionModel<EntityModel<SectionMetaDTO>> toCollectionModel(
      @NotNull Iterable<? extends SectionMetaDTO> entities) {
    return RepresentationModelAssembler.super.toCollectionModel(entities);
  }
}
