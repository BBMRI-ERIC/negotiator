package eu.bbmri.eric.csit.service.negotiator.mappers;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import eu.bbmri.eric.csit.service.negotiator.api.controller.v3.UserController;
import eu.bbmri.eric.csit.service.negotiator.dto.person.UserModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
public class UserModelAssembler
    implements RepresentationModelAssembler<UserModel, EntityModel<UserModel>> {
  public UserModelAssembler() {}

  @Override
  public EntityModel<UserModel> toModel(UserModel entity) {
    return EntityModel.of(
        entity,
        linkTo(methodOn(UserController.class).findById(Long.valueOf(entity.getId()))).withSelfRel(),
        linkTo(methodOn(UserController.class).listUsers()).withRel("users"));
  }
}
