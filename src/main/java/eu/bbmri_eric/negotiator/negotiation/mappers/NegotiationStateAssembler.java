package eu.bbmri_eric.negotiator.negotiation.mappers;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import eu.bbmri_eric.negotiator.negotiation.NegotiationLifecycleController;
import eu.bbmri_eric.negotiator.negotiation.dto.NegotiationStateMetadataDto;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
public class NegotiationStateAssembler
    implements RepresentationModelAssembler<
        NegotiationStateMetadataDto, EntityModel<NegotiationStateMetadataDto>> {
  @Override
  public @NotNull EntityModel<NegotiationStateMetadataDto> toModel(
      @NonNull NegotiationStateMetadataDto entity) {
    List<Link> links = new ArrayList<>();
    links.add(
        linkTo(methodOn(NegotiationLifecycleController.class).getAllStates()).withRel("states"));
    links.add(
        linkTo(methodOn(NegotiationLifecycleController.class).getState(entity.getValue()))
            .withSelfRel());
    return EntityModel.of(entity, links);
  }

  @Override
  public @NotNull CollectionModel<EntityModel<NegotiationStateMetadataDto>> toCollectionModel(
      @NonNull Iterable<? extends NegotiationStateMetadataDto> entities) {
    return RepresentationModelAssembler.super.toCollectionModel(entities);
  }
}
