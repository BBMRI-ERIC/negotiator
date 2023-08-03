package eu.bbmri.eric.csit.service.negotiator.service;

import org.springframework.stereotype.Service;

@Service
public interface NotificationService {
  public boolean sendEmail(String recipientAddress, String subject, String mailBody);
}
