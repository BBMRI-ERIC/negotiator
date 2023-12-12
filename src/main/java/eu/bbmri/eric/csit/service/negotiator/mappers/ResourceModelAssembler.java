package eu.bbmri.eric.csit.service.negotiator.mappers;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import eu.bbmri.eric.csit.service.negotiator.api.controller.v3.ResourceController;
import eu.bbmri.eric.csit.service.negotiator.dto.person.ResourceResponseModel;
import java.util.ArrayList;
import java.util.List;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
public class ResourceModelAssembler
    implements RepresentationModelAssembler<
        ResourceResponseModel, EntityModel<ResourceResponseModel>> {
  public ResourceModelAssembler() {}

  @Override
  public EntityModel<ResourceResponseModel> toModel(ResourceResponseModel entity) {
    return EntityModel.of(entity)
        .add(
            linkTo(methodOn(ResourceController.class).getResourceById(Long.valueOf(entity.getId())))
                .withSelfRel(),
            linkTo(ResourceController.class).withRel("resources"));
  }

  @Override
  public CollectionModel<EntityModel<ResourceResponseModel>> toCollectionModel(
      Iterable<? extends ResourceResponseModel> entities) {
    List<EntityModel<ResourceResponseModel>> entityModels = new ArrayList<>();
    for (ResourceResponseModel resourceResponseModel : entities) {
      entityModels.add(toModel(resourceResponseModel));
    }
    return CollectionModel.of(entityModels)
        .add(linkTo(ResourceController.class).withRel("resources"));
  }
}