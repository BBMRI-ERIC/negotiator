package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.database.model.NotificationEmail;
import eu.bbmri.eric.csit.service.negotiator.database.model.Person;
import eu.bbmri.eric.csit.service.negotiator.database.repository.NotificationEmailRepository;
import java.util.Objects;
import java.util.regex.Pattern;
import lombok.NonNull;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@CommonsLog
@Service
public class EmailServiceImpl implements EmailService {
  JavaMailSender javaMailSender;
  NotificationEmailRepository notificationEmailRepository;

  public EmailServiceImpl(
      @Autowired JavaMailSender javaMailSender,
      @Autowired NotificationEmailRepository notificationEmailRepository) {
    this.javaMailSender = javaMailSender;
    this.notificationEmailRepository = notificationEmailRepository;
  }

  private static SimpleMailMessage buildMessage(
      @NonNull String recipientAddress, @NonNull String subject, @NonNull String mailBody) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom("noreply@bbmri-eric.eu");
    message.setTo(recipientAddress);
    message.setSubject(subject);
    message.setText(mailBody);
    return message;
  }

  private static boolean isValidEmailAddress(String recipientAddress) {
    if (Objects.isNull(recipientAddress)) {
      return false;
    }
    String regexPattern = "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$";
    return Pattern.compile(regexPattern).matcher(recipientAddress).matches();
  }

  @Override
  @Async
  public void sendEmail(Person recipient, String subject, String mailBody) {
    SimpleMailMessage message;
    if (!isValidEmailAddress(recipient.getAuthEmail())) {
      log.error("Failed to send email. Invalid recipient email address.");
      return;
    }
    try {
      message = buildMessage(recipient.getAuthEmail(), subject, mailBody);
    } catch (NullPointerException e) {
      log.error("Failed to send email. Check message content.");
      return;
    }
    NotificationEmail notificationEmail =
        notificationEmailRepository.save(
            NotificationEmail.builder().recipient(recipient).message(message.toString()).build());
    try {
      javaMailSender.send(message);
    } catch (MailSendException e) {
      log.error("Failed to send email. Check SMTP configuration.");
      return;
    }
    notificationEmail.setWas_successfully_sent(true);
    notificationEmailRepository.save(notificationEmail);
    log.info("Email message sent.");
  }
}
