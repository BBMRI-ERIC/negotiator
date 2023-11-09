package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.database.repository.NotificationRepository;
import eu.bbmri.eric.csit.service.negotiator.dto.NotificationDTO;
import java.util.List;
import java.util.stream.Collectors;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserNotificationServiceImpl implements UserNotificationService {

  @Autowired NotificationRepository notificationRepository;
  @Autowired ModelMapper modelMapper;

  @Override
  public List<NotificationDTO> getNotificationsForUser(Long userId) {
    return notificationRepository.findByRecipientId(userId).stream()
        .map(notification -> modelMapper.map(notification, NotificationDTO.class))
        .collect(Collectors.toList());
  }
}
