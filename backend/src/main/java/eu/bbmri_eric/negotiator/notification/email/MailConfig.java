package eu.bbmri_eric.negotiator.notification.email;

import java.util.Properties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
public class MailConfig {
  @Value("${spring.mail.host}")
  private String host;

  @Value("${spring.mail.port}")
  private int port;

  @Value("${spring.mail.username}")
  private String username;

  @Value("${spring.mail.password}")
  private String password;

  @Value("${spring.mail.properties.debug}")
  private boolean mailDebug;

  @Value("${spring.mail.properties.starttls}")
  private boolean starttls;

  @Value("${spring.mail.properties.auth}")
  private boolean auth;

  @Value("${spring.mail.protocol}")
  private String protocol;

  @Bean
  public JavaMailSender getJavaMailSender() {
    JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
    Properties props = mailSender.getJavaMailProperties();
    props.put("mail.transport.protocol", protocol);
    props.put("mail.smtp.auth", String.valueOf(auth));
    props.put("mail.smtp.starttls.enable", String.valueOf(starttls));
    props.put("mail.debug", String.valueOf(mailDebug));
    mailSender.setHost(host);
    mailSender.setPort(port);
    mailSender.setUsername(username);
    mailSender.setPassword(password);
    return mailSender;
  }
}
