package eu.bbmri_eric.negotiator.notification.email;

import java.util.ArrayList;
import java.util.List;
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
  public List<String> getAllNotificationTemplates() {
    log.debug("Getting all notification templates.");
    ArrayList<String> templates = emailTemplateRepository.listAll();
    return templates;
  }

  @Override
  public String getNotificationTemplate(String templateName) {
    log.debug("Getting notification template.");
    return emailTemplateRepository.load(templateName);
  }

  @Override
  public String updateNotificationTemplate(String templateName, String template) {
    log.debug("Updating notification template.");
    String validatedTemplate = validateHtml(template);
    emailTemplateRepository.save(templateName, validatedTemplate);
    return emailTemplateRepository.load(templateName);
  }

  @Override
  public String resetNotificationTemplate(String templateName) {
    log.debug("Resetting notification template.");
    emailTemplateRepository.reset(templateName);
    return emailTemplateRepository.load(templateName);
  }

  private String validateHtml(String html) {
    if (html == null) {
      throw new UnsupportedMediaTypeException("Template cannot be null.");
    }
    try {
      return Jsoup.parse(html).html();
    } catch (Exception e) {
      throw new UnsupportedMediaTypeException("Template is not valid HTML: " + e.getMessage());
    }
  }
}
