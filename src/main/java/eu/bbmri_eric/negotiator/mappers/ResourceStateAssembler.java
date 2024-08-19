package eu.bbmri_eric.negotiator.mappers;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import eu.bbmri_eric.negotiator.api.controller.v3.ResourceLifecycleController;
import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceStateMetadataDto;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
public class ResourceStateAssembler
    implements RepresentationModelAssembler<
        ResourceStateMetadataDto, EntityModel<ResourceStateMetadataDto>> {
  @Override
  public @NonNull EntityModel<ResourceStateMetadataDto> toModel(
      @NonNull ResourceStateMetadataDto entity) {
    List<Link> links = new ArrayList<>();
    links.add(linkTo(methodOn(ResourceLifecycleController.class).getAllStates()).withRel("states"));
    links.add(
        linkTo(methodOn(ResourceLifecycleController.class).getState(entity.getValue()))
            .withSelfRel());
    return EntityModel.of(entity, links);
  }

  @Override
  public @NonNull CollectionModel<EntityModel<ResourceStateMetadataDto>> toCollectionModel(
      @NonNull Iterable<? extends ResourceStateMetadataDto> entities) {
    return RepresentationModelAssembler.super.toCollectionModel(entities);
  }
}
