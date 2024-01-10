package eu.bbmri.eric.csit.service.negotiator.mappers;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import eu.bbmri.eric.csit.service.negotiator.api.controller.v3.NegotiationController;
import eu.bbmri.eric.csit.service.negotiator.dto.negotiation.NegotiationDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;

public class NegotiationModelAssembler
    implements RepresentationModelAssembler<NegotiationDTO, EntityModel<NegotiationDTO>> {
  @Override
  public @NonNull EntityModel<NegotiationDTO> toModel(@NonNull NegotiationDTO entity) {
    List<Link> links = new ArrayList<>();
    links.add(linkTo(NegotiationController.class).slash("negotiations").withRel("negotiations"));
    return EntityModel.of(entity, links);
  }

  public PagedModel<EntityModel<NegotiationDTO>> toPagedModel(@NonNull Page<NegotiationDTO> page) {
    List<Link> links = getLinks(page);
    return PagedModel.of(
        page.getContent().stream().map(this::toModel).collect(Collectors.toList()),
        new PagedModel.PageMetadata(
            page.getSize(), page.getNumber(), page.getTotalElements(), page.getTotalPages()),
        links);
  }

  private List<Link> getLinks(Page<NegotiationDTO> page) {
    List<Link> links = new ArrayList<>();
    return links;
  }
}
