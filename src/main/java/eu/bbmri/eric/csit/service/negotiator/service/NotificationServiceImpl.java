package eu.bbmri.eric.csit.service.negotiator.service;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@CommonsLog
@Service
public class NotificationServiceImpl implements NotificationService {

  @Autowired private JavaMailSender emailSender;

  @Override
  public boolean sendEmailToResourceRepresentatives(String resourceId, String mailBody) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom("from@example.com");
    message.setTo("to@example.com");
    message.setSubject("java test");
    message.setText("idk");
    try {
      emailSender.send(message);
    } catch (MailException e) {
      e.getStackTrace();
      return false;
    }
    return true;
  }
}
