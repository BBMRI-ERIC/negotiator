package eu.bbmri_eric.negotiator.notification;

import eu.bbmri_eric.negotiator.common.AuthenticatedUserContext;
import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.common.exceptions.ForbiddenRequestException;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
class UserNotificationServiceImpl implements UserNotificationService {

  private NotificationRepository notificationRepository;
  private ModelMapper modelMapper;

  public UserNotificationServiceImpl(
      NotificationRepository notificationRepository, ModelMapper modelMapper) {
    this.notificationRepository = notificationRepository;
    this.modelMapper = modelMapper;
  }

  @Override
  public Iterable<NotificationDTO> getAllByUserId(Long userId, NotificationFilters filters) {
    if (!AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId().equals(userId)) {
      throw new ForbiddenRequestException(
          "You are not authorized to view Notifications of this user");
    }
    return notificationRepository
        .findAllByRecipient_id(
            userId,
            PageRequest.of(
                filters.getPage(), filters.getSize(), Sort.by("creationDate").descending()))
        .map(notification -> modelMapper.map(notification, NotificationDTO.class));
  }

  @Override
  public NotificationDTO getById(Long id) {
    Notification notification =
        notificationRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(id));
    if (!notification
        .getRecipient()
        .getId()
        .equals(AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId())) {
      throw new ForbiddenRequestException("You are not authorized to view this Notification");
    }
    return modelMapper.map(notification, NotificationDTO.class);
  }
}
