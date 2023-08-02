package eu.bbmri.eric.csit.service.negotiator.service;


import java.util.Objects;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@AllArgsConstructor
@CommonsLog
public class NotificationServiceImpl implements NotificationService {
    JavaMailSender javaMailSender;
    @Override
    public boolean sendEmail(String recipientAddress) {
        if (!isValidEmailAddress(recipientAddress)){
            log.error("Failed to send email. Invalid recipient email address.");
            return false;
        }
        try{
            javaMailSender.send(buildMessage(recipientAddress));
        }catch (MailSendException e){
            log.error("Failed to send email. Check SMTP configuration.");
            return false;
        }
        log.info("Email sent.");
        return true;
    }
    
    private static SimpleMailMessage buildMessage(String recipientAddress) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@baeldung.com");
        message.setTo(recipientAddress);
        message.setSubject("idk");
        message.setText("idk");
        return message;
    }

    private static boolean isValidEmailAddress(String recipientAddress) {
        if (Objects.isNull(recipientAddress)){
            return false;
        }
        String regexPattern = "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$";
        return Pattern.compile(regexPattern).matcher(recipientAddress).matches();
    }
}
