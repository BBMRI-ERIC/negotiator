package eu.bbmri_eric.negotiator.notification.email;

import com.google.gson.Gson;
import java.util.ArrayList;
import lombok.extern.apachecommons.CommonsLog;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.UnsupportedMediaTypeException;

@Service
@CommonsLog
public class EmailTemplateServiceImpl implements EmailTemplateService {
  private EmailTemplateRepository emailTemplateRepository;

  public EmailTemplateServiceImpl(EmailTemplateRepository emailTemplateRepository) {
    this.emailTemplateRepository = emailTemplateRepository;
  }

  @Override
  public String getAllNotificationTemplates() {
    log.info("Getting all notification templates.");
    ArrayList<String> templates = emailTemplateRepository.listAll();
    return new Gson().toJson(templates);
  }

  @Override
  public String getNotificationTemplate(String templateName) {
    log.info("Getting notification template.");
    return emailTemplateRepository.load(templateName);
  }

  @Override
  public String updateNotificationTemplate(String templateName, String template) {
    log.info("Updating notification template.");
    String validatedTemplate = validateHtml(template);
    emailTemplateRepository.save(templateName, validatedTemplate);
    return emailTemplateRepository.load(templateName);
  }

  @Override
  public String resetNotificationTemplate(String templateName) {
    log.info("Resetting notification template.");
    emailTemplateRepository.reset(templateName);
    return emailTemplateRepository.load(templateName);
  }

  private String validateHtml(String html) {
    try {
      return Jsoup.parse(html).html();
    } catch (Exception e) {
      throw new UnsupportedMediaTypeException("Template is not valid HTML: " + e.getMessage());
    }
  }
}
