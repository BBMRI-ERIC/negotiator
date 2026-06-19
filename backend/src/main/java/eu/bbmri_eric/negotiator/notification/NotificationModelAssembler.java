package eu.bbmri_eric.negotiator.notification;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import eu.bbmri_eric.negotiator.common.LinkBuilder;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
class NotificationModelAssembler
    implements RepresentationModelAssembler<NotificationDTO, EntityModel<NotificationDTO>> {
  @Override
  public @NotNull EntityModel<NotificationDTO> toModel(@NotNull NotificationDTO entity) {
    return EntityModel.of(entity);
  }

  public PagedModel<EntityModel<NotificationDTO>> toPagedModel(
      Page<NotificationDTO> page, NotificationFilters filters, Long userId) {
    PagedModel.PageMetadata pageMetadata =
        new PagedModel.PageMetadata(
            page.getSize(), page.getNumber(), page.getTotalElements(), page.getTotalPages());
    return PagedModel.of(
        page.getContent().stream().map(this::toModel).toList(),
        pageMetadata,
        LinkBuilder.getPageLinks(
            linkTo(methodOn(NotificationController.class).getNotificationsByUserId(userId, null))
                .toUri(),
            filters,
            pageMetadata));
  }
}
