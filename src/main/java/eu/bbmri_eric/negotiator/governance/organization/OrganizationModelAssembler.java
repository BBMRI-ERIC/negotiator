package eu.bbmri_eric.negotiator.governance.organization;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import eu.bbmri_eric.negotiator.governance.resource.ResourceController;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.stereotype.Component;

@Component
public class OrganizationModelAssembler
    implements RepresentationModelAssembler<OrganizationDTO, EntityModel<OrganizationDTO>> {

  @Override
  public @NonNull EntityModel<OrganizationDTO> toModel(@NonNull OrganizationDTO entity) {
    return EntityModel.of(entity)
        .add(
            WebMvcLinkBuilder.linkTo(
                    methodOn(OrganizationController.class).findById(entity.getId()))
                .withSelfRel(),
            linkTo(OrganizationController.class).withRel("organizations"));
  }

  @Override
  public CollectionModel<EntityModel<OrganizationDTO>> toCollectionModel(
      Iterable<? extends OrganizationDTO> entities) {
    List<EntityModel<OrganizationDTO>> entityModels = new ArrayList<>();
    for (OrganizationDTO organizationDTO : entities) {
      entityModels.add(toModel(organizationDTO));
    }
    return CollectionModel.of(entityModels)
        .add(linkTo(ResourceController.class).withRel("organizations"));
  }

  public PagedModel<EntityModel<OrganizationDTO>> toPagedModel(Page<OrganizationDTO> page) {
    return PagedModel.of(
        page.getContent().stream().map(this::toModel).collect(Collectors.toList()),
        new PagedModel.PageMetadata(
            page.getSize(), page.getNumber(), page.getTotalElements(), page.getTotalPages()),
        new ArrayList<>());
  }
}
