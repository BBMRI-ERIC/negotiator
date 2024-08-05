package eu.bbmri_eric.negotiator.mappers;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import eu.bbmri_eric.negotiator.api.controller.v3.InformationRequirementController;
import eu.bbmri_eric.negotiator.api.controller.v3.InformationSubmissionController;
import eu.bbmri_eric.negotiator.api.controller.v3.NegotiationController;
import eu.bbmri_eric.negotiator.api.controller.v3.ResourceController;
import eu.bbmri_eric.negotiator.configuration.state_machine.resource.NegotiationResourceEvent;
import eu.bbmri_eric.negotiator.configuration.state_machine.resource.NegotiationResourceState;
import eu.bbmri_eric.negotiator.dto.InformationRequirementDTO;
import eu.bbmri_eric.negotiator.dto.SubmittedInformationDTO;
import eu.bbmri_eric.negotiator.dto.resource.ResourceWithStatusDTO;
import eu.bbmri_eric.negotiator.service.InformationRequirementService;
import eu.bbmri_eric.negotiator.service.InformationSubmissionService;
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
  private final InformationSubmissionService informationSubmissionService;
  private Map<NegotiationResourceState, List<NegotiationResourceEvent>> cache = new HashMap<>();
  private Map<NegotiationResourceEvent, List<InformationRequirementDTO>> requirementsCache =
      new HashMap<>();
  private List<SubmittedInformationDTO> submittedInformationCache = new ArrayList<>();

  public ResourceWithStatusAssembler(
      ResourceLifecycleService resourceLifecycleService,
      InformationRequirementService informationRequirementService,
      InformationSubmissionService informationSubmissionService) {
    this.resourceLifecycleService = resourceLifecycleService;
    this.informationRequirementService = informationRequirementService;
    this.informationSubmissionService = informationSubmissionService;
  }

  @Override
  public @NonNull EntityModel<ResourceWithStatusDTO> toModel(
      @NonNull ResourceWithStatusDTO entity) {
    log.warn(entity.toString());
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
      if (submittedInformationCache.isEmpty()) {
        submittedInformationCache =
            informationSubmissionService.findAllForNegotiation(entity.getNegotiationId());
      }
      for (SubmittedInformationDTO submittedInformationDTO : submittedInformationCache) {
        if (submittedInformationDTO.getResourceId().equals(entity.getId())) {
          links.add(
              linkTo(
                      methodOn(InformationSubmissionController.class)
                          .getInfoSubmission(submittedInformationDTO.getId()))
                  .withRel("submission-%s".formatted(submittedInformationDTO.getId()))
                  .withTitle("Submitted Information"));
        }
      }
      Set<NegotiationResourceEvent> events;
      if (cache.containsKey(entity.getCurrentState())) {
        events =
            cache.get(entity.getCurrentState()).stream().collect(Collectors.toUnmodifiableSet());
      } else {
        events =
            resourceLifecycleService.getPossibleEvents(
                entity.getNegotiationId(), entity.getSourceId());
        cache.put(entity.getCurrentState(), new ArrayList<>(events));
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
          log.warn(requirements);
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
