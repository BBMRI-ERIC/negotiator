package eu.bbmri.eric.csit.service.negotiator.service;


import java.util.Objects;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@AllArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    JavaMailSender javaMailSender;
    boolean devMode;
    @Override
    public boolean sendEmail(String recipientAddress) {
        if (!isValidEmailAddress(recipientAddress)){
            return false;
        }
        try{
            javaMailSender.send(buildMessage());
        }catch (MailSendException e){
            return false;
        }
        return true;
    }
    
    private static SimpleMailMessage buildMessage() {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@baeldung.com");
        message.setTo("idk");
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
