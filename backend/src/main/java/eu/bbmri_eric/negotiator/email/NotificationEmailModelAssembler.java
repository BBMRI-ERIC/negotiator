package eu.bbmri_eric.negotiator.email;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import org.springframework.data.domain.Page;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
public class NotificationEmailModelAssembler
    implements RepresentationModelAssembler<
        NotificationEmailDTO, EntityModel<NotificationEmailDTO>> {

  @Override
  public EntityModel<NotificationEmailDTO> toModel(NotificationEmailDTO notificationEmailDTO) {
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
    PagedModel<EntityModel<NotificationEmailDTO>> pagedModel =
        PagedModel.of(
            page.getContent().stream().map(this::toModel).toList(),
            new PagedModel.PageMetadata(
                page.getSize(), page.getNumber(), page.getTotalElements(), page.getTotalPages()));

    pagedModel.add(
        linkTo(methodOn(NotificationEmailController.class).getAllNotificationEmails(filters))
            .withSelfRel());

    if (page.hasNext()) {
      NotificationEmailFilterDTO nextFilters = createFiltersForPage(filters, page.getNumber() + 1);
      pagedModel.add(
          linkTo(methodOn(NotificationEmailController.class).getAllNotificationEmails(nextFilters))
              .withRel("next"));
    }

    if (page.hasPrevious()) {
      NotificationEmailFilterDTO prevFilters = createFiltersForPage(filters, page.getNumber() - 1);
      pagedModel.add(
          linkTo(methodOn(NotificationEmailController.class).getAllNotificationEmails(prevFilters))
              .withRel("prev"));
    }

    NotificationEmailFilterDTO firstFilters = createFiltersForPage(filters, 0);
    pagedModel.add(
        linkTo(methodOn(NotificationEmailController.class).getAllNotificationEmails(firstFilters))
            .withRel("first"));

    NotificationEmailFilterDTO lastFilters =
        createFiltersForPage(filters, page.getTotalPages() - 1);
    pagedModel.add(
        linkTo(methodOn(NotificationEmailController.class).getAllNotificationEmails(lastFilters))
            .withRel("last"));

    return pagedModel;
  }

  private NotificationEmailFilterDTO createFiltersForPage(
      NotificationEmailFilterDTO originalFilters, int page) {
    NotificationEmailFilterDTO filters = new NotificationEmailFilterDTO();
    filters.setPage(page);
    filters.setSize(originalFilters.getSize());
    filters.setAddress(originalFilters.getAddress());
    filters.setSentAfter(originalFilters.getSentAfter());
    filters.setSentBefore(originalFilters.getSentBefore());
    filters.setSort(originalFilters.getSort());
    return filters;
  }
}
