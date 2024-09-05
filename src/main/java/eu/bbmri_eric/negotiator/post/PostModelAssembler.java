package eu.bbmri_eric.negotiator.post;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
public class PostModelAssembler
    implements RepresentationModelAssembler<PostDTO, EntityModel<PostDTO>> {

  public PostModelAssembler() {}

  @Override
  public @NonNull EntityModel<PostDTO> toModel(@NonNull PostDTO entity) {
    List<Link> links = new ArrayList<>();
    links.add(linkTo(methodOn(PostController.class).getById(entity.getId())).withSelfRel());
    return EntityModel.of(entity, links);
  }
}
