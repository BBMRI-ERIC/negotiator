package eu.bbmri_eric.negotiator.mappers;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import eu.bbmri_eric.negotiator.api.controller.v3.NegotiationController;
import eu.bbmri_eric.negotiator.api.controller.v3.ResourceController;
import eu.bbmri_eric.negotiator.configuration.state_machine.resource.NegotiationResourceEvent;
import eu.bbmri_eric.negotiator.configuration.state_machine.resource.NegotiationResourceState;
import eu.bbmri_eric.negotiator.dto.resource.ResourceWithStatusDTO;
import eu.bbmri_eric.negotiator.service.ResourceLifecycleService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.stereotype.Component;

@Component
@CommonsLog
public class ResourceWithStatusAssembler
    implements RepresentationModelAssembler<
        ResourceWithStatusDTO, EntityModel<ResourceWithStatusDTO>> {
  private final ResourceLifecycleService resourceLifecycleService;
  private final Map<NegotiationResourceState, List<NegotiationResourceEvent>> cache =
      new HashMap<>();

  public ResourceWithStatusAssembler(ResourceLifecycleService resourceLifecycleService) {
    this.resourceLifecycleService = resourceLifecycleService;
  }

  @Override
  public @NonNull EntityModel<ResourceWithStatusDTO> toModel(
      @NonNull ResourceWithStatusDTO entity) {
    List<Link> links = new ArrayList<>();
    links.add(
        WebMvcLinkBuilder.linkTo(methodOn(ResourceController.class).getResourceById(entity.getId()))
            .withSelfRel());
    links.add(linkTo(ResourceController.class).withRel("resources"));
    try {
      Set<NegotiationResourceEvent> events;
      if (cache.containsKey(entity.getStatus())) {
        events = cache.get(entity.getStatus()).stream().collect(Collectors.toUnmodifiableSet());
      } else {
        events =
            resourceLifecycleService.getPossibleEvents(
                entity.getNegotiationId(), entity.getExternalId());
        cache.put(entity.getStatus(), new ArrayList<>(events));
      }
      for (NegotiationResourceEvent event : events) {
        links.add(
            linkTo(
                    methodOn(NegotiationController.class)
                        .sendEventForNegotiationResource(
                            entity.getNegotiationId(), entity.getExternalId(), event))
                .withRel(event.toString())
                .withTitle("Next Lifecycle event")
                .withName(event.getLabel()));
      }
    } catch (Exception e) {
      log.warn("Could not attach lifecycle links");
    }
    return EntityModel.of(entity).add(links);
  }

  @Override
  public @NonNull CollectionModel<EntityModel<ResourceWithStatusDTO>> toCollectionModel(
      @NonNull Iterable<? extends ResourceWithStatusDTO> entities) {
    return RepresentationModelAssembler.super
        .toCollectionModel(entities)
        .add(WebMvcLinkBuilder.linkTo(ResourceController.class).withRel("resources"));
  }
}
