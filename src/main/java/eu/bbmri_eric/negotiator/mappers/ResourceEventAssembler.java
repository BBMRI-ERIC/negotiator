package eu.bbmri_eric.negotiator.mappers;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import eu.bbmri_eric.negotiator.api.controller.v3.ResourceLifecycleController;
import eu.bbmri_eric.negotiator.dto.resource.ResourceEventMetadataDto;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
public class ResourceEventAssembler
    implements RepresentationModelAssembler<
        ResourceEventMetadataDto, EntityModel<ResourceEventMetadataDto>> {
  @Override
  public @NonNull EntityModel<ResourceEventMetadataDto> toModel(
      @NonNull ResourceEventMetadataDto entity) {
    List<Link> links = new ArrayList<>();
    links.add(linkTo(methodOn(ResourceLifecycleController.class).getAllEvents()).withRel("events"));
    links.add(
        linkTo(methodOn(ResourceLifecycleController.class).getEvent(entity.getValue()))
            .withSelfRel());
    return EntityModel.of(entity, links);
  }

  @Override
  public @NonNull CollectionModel<EntityModel<ResourceEventMetadataDto>> toCollectionModel(
      @NonNull Iterable<? extends ResourceEventMetadataDto> entities) {
    return RepresentationModelAssembler.super.toCollectionModel(entities);
  }
}
