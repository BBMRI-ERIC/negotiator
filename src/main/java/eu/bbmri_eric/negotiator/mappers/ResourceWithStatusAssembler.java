package eu.bbmri_eric.negotiator.mappers;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import eu.bbmri_eric.negotiator.api.controller.v3.InformationRequirementController;
import eu.bbmri_eric.negotiator.api.controller.v3.InformationSubmissionController;
import eu.bbmri_eric.negotiator.api.controller.v3.NegotiationController;
import eu.bbmri_eric.negotiator.api.controller.v3.ResourceController;
import eu.bbmri_eric.negotiator.configuration.state_machine.resource.NegotiationResourceEvent;
import eu.bbmri_eric.negotiator.dto.InformationRequirementDTO;
import eu.bbmri_eric.negotiator.dto.SubmittedInformationDTO;
import eu.bbmri_eric.negotiator.dto.resource.ResourceWithStatusDTO;
import eu.bbmri_eric.negotiator.service.InformationRequirementService;
import eu.bbmri_eric.negotiator.service.InformationSubmissionService;
import eu.bbmri_eric.negotiator.service.ResourceLifecycleService;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
  private static List<InformationRequirementDTO> requirementsCache = new ArrayList<>();
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
    List<Link> links = addWebLinks(entity);
    return EntityModel.of(entity).add(links);
  }

  private @NonNull List<Link> addWebLinks(@NonNull ResourceWithStatusDTO entity) {
    List<Link> links = new ArrayList<>();
    links.add(
        WebMvcLinkBuilder.linkTo(methodOn(ResourceController.class).getResourceById(entity.getId()))
            .withSelfRel());
    links.add(linkTo(ResourceController.class).withRel("resources"));
    addLifecycleLink(entity, links);
    addSubmissionLinks(entity, links);
    addRequirementLinks(links, entity.getId());
    return links;
  }

  @Override
  public @NonNull CollectionModel<EntityModel<ResourceWithStatusDTO>> toCollectionModel(
      @NonNull Iterable<? extends ResourceWithStatusDTO> entities) {
    return RepresentationModelAssembler.super
        .toCollectionModel(entities)
        .add(WebMvcLinkBuilder.linkTo(ResourceController.class).withRel("resources"));
  }

  private void addSubmissionLinks(@NonNull ResourceWithStatusDTO entity, List<Link> links) {
    try {
      if (submittedInformationCache.isEmpty()) {
        submittedInformationCache =
            informationSubmissionService.findAllForNegotiation(entity.getNegotiationId());
      }
      for (SubmittedInformationDTO info : submittedInformationCache) {
        addSubmissionLink(entity, links, info);
      }
    } catch (Exception e) {
      log.error("Could not attach submission links: " + e.getMessage());
    }
  }

  private static void addSubmissionLink(
      @NonNull ResourceWithStatusDTO entity, List<Link> links, SubmittedInformationDTO info) {
    if (info.getResourceId().equals(entity.getId())) {
      links.add(
          linkTo(methodOn(InformationSubmissionController.class).getInfoSubmission(info.getId()))
              .withRel("submission-%s".formatted(info.getId()))
              .withTitle("Submitted Information")
              .withName("Submitted Information"));
    }
  }

  private void addRequirementLinks(List<Link> links, Long resourceId) {
    try {
      if (requirementsCache.isEmpty()) {
        requirementsCache = informationRequirementService.getAllInformationRequirements();
      }
      for (InformationRequirementDTO dto : requirementsCache) {
        addRequirementLink(links, resourceId, dto);
      }
    } catch (Exception e) {
      log.error("Could not attach requirement links: " + e.getMessage());
    }
  }

  private void addRequirementLink(
      List<Link> links, Long resourceId, InformationRequirementDTO dto) {
    if (submittedInformationCache.stream()
        .noneMatch(
            i ->
                i.getResourceId().equals(resourceId) && i.getRequirementId().equals(dto.getId()))) {
      links.add(
          linkTo(methodOn(InformationRequirementController.class).findRequirementById(dto.getId()))
              .withRel("requirement-%s".formatted(dto.getId()))
              .withTitle("Requirement to fulfill")
              .withName(dto.getForResourceEvent().toString() + " requirement"));
    }
  }

  private void addLifecycleLink(@NonNull ResourceWithStatusDTO entity, List<Link> links) {
    try {
      Set<NegotiationResourceEvent> events =
          resourceLifecycleService.getPossibleEvents(
              entity.getNegotiationId(), entity.getSourceId());
      for (NegotiationResourceEvent event : events) {
        addLifecycleEventLink(entity, event, links);
      }
    } catch (Exception e) {
      log.error("Could not attach lifecycle links: " + e.getMessage());
    }
  }

  private static void addLifecycleEventLink(
      @NonNull ResourceWithStatusDTO entity, NegotiationResourceEvent event, List<Link> links) {
    links.add(
        linkTo(
                methodOn(NegotiationController.class)
                    .sendEventForNegotiationResource(
                        entity.getNegotiationId(), entity.getSourceId(), event))
            .withRel(event.toString())
            .withTitle("Next Lifecycle event")
            .withName(event.getLabel()));
  }
}
