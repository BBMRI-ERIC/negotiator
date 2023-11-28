package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.database.model.Person;

public interface EmailService {
  void sendEmail(Person recipient, String subject, String mailBody);
}
