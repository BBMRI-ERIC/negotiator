package eu.bbmri.eric.csit.service.negotiator.service;


public interface EmailService {
  void sendEmail(String recipientAddress, String subject, String mailBody);
}
