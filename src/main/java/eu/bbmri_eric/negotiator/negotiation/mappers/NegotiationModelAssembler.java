package eu.bbmri_eric.negotiator.negotiation.mappers;

import static eu.bbmri_eric.negotiator.common.LinkBuilder.getPageLinks;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import eu.bbmri_eric.negotiator.attachment.AttachmentController;
import eu.bbmri_eric.negotiator.info_requirement.InformationRequirementDTO;
import eu.bbmri_eric.negotiator.info_requirement.InformationRequirementService;
import eu.bbmri_eric.negotiator.info_submission.InformationSubmissionController;
import eu.bbmri_eric.negotiator.negotiation.NegotiationController;
import eu.bbmri_eric.negotiator.negotiation.dto.NegotiationDTO;
import eu.bbmri_eric.negotiator.negotiation.dto.NegotiationFilterDTO;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationLifecycleService;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.stereotype.Component;

@Component
public class NegotiationModelAssembler
    implements RepresentationModelAssembler<NegotiationDTO, EntityModel<NegotiationDTO>> {
  private final InformationRequirementService requirementService;
  private final NegotiationLifecycleService negotiationLifecycleService;

  public NegotiationModelAssembler(
      InformationRequirementService requirementService,
      NegotiationLifecycleService negotiationLifecycleService) {
    this.requirementService = requirementService;
    this.negotiationLifecycleService = negotiationLifecycleService;
  }

  public @NonNull EntityModel<NegotiationDTO> toModelWithRequirementLink(
      @NonNull NegotiationDTO entity, boolean isAdmin) {
    EntityModel<NegotiationDTO> entityModel = toModel(entity);
    for (InformationRequirementDTO requirement :
        requirementService.getAllInformationRequirements()) {
      if (requirement.isViewableOnlyByAdmin() && !isAdmin) {
        continue;
      }
      entityModel.add(
          WebMvcLinkBuilder.linkTo(
                  methodOn(InformationSubmissionController.class)
                      .getSummaryInformation(entity.getId(), requirement.getId()))
              .withRel("Requirement summary %s".formatted(requirement.getId()))
              .withTitle(requirement.getRequiredAccessForm().getName() + " summary"));
    }
    for (NegotiationEvent event : negotiationLifecycleService.getPossibleEvents(entity.getId())) {
      entityModel.add(
          WebMvcLinkBuilder.linkTo(
                  methodOn(NegotiationController.class).sendEvent(entity.getId(), event, null))
              .withRel(event.toString())
              .withTitle("Next Lifecycle event")
              .withName(event.getLabel()));
    }
    if (entity.isPayloadUpdatable()) {
      entityModel.add(
          WebMvcLinkBuilder.linkTo(
                  methodOn(NegotiationController.class).update(entity.getId(), null))
              .withRel("Update"));
    }
    return entityModel;
  }

  @Override
  public @NonNull EntityModel<NegotiationDTO> toModel(@NonNull NegotiationDTO entity) {
    List<Link> links = new ArrayList<>();
    links.add(
        WebMvcLinkBuilder.linkTo(methodOn(NegotiationController.class).retrieve(entity.getId()))
            .withSelfRel());
    links.add(
        WebMvcLinkBuilder.linkTo(NegotiationController.class)
            .slash("negotiations/%s/posts".formatted(entity.getId()))
            .withRel("posts"));
    links.add(
        WebMvcLinkBuilder.linkTo(
                methodOn(AttachmentController.class).listByNegotiation(entity.getId()))
            .withRel("attachments"));
    links.add(
        linkTo(methodOn(NegotiationController.class).findResourcesForNegotiation(entity.getId()))
            .withRel("resources"));
    return EntityModel.of(entity, links);
  }

  public PagedModel<EntityModel<NegotiationDTO>> toPagedModel(
      @NonNull Page<NegotiationDTO> page, NegotiationFilterDTO filters, Long userId) {

    List<Link> links = new ArrayList<>();
    if (page.hasContent()) {
      PagedModel.PageMetadata pageMetadata =
          new PagedModel.PageMetadata(
              page.getSize(), page.getNumber(), page.getTotalElements(), page.getTotalPages());

      if (userId == null) {
        links =
            getPageLinks(
                linkTo(methodOn(NegotiationController.class).list(filters)).toUri(),
                filters,
                pageMetadata);
      } else {
        links =
            getPageLinks(
                linkTo(methodOn(NegotiationController.class).listRelated(userId, filters)).toUri(),
                filters,
                pageMetadata);
      }
    }
    return PagedModel.of(
        page.getContent().stream().map(this::toModel).collect(Collectors.toList()),
        new PagedModel.PageMetadata(
            page.getSize(), page.getNumber(), page.getTotalElements(), page.getTotalPages()),
        links);
  }
}
