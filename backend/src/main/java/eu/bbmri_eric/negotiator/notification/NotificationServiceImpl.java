package eu.bbmri_eric.negotiator.notification;

import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
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
                      new Notification(id, dto.getTitle(), dto.getBody(), dto.getNegotiationId()));
              eventPublisher.publishEvent(new NewNotificationEvent(this, notification.getId()));
              notificationDTOs.add(modelMapper.map(notification, NotificationDTO.class));
            });
    return notificationDTOs;
  }
}
