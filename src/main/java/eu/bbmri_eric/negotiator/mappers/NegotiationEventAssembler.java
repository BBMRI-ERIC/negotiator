package eu.bbmri_eric.negotiator.mappers;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import eu.bbmri_eric.negotiator.api.controller.v3.NegotiationLifecycleController;
import eu.bbmri_eric.negotiator.dto.negotiation.NegotiationEventMetadataDto;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
public class NegotiationEventAssembler
    implements RepresentationModelAssembler<
        NegotiationEventMetadataDto, EntityModel<NegotiationEventMetadataDto>> {

  @Override
  public @NonNull EntityModel<NegotiationEventMetadataDto> toModel(
      @NonNull NegotiationEventMetadataDto entity) {
    List<Link> links = new ArrayList<>();
    links.add(
        linkTo(methodOn(NegotiationLifecycleController.class).getAllEvents()).withRel("events"));
    links.add(
        linkTo(methodOn(NegotiationLifecycleController.class).getEvent(entity.getValue()))
            .withSelfRel());
    return EntityModel.of(entity, links);
  }

  @Override
  public @NonNull CollectionModel<EntityModel<NegotiationEventMetadataDto>> toCollectionModel(
      @NonNull Iterable<? extends NegotiationEventMetadataDto> entities) {
    return RepresentationModelAssembler.super.toCollectionModel(entities);
  }
}
