package eu.bbmri.eric.csit.service.negotiator.mappers;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import eu.bbmri.eric.csit.service.negotiator.api.controller.v3.UserController;
import eu.bbmri.eric.csit.service.negotiator.dto.person.UserResponseModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
public class UserModelAssembler
    implements RepresentationModelAssembler<UserResponseModel, EntityModel<UserResponseModel>> {
  public UserModelAssembler() {}

  @Override
  public EntityModel<UserResponseModel> toModel(UserResponseModel entity) {
    List<Link> links = new ArrayList<>();
    links.add(linkTo(UserController.class).slash("users").withRel("users"));
    links.add(
        linkTo(methodOn(UserController.class).findById(Long.valueOf(entity.getId())))
            .withSelfRel());
    if (entity.isRepresentativeOfAnyResource()) {
      links.add(
          linkTo(
                  methodOn(UserController.class)
                      .findRepresentedResources(Long.valueOf(entity.getId())))
              .withRel("represented_resources"));
    }
    return EntityModel.of(entity, links);
  }

  public PagedModel<EntityModel<UserResponseModel>> toPagedModel(Page<UserResponseModel> page) {
    List<Link> links = getLinks(page);
    return PagedModel.of(
        page.getContent().stream().map(this::toModel).collect(Collectors.toList()),
        new PagedModel.PageMetadata(
            page.getSize(), page.getNumber(), page.getTotalElements(), page.getTotalPages()),
        links);
  }

  @NonNull
  private static List<Link> getLinks(Page<UserResponseModel> page) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("filter", null);
    List<Link> links = new ArrayList<>();
    if (page.hasPrevious()) {
      links.add(
          linkTo(
                  methodOn(UserController.class)
                      .listUsers(null, page.getNumber() - 1, page.getSize()))
              .withRel(IanaLinkRelations.PREVIOUS)
              .expand(parameters));
    }
    if (page.hasNext()) {
      links.add(
          linkTo(
                  methodOn(UserController.class)
                      .listUsers(null, page.getNumber() + 1, page.getSize()))
              .withRel(IanaLinkRelations.NEXT)
              .expand(parameters));
    }
    links.add(
        linkTo(methodOn(UserController.class).listUsers(null, 0, page.getSize()))
            .withRel("first")
            .expand(parameters));
    links.add(
        linkTo(methodOn(UserController.class).listUsers(null, page.getNumber(), page.getSize()))
            .withRel(IanaLinkRelations.CURRENT)
            .expand());
    links.add(
        linkTo(
                methodOn(UserController.class)
                    .listUsers(null, page.getTotalPages() - 1, page.getSize()))
            .withRel(IanaLinkRelations.LAST)
            .expand(parameters));
    return links;
  }
}