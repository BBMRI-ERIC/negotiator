package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.database.model.NotificationEmail;
import eu.bbmri.eric.csit.service.negotiator.database.model.Person;
import eu.bbmri.eric.csit.service.negotiator.database.repository.NotificationEmailRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.Objects;
import java.util.regex.Pattern;
import lombok.NonNull;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
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

  private static MimeMessage buildMimeMessage(
      @NonNull MimeMessage mimeMessage,
      @NonNull String recipientAddress,
      @NonNull String subject,
      @NonNull String mailBody) {
    try {
      MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
      helper.setText(mailBody, true);
      helper.setTo(recipientAddress);
      helper.setSubject(subject);
      helper.setFrom("noreply@bbmri-eric.eu");
    } catch (MessagingException e) {
      throw new NullPointerException(e.toString());
    }
    return mimeMessage;
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
    MimeMessage mimeMessage = javaMailSender.createMimeMessage();
    if (!isValidEmailAddress(recipient.getEmail())) {
      log.error("Failed to send email. Invalid recipient email address.");
      return;
    }
    try {
      mimeMessage = buildMimeMessage(mimeMessage, recipient.getEmail(), subject, mailBody);
    } catch (NullPointerException e) {
      log.error("Failed to send email. Check message content.");
      return;
    }
    NotificationEmail notificationEmail =
        notificationEmailRepository.save(
            NotificationEmail.builder().recipient(recipient).message(mailBody).build());
    try {
      javaMailSender.send(mimeMessage);
    } catch (MailSendException e) {
      log.error("Failed to send email. Check SMTP configuration.");
      return;
    }
    notificationEmail.setWasSuccessfullySent(true);
    notificationEmailRepository.save(notificationEmail);
    log.info("Email message sent.");
  }
}
