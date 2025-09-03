package eu.bbmri_eric.negotiator.email;

import static eu.bbmri_eric.negotiator.common.LinkBuilder.getPageLinks;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
class NotificationEmailModelAssembler
    implements RepresentationModelAssembler<
        NotificationEmailDTO, EntityModel<NotificationEmailDTO>> {

  @Override
  public @NotNull EntityModel<NotificationEmailDTO> toModel(
      @NotNull NotificationEmailDTO notificationEmailDTO) {
    return EntityModel.of(notificationEmailDTO)
        .add(
            linkTo(
                    methodOn(NotificationEmailController.class)
                        .getNotificationEmail(notificationEmailDTO.getId()))
                .withSelfRel())
        .add(
            linkTo(
                    methodOn(NotificationEmailController.class)
                        .getAllNotificationEmails(new NotificationEmailFilterDTO()))
                .withRel("emails"));
  }

  public PagedModel<EntityModel<NotificationEmailDTO>> toPagedModel(
      Page<NotificationEmailDTO> page, NotificationEmailFilterDTO filters) {
    PagedModel.PageMetadata pageMetadata =
        new PagedModel.PageMetadata(
            page.getSize(), page.getNumber(), page.getTotalElements(), page.getTotalPages());

    List<Link> links =
        getPageLinks(
            linkTo(methodOn(NotificationEmailController.class).getAllNotificationEmails(null))
                .toUri(),
            filters,
            pageMetadata);

    return PagedModel.of(
        page.getContent().stream().map(this::toModel).toList(), pageMetadata, links);
  }
}
