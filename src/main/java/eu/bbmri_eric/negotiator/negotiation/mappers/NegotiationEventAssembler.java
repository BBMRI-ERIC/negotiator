package eu.bbmri_eric.negotiator.negotiation.mappers;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import eu.bbmri_eric.negotiator.negotiation.dto.NegotiationEventMetadataDTO;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationLifecycleController;
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
        NegotiationEventMetadataDTO, EntityModel<NegotiationEventMetadataDTO>> {

  @Override
  public @NonNull EntityModel<NegotiationEventMetadataDTO> toModel(
      @NonNull NegotiationEventMetadataDTO entity) {
    List<Link> links = new ArrayList<>();
    links.add(
        linkTo(methodOn(NegotiationLifecycleController.class).getAllEvents()).withRel("events"));
    links.add(
        linkTo(
                methodOn(NegotiationLifecycleController.class)
                    .getEvent(NegotiationEvent.valueOf(entity.getValue())))
            .withSelfRel());
    return EntityModel.of(entity, links);
  }

  @Override
  public @NonNull CollectionModel<EntityModel<NegotiationEventMetadataDTO>> toCollectionModel(
      @NonNull Iterable<? extends NegotiationEventMetadataDTO> entities) {
    return RepresentationModelAssembler.super.toCollectionModel(entities);
  }
}
