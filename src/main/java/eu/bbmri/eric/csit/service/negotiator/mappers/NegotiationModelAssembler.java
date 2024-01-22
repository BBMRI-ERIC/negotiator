package eu.bbmri.eric.csit.service.negotiator.mappers;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import eu.bbmri.eric.csit.service.negotiator.api.controller.v3.NegotiationController;
import eu.bbmri.eric.csit.service.negotiator.api.controller.v3.NegotiationRole;
import eu.bbmri.eric.csit.service.negotiator.configuration.state_machine.negotiation.NegotiationState;
import eu.bbmri.eric.csit.service.negotiator.dto.negotiation.NegotiationDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;

public class NegotiationModelAssembler
    implements RepresentationModelAssembler<NegotiationDTO, EntityModel<NegotiationDTO>> {
  public NegotiationModelAssembler() {}

  @Override
  public @NonNull EntityModel<NegotiationDTO> toModel(@NonNull NegotiationDTO entity) {
    List<Link> links = new ArrayList<>();
    links.add(linkTo(NegotiationController.class).slash("negotiations").withRel("negotiations"));
    links.add(linkTo(methodOn(NegotiationController.class).retrieve(entity.getId())).withSelfRel());
    return EntityModel.of(entity, links);
  }

  public PagedModel<EntityModel<NegotiationDTO>> toPagedModel(
      @NonNull Page<NegotiationDTO> page, NegotiationRole role, Long userId) {
    List<Link> links = new ArrayList<>();
    if (page.hasContent()) {
      links = getLinks(page, role, userId);
    }
    return PagedModel.of(
        page.getContent().stream().map(this::toModel).collect(Collectors.toList()),
        new PagedModel.PageMetadata(
            page.getSize(), page.getNumber(), page.getTotalElements(), page.getTotalPages()),
        links);
  }

  public PagedModel<EntityModel<NegotiationDTO>> toPagedModel(
      @NonNull Page<NegotiationDTO> page, NegotiationState status) {
    List<Link> links = new ArrayList<>();
    if (page.hasContent()) {
      links = getLinks(page, status);
    }
    return PagedModel.of(
        page.getContent().stream().map(this::toModel).collect(Collectors.toList()),
        new PagedModel.PageMetadata(
            page.getSize(), page.getNumber(), page.getTotalElements(), page.getTotalPages()),
        links);
  }

  private List<Link> getLinks(Page<NegotiationDTO> page, NegotiationState status) {
    List<Link> links = new ArrayList<>();
    if (page.hasPrevious()) {
      links.add(
          linkTo(
                  methodOn(NegotiationController.class)
                      .list(status, page.getNumber() - 1, page.getSize()))
              .withRel(IanaLinkRelations.PREVIOUS)
              .expand());
    }
    if (page.hasNext()) {
      links.add(
          linkTo(
                  methodOn(NegotiationController.class)
                      .list(status, page.getNumber() + 1, page.getSize()))
              .withRel(IanaLinkRelations.NEXT)
              .expand());
    }
    links.add(
        linkTo(methodOn(NegotiationController.class).list(status, 0, page.getSize()))
            .withRel("first")
            .expand());
    links.add(
        linkTo(methodOn(NegotiationController.class).list(status, page.getNumber(), page.getSize()))
            .withRel(IanaLinkRelations.CURRENT)
            .expand());
    links.add(
        linkTo(
                methodOn(NegotiationController.class)
                    .list(status, page.getTotalPages() - 1, page.getSize()))
            .withRel(IanaLinkRelations.LAST)
            .expand());
    return links;
  }

  private List<Link> getLinks(
      Page<NegotiationDTO> page, NegotiationRole negotiationRole, Long userId) {
    List<Link> links = new ArrayList<>();
    if (page.hasPrevious()) {
      links.add(
          linkTo(
                  methodOn(NegotiationController.class)
                      .listRelated(userId, negotiationRole, page.getNumber() - 1, page.getSize()))
              .withRel(IanaLinkRelations.PREVIOUS)
              .expand());
    }
    if (page.hasNext()) {
      links.add(
          linkTo(
                  methodOn(NegotiationController.class)
                      .listRelated(userId, negotiationRole, page.getNumber() + 1, page.getSize()))
              .withRel(IanaLinkRelations.NEXT)
              .expand());
    }
    links.add(
        linkTo(
                methodOn(NegotiationController.class)
                    .listRelated(userId, negotiationRole, 0, page.getSize()))
            .withRel("first")
            .expand());
    links.add(
        linkTo(
                methodOn(NegotiationController.class)
                    .listRelated(userId, negotiationRole, page.getNumber(), page.getSize()))
            .withRel(IanaLinkRelations.CURRENT)
            .expand());
    links.add(
        linkTo(
                methodOn(NegotiationController.class)
                    .listRelated(userId, negotiationRole, page.getTotalPages() - 1, page.getSize()))
            .withRel(IanaLinkRelations.LAST)
            .expand());
    return links;
  }
}
