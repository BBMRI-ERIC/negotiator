package eu.bbmri_eric.negotiator.email;

import eu.bbmri_eric.negotiator.user.Person;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.regex.Pattern;
import lombok.NonNull;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
  @Value("${negotiator.email-address}")
  private String from;

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
      @NonNull String mailBody,
      @NonNull String from) {
    try {
      MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
      helper.setText(mailBody, true);
      helper.setTo(recipientAddress);
      helper.setSubject(subject);
      helper.setFrom(from);
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
      mimeMessage = buildMimeMessage(mimeMessage, recipient.getEmail(), subject, mailBody, from);
    } catch (NullPointerException e) {
      log.error("Failed to send email. Check message content.");
      return;
    } catch (RuntimeException e) {
      log.error("Failed to send email.");
      return;
    }
    NotificationEmail notificationEmail =
        notificationEmailRepository.save(
            NotificationEmail.builder()
                .address(recipient.getEmail())
                .message(mailBody)
                .sentAt(LocalDateTime.now())
                .build());
    try {
      javaMailSender.send(mimeMessage);
    } catch (MailSendException e) {
      log.error("Failed to send email. Check SMTP configuration.");
      return;
    }
    notificationEmailRepository.save(notificationEmail);
    log.debug("Email message sent.");
  }
}
