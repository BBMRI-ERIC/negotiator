package eu.bbmri_eric.negotiator.mappers;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import eu.bbmri_eric.negotiator.api.controller.v3.AttachmentController;
import eu.bbmri_eric.negotiator.api.controller.v3.InformationSubmissionController;
import eu.bbmri_eric.negotiator.api.controller.v3.NegotiationController;
import eu.bbmri_eric.negotiator.api.controller.v3.NetworkController;
import eu.bbmri_eric.negotiator.api.controller.v3.utils.NegotiationSortField;
import eu.bbmri_eric.negotiator.dto.InformationRequirementDTO;
import eu.bbmri_eric.negotiator.dto.negotiation.NegotiationDTO;
import eu.bbmri_eric.negotiator.dto.negotiation.NegotiationFilters;
import eu.bbmri_eric.negotiator.service.InformationRequirementService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.stereotype.Component;

@Component
public class NegotiationModelAssembler
    implements RepresentationModelAssembler<NegotiationDTO, EntityModel<NegotiationDTO>> {
  private final InformationRequirementService requirementService;

  public NegotiationModelAssembler(InformationRequirementService requirementService) {
    this.requirementService = requirementService;
  }

  @Override
  public @NonNull EntityModel<NegotiationDTO> toModel(@NonNull NegotiationDTO entity) {
    List<Link> links = new ArrayList<>();
    for (InformationRequirementDTO requirement :
        requirementService.getAllInformationRequirements()) {
      try {
        links.add(
            WebMvcLinkBuilder.linkTo(
                    methodOn(InformationSubmissionController.class)
                        .getSummaryInformation(entity.getId(), requirement.getId()))
                .withRel("Requirement summary")
                .withTitle(requirement.getRequiredAccessForm().getName() + " summary"));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
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
    return EntityModel.of(entity, links);
  }

  public PagedModel<EntityModel<NegotiationDTO>> toPagedModel(
      @NonNull Page<NegotiationDTO> page,
      NegotiationFilters filters,
      NegotiationSortField sortBy,
      Sort.Direction sortOrder,
      Long userId) {

    List<Link> links = new ArrayList<>();
    if (page.hasContent()) {
      links = getLinks(page, filters, sortBy, sortOrder, userId);
    }
    return PagedModel.of(
        page.getContent().stream().map(this::toModel).collect(Collectors.toList()),
        new PagedModel.PageMetadata(
            page.getSize(), page.getNumber(), page.getTotalElements(), page.getTotalPages()),
        links);
  }

  public PagedModel<EntityModel<NegotiationDTO>> toPagedModel(
      @NonNull Page<NegotiationDTO> page,
      NegotiationFilters filters,
      NegotiationSortField sortBy,
      Sort.Direction sortOrder) {
    List<Link> links = new ArrayList<>();
    if (page.hasContent()) {
      links = getLinks(page, filters, sortBy, sortOrder);
    }
    return PagedModel.of(
        page.getContent().stream().map(this::toModel).collect(Collectors.toList()),
        new PagedModel.PageMetadata(
            page.getSize(), page.getNumber(), page.getTotalElements(), page.getTotalPages()),
        links);
  }

  public PagedModel<EntityModel<NegotiationDTO>> toPagedModel(
      @NonNull Page<NegotiationDTO> page,
      NegotiationSortField sortBy,
      Sort.Direction sortOrder,
      Long networkId) {
    List<Link> links = new ArrayList<>();
    if (page.hasContent()) {
      links = getLinks(page, sortBy, sortOrder, networkId);
    }
    return PagedModel.of(
        page.getContent().stream().map(this::toModel).collect(Collectors.toList()),
        new PagedModel.PageMetadata(
            page.getSize(), page.getNumber(), page.getTotalElements(), page.getTotalPages()),
        links);
  }

  private List<Link> getLinks(
      Page<NegotiationDTO> page,
      NegotiationFilters filters,
      NegotiationSortField sortBy,
      Sort.Direction sortOrder) {
    List<Link> links = new ArrayList<>();
    if (page.hasPrevious()) {
      links.add(
          linkTo(
                  methodOn(NegotiationController.class)
                      .list(
                          null,
                          filters.getStatus(),
                          filters.getCreatedAfter(),
                          filters.getCreatedBefore(),
                          sortBy,
                          sortOrder,
                          page.getNumber() - 1,
                          page.getSize()))
              .withRel(IanaLinkRelations.PREVIOUS)
              .expand());
    }
    if (page.hasNext()) {
      links.add(
          linkTo(
                  methodOn(NegotiationController.class)
                      .list(
                          null,
                          filters.getStatus(),
                          filters.getCreatedAfter(),
                          filters.getCreatedBefore(),
                          sortBy,
                          sortOrder,
                          page.getNumber() + 1,
                          page.getSize()))
              .withRel(IanaLinkRelations.NEXT)
              .expand());
    }
    links.add(
        linkTo(
                methodOn(NegotiationController.class)
                    .list(
                        null,
                        filters.getStatus(),
                        filters.getCreatedAfter(),
                        filters.getCreatedBefore(),
                        sortBy,
                        sortOrder,
                        0,
                        page.getSize()))
            .withRel(IanaLinkRelations.FIRST)
            .expand());
    links.add(
        linkTo(
                methodOn(NegotiationController.class)
                    .list(
                        null,
                        filters.getStatus(),
                        filters.getCreatedAfter(),
                        filters.getCreatedBefore(),
                        sortBy,
                        sortOrder,
                        page.getNumber(),
                        page.getSize()))
            .withRel(IanaLinkRelations.CURRENT)
            .expand());
    links.add(
        linkTo(
                methodOn(NegotiationController.class)
                    .list(
                        null,
                        filters.getStatus(),
                        filters.getCreatedAfter(),
                        filters.getCreatedBefore(),
                        sortBy,
                        sortOrder,
                        page.getTotalPages() - 1,
                        page.getSize()))
            .withRel(IanaLinkRelations.LAST)
            .expand());
    return links;
  }

  private List<Link> getLinks(
      Page<NegotiationDTO> page,
      NegotiationFilters filters,
      NegotiationSortField sortBy,
      Sort.Direction sortOrder,
      Long userId) {
    List<Link> links = new ArrayList<>();
    if (page.hasPrevious()) {
      links.add(
          linkTo(
                  methodOn(NegotiationController.class)
                      .listRelated(
                          null,
                          userId,
                          filters.getRole(),
                          filters.getStatus(),
                          filters.getCreatedAfter(),
                          filters.getCreatedBefore(),
                          sortBy,
                          sortOrder,
                          page.getNumber() - 1,
                          page.getSize()))
              .withRel(IanaLinkRelations.PREVIOUS)
              .expand());
    }
    if (page.hasNext()) {
      links.add(
          linkTo(
                  methodOn(NegotiationController.class)
                      .listRelated(
                          null,
                          userId,
                          filters.getRole(),
                          filters.getStatus(),
                          filters.getCreatedAfter(),
                          filters.getCreatedBefore(),
                          sortBy,
                          sortOrder,
                          page.getNumber() + 1,
                          page.getSize()))
              .withRel(IanaLinkRelations.NEXT)
              .expand());
    }
    links.add(
        linkTo(
                methodOn(NegotiationController.class)
                    .listRelated(
                        null,
                        userId,
                        filters.getRole(),
                        filters.getStatus(),
                        filters.getCreatedAfter(),
                        filters.getCreatedBefore(),
                        sortBy,
                        sortOrder,
                        0,
                        page.getSize()))
            .withRel(IanaLinkRelations.FIRST)
            .expand());
    links.add(
        linkTo(
                methodOn(NegotiationController.class)
                    .listRelated(
                        null,
                        userId,
                        filters.getRole(),
                        filters.getStatus(),
                        filters.getCreatedAfter(),
                        filters.getCreatedBefore(),
                        sortBy,
                        sortOrder,
                        page.getNumber(),
                        page.getSize()))
            .withRel(IanaLinkRelations.CURRENT)
            .expand());
    links.add(
        linkTo(
                methodOn(NegotiationController.class)
                    .listRelated(
                        null,
                        userId,
                        filters.getRole(),
                        filters.getStatus(),
                        filters.getCreatedAfter(),
                        filters.getCreatedBefore(),
                        sortBy,
                        sortOrder,
                        page.getTotalPages() - 1,
                        page.getSize()))
            .withRel(IanaLinkRelations.LAST)
            .expand());
    return links;
  }

  private List<Link> getLinks(
      Page<NegotiationDTO> page,
      NegotiationSortField sortBy,
      Sort.Direction sortOrder,
      Long networkId) {
    List<Link> links = new ArrayList<>();
    if (page.hasPrevious()) {
      links.add(
          linkTo(
                  methodOn(NetworkController.class)
                      .getNegotiations(
                          networkId, page.getNumber() - 1, page.getSize(), sortBy, sortOrder))
              .withRel(IanaLinkRelations.PREVIOUS)
              .expand());
    }
    if (page.hasNext()) {
      links.add(
          linkTo(
                  methodOn(NetworkController.class)
                      .getNegotiations(
                          networkId, page.getNumber() + 1, page.getSize(), sortBy, sortOrder))
              .withRel(IanaLinkRelations.NEXT)
              .expand());
    }
    links.add(
        linkTo(
                methodOn(NetworkController.class)
                    .getNegotiations(networkId, 0, page.getSize(), sortBy, sortOrder))
            .withRel(IanaLinkRelations.FIRST)
            .expand());
    links.add(
        linkTo(
                methodOn(NetworkController.class)
                    .getNegotiations(
                        networkId, page.getNumber(), page.getSize(), sortBy, sortOrder))
            .withRel(IanaLinkRelations.CURRENT)
            .expand());
    links.add(
        linkTo(
                methodOn(NetworkController.class)
                    .getNegotiations(
                        networkId, page.getTotalPages() - 1, page.getSize(), sortBy, sortOrder))
            .withRel(IanaLinkRelations.LAST)
            .expand());
    return links;
  }
}
