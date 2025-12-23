package eu.bbmri_eric.negotiator.notification;

import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@Transactional
class NotificationServiceImpl implements NotificationService {
  private final NotificationRepository notificationRepository;
  private final ApplicationEventPublisher eventPublisher;
  private final ModelMapper modelMapper;

  NotificationServiceImpl(
      NotificationRepository notificationRepository,
      ApplicationEventPublisher eventPublisher,
      ModelMapper modelMapper) {
    this.notificationRepository = notificationRepository;
    this.eventPublisher = eventPublisher;
    this.modelMapper = modelMapper;
  }

  @Override
  public List<NotificationDTO> createNotifications(@NotNull NotificationCreateDTO dto) {
    List<NotificationDTO> notificationDTOs = new ArrayList<>();
    dto.getUserIds()
        .forEach(
            id -> {
              Notification notification =
                  notificationRepository.save(
                      new Notification(id, dto.getNegotiationId(), dto.getTitle(), dto.getBody()));
              eventPublisher.publishEvent(new NewNotificationEvent(this, notification.getId()));
              notificationDTOs.add(modelMapper.map(notification, NotificationDTO.class));
            });
    return notificationDTOs;
  }

  @Override
  public NotificationDTO findById(@NotNull Long id) {
    return notificationRepository
        .findById(id)
        .map(notification -> modelMapper.map(notification, NotificationDTO.class))
        .orElseThrow(
            () -> new EntityNotFoundException("Notification with id " + id + " not found"));
  }

  @Override
  public int countByRecipientIdAndNegotiationId(
      @NotNull Long recipientId, @NotNull String negotiationId) {
    return notificationRepository.countByRecipientIdAndNegotiationId(recipientId, negotiationId);
  }
}
