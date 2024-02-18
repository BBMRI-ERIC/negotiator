package eu.bbmri_eric.negotiator.service;

import eu.bbmri_eric.negotiator.database.model.Person;

public interface EmailService {
  void sendEmail(Person recipient, String subject, String mailBody);
}
