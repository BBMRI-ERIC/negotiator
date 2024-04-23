package eu.bbmri_eric.negotiator.mappers;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import eu.bbmri_eric.negotiator.api.controller.v3.AccessFormController;
import eu.bbmri_eric.negotiator.dto.access_form.AccessFormDTO;
import eu.bbmri_eric.negotiator.dto.access_form.AccessFormElementDTO;
import eu.bbmri_eric.negotiator.dto.access_form.AccessFormSectionDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
public class AccessFormModelAssembler
    implements RepresentationModelAssembler<AccessFormDTO, EntityModel<AccessFormDTO>> {

  @Override
  public @NonNull EntityModel<AccessFormDTO> toModel(@NonNull AccessFormDTO entity) {
    List<Link> formLinks = new ArrayList<>();
    formLinks.add(linkTo(AccessFormController.class).slash("access-forms").withRel("access-forms"));
    formLinks.add(
        linkTo(methodOn(AccessFormController.class).getAccessFormById(entity.getId()))
            .withSelfRel());
    formLinks.add(
        linkTo(methodOn(AccessFormController.class).linkSection(entity.getId(), null))
            .withRel("add_sections"));
    for (AccessFormSectionDTO section : entity.getSections()) {
      List<Link> sectionLinks = new ArrayList<>();
      sectionLinks.add(
          linkTo(methodOn(AccessFormController.class).getAllSections()).withRel("sections"));
      sectionLinks.add(
          linkTo(
                  methodOn(AccessFormController.class)
                      .unlinkSection(entity.getId(), section.getId()))
              .withRel("remove"));
      sectionLinks.add(
          linkTo(
                  methodOn(AccessFormController.class)
                      .linkElement(entity.getId(), section.getId(), null))
              .withRel("add_elements"));
      sectionLinks.add(
          linkTo(methodOn(AccessFormController.class).getSectionById(entity.getId()))
              .withSelfRel());
      section.add(sectionLinks);
      for (AccessFormElementDTO elementDTO : section.getElements()) {
        List<Link> elementLinks = new ArrayList<>();
        elementLinks.add(
            linkTo(methodOn(AccessFormController.class).getAllElements()).withRel("elements"));
        elementLinks.add(
            linkTo(
                    methodOn(AccessFormController.class)
                        .unlinkElementFromSection(
                            entity.getId(), section.getId(), elementDTO.getId()))
                .withRel("remove"));
        elementLinks.add(
            linkTo(methodOn(AccessFormController.class).getElementById(entity.getId()))
                .withSelfRel());
        elementDTO.add(elementLinks);
      }
    }
    return EntityModel.of(entity, formLinks);
  }

  public PagedModel<EntityModel<AccessFormDTO>> toPagedModel(Page<AccessFormDTO> page) {
    List<Link> links = new ArrayList<>();
    links.add(linkTo(AccessFormController.class).slash("access-forms").withRel("access-forms"));
    return PagedModel.of(
        page.getContent().stream().map(this::toModel).collect(Collectors.toList()),
        new PagedModel.PageMetadata(
            page.getSize(), page.getNumber(), page.getTotalElements(), page.getTotalPages()),
        links);
  }

  @Override
  public @NonNull CollectionModel<EntityModel<AccessFormDTO>> toCollectionModel(
      @NonNull Iterable<? extends AccessFormDTO> entities) {
    return RepresentationModelAssembler.super.toCollectionModel(entities);
  }
}
