package eu.bbmri_eric.negotiator.service;

import org.springframework.stereotype.Service;

@Service
public interface NotificationService {
  boolean sendEmail(String recipientAddress, String subject, String mailBody);
}
