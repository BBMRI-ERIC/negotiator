package eu.bbmri_eric.negotiator.mappers;

import eu.bbmri_eric.negotiator.dto.access_form.AccessFormDTO;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
public class AccessFormModelAssembler
    implements RepresentationModelAssembler<AccessFormDTO, EntityModel<AccessFormDTO>> {

  @Override
  public @NonNull EntityModel<AccessFormDTO> toModel(@NonNull AccessFormDTO entity) {
    List<Link> links = new ArrayList<>();
    return EntityModel.of(entity, links);
  }

  @Override
  public @NonNull CollectionModel<EntityModel<AccessFormDTO>> toCollectionModel(
      @NonNull Iterable<? extends AccessFormDTO> entities) {
    return RepresentationModelAssembler.super.toCollectionModel(entities);
  }
}
