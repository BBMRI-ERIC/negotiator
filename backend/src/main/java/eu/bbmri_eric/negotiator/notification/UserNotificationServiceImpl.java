package eu.bbmri_eric.negotiator.notification;

import eu.bbmri_eric.negotiator.common.AuthenticatedUserContext;
import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.common.exceptions.ForbiddenRequestException;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
class UserNotificationServiceImpl implements UserNotificationService {

  private final NotificationRepository notificationRepository;
  private final ModelMapper modelMapper;

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
        .findAllByRecipientId(
            userId,
            PageRequest.of(filters.getPage(), filters.getSize(), Sort.by("createdAt").descending()))
        .map(notification -> modelMapper.map(notification, NotificationDTO.class));
  }

  @Override
  public NotificationDTO getById(Long id) {
    Notification notification =
        notificationRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(id));
    if (!notification
        .getRecipientId()
        .equals(AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId())) {
      throw new ForbiddenRequestException("You are not authorized to view this Notification");
    }
    return modelMapper.map(notification, NotificationDTO.class);
  }

  @Override
  public List<NotificationDTO> updateNotifications(@NotNull List<NotificationUpdateDTO> updates) {
    Long currentUserId = AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId();
    List<NotificationDTO> updatedNotifications = new ArrayList<>();

    for (NotificationUpdateDTO update : updates) {
      Notification notification =
          notificationRepository
              .findById(update.getId())
              .orElseThrow(
                  () ->
                      new EntityNotFoundException(
                          "Notification with id " + update.getId() + " not found"));
      if (!notification.getRecipientId().equals(currentUserId)) {
        throw new ForbiddenRequestException(
            "User is not authorized to update notification with id " + update.getId());
      }
      notification.setRead(update.getRead());
      Notification savedNotification = notificationRepository.save(notification);
      updatedNotifications.add(modelMapper.map(savedNotification, NotificationDTO.class));
    }

    return updatedNotifications;
  }
}
