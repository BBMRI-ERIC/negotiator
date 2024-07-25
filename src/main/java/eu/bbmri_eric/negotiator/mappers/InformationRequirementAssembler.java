package eu.bbmri_eric.negotiator.mappers;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import eu.bbmri_eric.negotiator.api.controller.v3.InformationRequirementController;
import eu.bbmri_eric.negotiator.dto.InformationRequirementDTO;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
public class InformationRequirementAssembler
    implements RepresentationModelAssembler<
        InformationRequirementDTO, EntityModel<InformationRequirementDTO>> {
  public InformationRequirementAssembler() {}

  @Override
  public @NonNull EntityModel<InformationRequirementDTO> toModel(
      @NonNull InformationRequirementDTO entity) {
    List<Link> links = new ArrayList<>();
    links.add(
        linkTo(InformationRequirementController.class)
            .slash("info-requirements")
            .withRel("info-requirements"));
    links.add(
        linkTo(methodOn(InformationRequirementController.class).findRequirementById(entity.getId()))
            .withSelfRel());
    return EntityModel.of(entity, links);
  }

  @Override
  public @NonNull CollectionModel<EntityModel<InformationRequirementDTO>> toCollectionModel(
      @NonNull Iterable<? extends InformationRequirementDTO> entities) {
    return RepresentationModelAssembler.super.toCollectionModel(entities);
  }
}
