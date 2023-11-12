package eu.bbmri.eric.csit.service.negotiator.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public interface NotificationService {
  @Async
  void sendEmail(String recipientAddress, String subject, String mailBody);
}
