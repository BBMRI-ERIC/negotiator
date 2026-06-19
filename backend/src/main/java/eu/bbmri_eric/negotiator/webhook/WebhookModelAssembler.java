package eu.bbmri_eric.negotiator.webhook;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import jakarta.validation.constraints.NotNull;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
public class WebhookModelAssembler
    implements RepresentationModelAssembler<WebhookResponseDTO, EntityModel<WebhookResponseDTO>> {

  @Override
  public @NotNull EntityModel<WebhookResponseDTO> toModel(WebhookResponseDTO dto) {
    return EntityModel.of(
        dto,
        linkTo(methodOn(WebhookController.class).getWebhookById(dto.getId())).withSelfRel(),
        linkTo(methodOn(WebhookController.class).getAllWebhooks()).withRel("webhooks"));
  }
}
