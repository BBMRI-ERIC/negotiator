package eu.bbmri_eric.negotiator.mappers;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import eu.bbmri_eric.negotiator.api.controller.v3.AccessFormController;
import eu.bbmri_eric.negotiator.dto.access_form.AccessFormDTO;
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
    List<Link> links = new ArrayList<>();
    return EntityModel.of(entity, links);
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
