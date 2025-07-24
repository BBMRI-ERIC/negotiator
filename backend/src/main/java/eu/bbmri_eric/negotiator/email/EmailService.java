package eu.bbmri_eric.negotiator.email;

import eu.bbmri_eric.negotiator.user.Person;

public interface EmailService {
  void sendEmail(Person recipient, String subject, String mailBody);
}
