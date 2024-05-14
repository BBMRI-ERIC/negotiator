package eu.bbmri_eric.negotiator.mappers;

import eu.bbmri_eric.negotiator.api.controller.v3.NetworkController;
import eu.bbmri_eric.negotiator.dto.NetworkDTO;
import java.util.ArrayList;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.stereotype.Component;

@Component
public class NetworkModelAssembler
    implements RepresentationModelAssembler<NetworkDTO, EntityModel<NetworkDTO>> {

  @Override
  public @NonNull EntityModel<NetworkDTO> toModel(@NonNull NetworkDTO entity) {
    return EntityModel.of(entity)
        .add(
            WebMvcLinkBuilder.linkTo(
                    WebMvcLinkBuilder.methodOn(NetworkController.class).findById(entity.getId()))
                .withSelfRel(),
            WebMvcLinkBuilder.linkTo(NetworkController.class).withRel("networks"));
  }

  public PagedModel<EntityModel<NetworkDTO>> toPagedModel(@NonNull Page<NetworkDTO> page) {
    return PagedModel.of(
        page.getContent().stream().map(this::toModel).collect(Collectors.toList()),
        new PagedModel.PageMetadata(
            page.getSize(), page.getNumber(), page.getTotalElements(), page.getTotalPages()),
        new ArrayList<>());
  }
}
