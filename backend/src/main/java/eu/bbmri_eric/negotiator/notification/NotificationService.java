package eu.bbmri_eric.negotiator.notification;

import jakarta.annotation.Nonnull;
import java.util.List;

public interface NotificationService {
  List<NotificationDTO> createNotifications(@Nonnull NotificationCreateDTO notificationRequest);
}
