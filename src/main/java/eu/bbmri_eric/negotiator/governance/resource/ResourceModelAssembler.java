package eu.bbmri_eric.negotiator.governance.resource;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import eu.bbmri_eric.negotiator.governance.network.NetworkController;
import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceFilterDTO;
import eu.bbmri_eric.negotiator.user.ResourceResponseModel;
import java.lang.reflect.Field;
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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class ResourceModelAssembler
    implements RepresentationModelAssembler<
        ResourceResponseModel, EntityModel<ResourceResponseModel>> {
  public ResourceModelAssembler() {}

  @Override
  public EntityModel<ResourceResponseModel> toModel(ResourceResponseModel entity) {
    return EntityModel.of(entity)
        .add(
            WebMvcLinkBuilder.linkTo(
                    methodOn(ResourceController.class)
                        .getResourceById(Long.valueOf(entity.getId())))
                .withSelfRel(),
            linkTo(ResourceController.class).withRel("resources"));
  }

  public PagedModel<EntityModel<ResourceResponseModel>> toPagedModel(
      Page<ResourceResponseModel> page, ResourceFilterDTO filterDTO) {
    List<Link> links = new ArrayList<>();
    UriComponents uriComponents =
        UriComponentsBuilder.fromUri(
                linkTo(methodOn(ResourceController.class).list(filterDTO)).toUri())
            .queryParams(getQueryParams(filterDTO))
            .build();
    links.add(Link.of(uriComponents.toUriString()).withRel("current"));
    return PagedModel.of(
        page.getContent().stream().map(this::toModel).collect(Collectors.toList()),
        new PagedModel.PageMetadata(
            page.getSize(), page.getNumber(), page.getTotalElements(), page.getTotalPages()),
        links);
  }

  public PagedModel<EntityModel<ResourceResponseModel>> toPagedModel(
      Page<ResourceResponseModel> page, long networkId) {
    List<Link> links = new ArrayList<>();
    if (page.hasContent()) {
      links = getLinks(page, networkId);
    }
    return PagedModel.of(
        page.getContent().stream().map(this::toModel).collect(Collectors.toList()),
        new PagedModel.PageMetadata(
            page.getSize(), page.getNumber(), page.getTotalElements(), page.getTotalPages()),
        links);
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

  @NonNull
  private static List<Link> getLinks(Page<ResourceResponseModel> page, Long networkId) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("filter", null);
    List<Link> links = new ArrayList<>();
    if (page.hasPrevious()) {
      links.add(
          linkTo(
                  methodOn(NetworkController.class)
                      .getResources(networkId, page.getNumber() - 1, page.getSize()))
              .withRel(IanaLinkRelations.PREVIOUS)
              .expand(parameters));
    }
    if (page.hasNext()) {
      links.add(
          linkTo(
                  methodOn(NetworkController.class)
                      .getResources(networkId, page.getNumber() + 1, page.getSize()))
              .withRel(IanaLinkRelations.NEXT)
              .expand(parameters));
    }
    links.add(
        linkTo(methodOn(NetworkController.class).getResources(networkId, 0, page.getSize()))
            .withRel("first")
            .expand(parameters));
    links.add(
        linkTo(
                methodOn(NetworkController.class)
                    .getResources(networkId, page.getNumber(), page.getSize()))
            .withRel(IanaLinkRelations.CURRENT)
            .expand());
    links.add(
        linkTo(
                methodOn(NetworkController.class)
                    .getResources(networkId, page.getTotalPages() - 1, page.getSize()))
            .withRel(IanaLinkRelations.LAST)
            .expand(parameters));
    return links;
  }

  private static MultiValueMap<String, String> getQueryParams(Object filterDTO) {
    MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    Field[] fields = filterDTO.getClass().getDeclaredFields();

    for (Field field : fields) {
      try {
        field.setAccessible(true);
        Object value = field.get(filterDTO);
        if (value != null) {
          queryParams.add(field.getName(), String.valueOf(value));
        }
      } catch (IllegalAccessException e) {
        // Handle exception or log error
      }
    }

    return queryParams;
  }
}
