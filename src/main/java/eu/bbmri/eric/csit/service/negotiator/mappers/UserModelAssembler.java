package eu.bbmri.eric.csit.service.negotiator.mappers;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import eu.bbmri.eric.csit.service.negotiator.api.controller.v3.RequestController;
import eu.bbmri.eric.csit.service.negotiator.api.controller.v3.UserController;
import eu.bbmri.eric.csit.service.negotiator.dto.person.ResourceModel;
import eu.bbmri.eric.csit.service.negotiator.dto.person.UserModel;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
public class UserModelAssembler
    implements RepresentationModelAssembler<UserModel, EntityModel<UserModel>> {
  public UserModelAssembler() {}

  @Override
  public EntityModel<UserModel> toModel(UserModel entity) {
    List<Link> links = new ArrayList<>();
    links.add(linkTo(UserController.class).slash("users").withRel("users"));
    links.add(
        linkTo(methodOn(UserController.class).findById(Long.valueOf(entity.getId())))
            .withSelfRel());
    for (ResourceModel resourceModel : entity.getRepresentedResources()) {
      links.add(
          linkTo(methodOn(RequestController.class).retrieve(resourceModel.getId()))
              .withRel("requests"));
    }
    return EntityModel.of(entity, links);
  }

  public PagedModel<EntityModel<UserModel>> toPagedModel(Page<UserModel> page) {
    List<Link> links = getLinks(page);
    return PagedModel.of(
        page.getContent().stream().map(this::toModel).collect(Collectors.toList()),
        new PagedModel.PageMetadata(
            page.getSize(), page.getNumber(), page.getTotalElements(), page.getTotalPages()),
        links);
  }

  @NonNull
  private static List<Link> getLinks(Page<UserModel> page) {
    List<Link> links = new ArrayList<>();
    if (page.hasPrevious()) {
      links.add(
          linkTo(methodOn(UserController.class).listUsers(page.getNumber() - 1, page.getSize()))
              .withRel("prev"));
    }
    if (page.hasNext()) {
      links.add(
          linkTo(methodOn(UserController.class).listUsers(page.getNumber() + 1, page.getSize()))
              .withRel("next"));
    }
    links.add(linkTo(methodOn(UserController.class).listUsers(0, page.getSize())).withRel("first"));
    links.add(
        linkTo(methodOn(UserController.class).listUsers(page.getNumber(), page.getSize()))
            .withSelfRel());
    links.add(
        linkTo(methodOn(UserController.class).listUsers(page.getTotalPages() - 1, page.getSize()))
            .withRel("last"));
    return links;
  }
}
