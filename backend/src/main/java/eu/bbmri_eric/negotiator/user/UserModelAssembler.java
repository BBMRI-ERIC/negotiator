package eu.bbmri_eric.negotiator.user;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import eu.bbmri_eric.negotiator.negotiation.NegotiationController;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRole;
import eu.bbmri_eric.negotiator.negotiation.dto.NegotiationFilterDTO;
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
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.stereotype.Component;

@Component
public class UserModelAssembler
    implements RepresentationModelAssembler<UserResponseModel, EntityModel<UserResponseModel>> {
  public UserModelAssembler() {}

  @Override
  public EntityModel<UserResponseModel> toModel(UserResponseModel entity) {
    List<Link> links = getLinksForModel(entity);
    return EntityModel.of(entity, links);
  }

  public EntityModel<UserInfoModel> toModel(UserResponseModel entity, List<String> roles) {
    UserInfoModel userInfoModel = new UserInfoModel(entity, roles);
    List<Link> links = getLinksForModel(entity);
    return EntityModel.of(userInfoModel, links);
  }

  @NonNull
  private static List<Link> getLinksForModel(UserResponseModel entity) {
    List<Link> links = new ArrayList<>();
    links.add(linkTo(UserController.class).slash("users").withRel("users"));
    links.add(
        linkTo(methodOn(UserController.class).findById(Long.valueOf(entity.getId())))
            .withSelfRel());
    links.add(
        WebMvcLinkBuilder.linkTo(
                methodOn(NegotiationController.class)
                    .listRelated(Long.valueOf(entity.getId()), null))
            .withRel("negotiations")
            .expand());
    links.add(
        linkTo(
                methodOn(NegotiationController.class)
                    .listRelated(
                        Long.valueOf(entity.getId()),
                        NegotiationFilterDTO.builder()
                            .role(NegotiationRole.AUTHOR)
                            .size(10)
                            .build()))
            .withRel("authored_negotiations")
            .expand());
    if (entity.isRepresentativeOfAnyResource()) {
      links.add(
          linkTo(
                  methodOn(UserController.class)
                      .findRepresentedResources(Long.valueOf(entity.getId())))
              .withRel("represented_resources"));
      links.add(
          linkTo(
                  methodOn(NegotiationController.class)
                      .listRelated(
                          Long.valueOf(entity.getId()),
                          NegotiationFilterDTO.builder()
                              .role(NegotiationRole.REPRESENTATIVE)
                              .size(10)
                              .build()))
              .withRel("negotiations_representative")
              .expand());
    }
    if (entity.isNetworkManager()) {
      links.add(
          linkTo(
                  methodOn(UserController.class)
                      .getRepresentedNetworks(Long.valueOf(entity.getId()), 0, 50))
              .withRel("networks"));
    }
    return links;
  }

  public PagedModel<EntityModel<UserResponseModel>> toPagedModel(Page<UserResponseModel> page) {
    List<Link> links = getLinks(page);
    return PagedModel.of(
        page.getContent().stream().map(this::toModel).collect(Collectors.toList()),
        new PagedModel.PageMetadata(
            page.getSize(), page.getNumber(), page.getTotalElements(), page.getTotalPages()),
        links);
  }

  public PagedModel<EntityModel<UserResponseModel>> toPagedModel(UserResponseModel entity) {
    return PagedModel.of(List.of(toModel(entity)), new PagedModel.PageMetadata(1, 0, 1, 1));
  }

  public PagedModel<EntityModel<UserResponseModel>> toUnLinkedPagedModel(
      Page<UserResponseModel> page) {
    return PagedModel.of(
        page.getContent().stream().map(this::toModel).collect(Collectors.toList()),
        new PagedModel.PageMetadata(
            page.getSize(), page.getNumber(), page.getTotalElements(), page.getTotalPages()));
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
