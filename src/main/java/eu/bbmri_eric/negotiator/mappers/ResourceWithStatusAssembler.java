package eu.bbmri_eric.negotiator.mappers;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import eu.bbmri_eric.negotiator.api.controller.v3.InformationRequirementController;
import eu.bbmri_eric.negotiator.api.controller.v3.NegotiationController;
import eu.bbmri_eric.negotiator.api.controller.v3.ResourceController;
import eu.bbmri_eric.negotiator.configuration.state_machine.resource.NegotiationResourceEvent;
import eu.bbmri_eric.negotiator.configuration.state_machine.resource.NegotiationResourceState;
import eu.bbmri_eric.negotiator.dto.InformationRequirementDTO;
import eu.bbmri_eric.negotiator.dto.resource.ResourceWithStatusDTO;
import eu.bbmri_eric.negotiator.service.InformationRequirementService;
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
  private final InformationRequirementService informationRequirementService;
  private final Map<NegotiationResourceState, List<NegotiationResourceEvent>> cache =
      new HashMap<>();
  private final Map<NegotiationResourceEvent, List<InformationRequirementDTO>> requirementsCache =
      new HashMap<>();

  public ResourceWithStatusAssembler(
      ResourceLifecycleService resourceLifecycleService,
      InformationRequirementService informationRequirementService) {
    this.resourceLifecycleService = resourceLifecycleService;
    this.informationRequirementService = informationRequirementService;
  }

  @Override
  public @NonNull EntityModel<ResourceWithStatusDTO> toModel(
      @NonNull ResourceWithStatusDTO entity) {
    List<Link> links = new ArrayList<>();
    links.add(
        WebMvcLinkBuilder.linkTo(methodOn(ResourceController.class).getResourceById(entity.getId()))
            .withSelfRel());
    links.add(linkTo(ResourceController.class).withRel("resources"));
    attachLifeCycleLinks(entity, links);
    return EntityModel.of(entity).add(links);
  }

  private void attachLifeCycleLinks(@NonNull ResourceWithStatusDTO entity, List<Link> links) {
    try {
      Set<NegotiationResourceEvent> events;
      if (cache.containsKey(entity.getStatus())) {
        events = cache.get(entity.getStatus()).stream().collect(Collectors.toUnmodifiableSet());
      } else {
        events =
            resourceLifecycleService.getPossibleEvents(
                entity.getNegotiationId(), entity.getSourceId());
        cache.put(entity.getStatus(), new ArrayList<>(events));
      }
      for (NegotiationResourceEvent event : events) {
        List<InformationRequirementDTO> requirements;
        if (requirementsCache.containsKey(event)) {
          requirements = requirementsCache.get(event);
        } else {
          requirements =
              informationRequirementService.getAllInformationRequirements().stream()
                  .filter(
                      informationRequirementDTO ->
                          informationRequirementDTO.getForResourceEvent().equals(event))
                  .toList();
          requirementsCache.put(event, requirements);
          // log.warn(requirements);
        }
        for (InformationRequirementDTO dto : requirements) {
          links.add(
              linkTo(
                      methodOn(InformationRequirementController.class)
                          .findRequirementById(dto.getId()))
                  .withRel("requirement-%s".formatted(dto.getId()))
                  .withTitle("Requirement to fulfill")
                  .withName(event.toString() + " requirement"));
        }
        links.add(
            linkTo(
                    methodOn(NegotiationController.class)
                        .sendEventForNegotiationResource(
                            entity.getNegotiationId(), entity.getSourceId(), event))
                .withRel(event.toString())
                .withTitle("Next Lifecycle event")
                .withName(event.getLabel()));
      }
    } catch (Exception e) {
      log.warn("Could not attach lifecycle links");
      log.warn(e.getMessage());
    }
  }

  @Override
  public @NonNull CollectionModel<EntityModel<ResourceWithStatusDTO>> toCollectionModel(
      @NonNull Iterable<? extends ResourceWithStatusDTO> entities) {
    return RepresentationModelAssembler.super
        .toCollectionModel(entities)
        .add(WebMvcLinkBuilder.linkTo(ResourceController.class).withRel("resources"));
  }
}
