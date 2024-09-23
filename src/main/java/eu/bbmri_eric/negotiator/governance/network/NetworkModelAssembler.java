package eu.bbmri_eric.negotiator.governance.network;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import eu.bbmri_eric.negotiator.governance.resource.ResourceController;
import eu.bbmri_eric.negotiator.user.UserController;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.stereotype.Component;

@Component
public class NetworkModelAssembler
    implements RepresentationModelAssembler<NetworkDTO, EntityModel<NetworkDTO>> {

  @Override
  public @NonNull EntityModel<NetworkDTO> toModel(@NonNull NetworkDTO entity) {
    EntityModel<NetworkDTO> networkModel =
        EntityModel.of(entity)
            .add(
                WebMvcLinkBuilder.linkTo(
                        WebMvcLinkBuilder.methodOn(NetworkController.class)
                            .findById(entity.getId()))
                    .withSelfRel())
            .add(WebMvcLinkBuilder.linkTo(NetworkController.class).withRel("networks"));

    networkModel.add(
        WebMvcLinkBuilder.linkTo(
                WebMvcLinkBuilder.methodOn(NetworkController.class)
                    .getManagers(entity.getId(), 0, 50))
            .withRel("managers"));

    return networkModel;
  }

  @Override
  public CollectionModel<EntityModel<NetworkDTO>> toCollectionModel(
      Iterable<? extends NetworkDTO> entities) {
    List<EntityModel<NetworkDTO>> entityModels = new ArrayList<>();
    for (NetworkDTO networkDTO : entities) {
      entityModels.add(toModel(networkDTO));
    }
    return CollectionModel.of(entityModels)
        .add(linkTo(ResourceController.class).withRel("networks"));
  }

  public PagedModel<EntityModel<NetworkDTO>> toPagedModel(@NonNull Page<NetworkDTO> page) {
    List<Link> links = getLinks(page);
    return PagedModel.of(
        page.getContent().stream().map(this::toModel).collect(Collectors.toList()),
        new PagedModel.PageMetadata(
            page.getSize(), page.getNumber(), page.getTotalElements(), page.getTotalPages()),
        links);
  }

  public PagedModel<EntityModel<NetworkDTO>> toPagedModel(
      @NonNull Page<NetworkDTO> page, Long managerId) {
    List<Link> links = new ArrayList<>();
    if (page.hasContent()) {
      links = getLinks(page, managerId);
    }
    return PagedModel.of(
        page.getContent().stream().map(this::toModel).collect(Collectors.toList()),
        new PagedModel.PageMetadata(
            page.getSize(), page.getNumber(), page.getTotalElements(), page.getTotalPages()),
        links);
  }

  @NonNull
  private static List<Link> getLinks(Page<NetworkDTO> page) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("filter", null);
    List<Link> links = new ArrayList<>();
    if (page.hasPrevious()) {
      links.add(
          linkTo(methodOn(NetworkController.class).list(page.getNumber() - 1, page.getSize()))
              .withRel(IanaLinkRelations.PREVIOUS)
              .expand(parameters));
    }
    if (page.hasNext()) {
      links.add(
          linkTo(methodOn(NetworkController.class).list(page.getNumber() + 1, page.getSize()))
              .withRel(IanaLinkRelations.NEXT)
              .expand(parameters));
    }
    links.add(
        linkTo(methodOn(NetworkController.class).list(0, page.getSize()))
            .withRel("first")
            .expand(parameters));
    links.add(
        linkTo(methodOn(NetworkController.class).list(page.getNumber(), page.getSize()))
            .withRel(IanaLinkRelations.CURRENT)
            .expand());
    links.add(
        linkTo(methodOn(NetworkController.class).list(page.getTotalPages() - 1, page.getSize()))
            .withRel(IanaLinkRelations.LAST)
            .expand(parameters));
    return links;
  }

  @NonNull
  private static List<Link> getLinks(Page<NetworkDTO> page, long managerId) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("filter", null);
    List<Link> links = new ArrayList<>();
    if (page.hasPrevious()) {
      links.add(
          linkTo(
                  methodOn(UserController.class)
                      .getRepresentedNetworks(managerId, page.getNumber() - 1, page.getSize()))
              .withRel(IanaLinkRelations.PREVIOUS)
              .expand(parameters));
    }
    if (page.hasNext()) {
      links.add(
          linkTo(
                  methodOn(UserController.class)
                      .getRepresentedNetworks(managerId, page.getNumber() + 1, page.getSize()))
              .withRel(IanaLinkRelations.NEXT)
              .expand(parameters));
    }
    links.add(
        linkTo(methodOn(UserController.class).getRepresentedNetworks(managerId, 0, page.getSize()))
            .withRel("first")
            .expand(parameters));
    links.add(
        linkTo(
                methodOn(UserController.class)
                    .getRepresentedNetworks(managerId, page.getNumber(), page.getSize()))
            .withRel(IanaLinkRelations.CURRENT)
            .expand());
    links.add(
        linkTo(
                methodOn(UserController.class)
                    .getRepresentedNetworks(managerId, page.getTotalPages() - 1, page.getSize()))
            .withRel(IanaLinkRelations.LAST)
            .expand(parameters));
    return links;
  }
}
