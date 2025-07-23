package eu.bbmri_eric.negotiator.template;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.apachecommons.CommonsLog;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.UnsupportedMediaTypeException;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
@CommonsLog
public class TemplateServiceImpl implements TemplateService {
  private TemplateRepository templateRepository;
  private SpringTemplateEngine templateEngine;

  public TemplateServiceImpl(TemplateRepository templateRepository,
                             SpringTemplateEngine templateEngine) {
    this.templateRepository = templateRepository;
    this.templateEngine = templateEngine;
  }

  @Override
  public List<String> getAllNotificationTemplates() {
    log.debug("Getting all notification templates.");
    ArrayList<String> templates = templateRepository.listAll();
    return templates;
  }

  @Override
  public String getNotificationTemplate(String templateName) {
    log.debug("Getting notification template.");
    return templateRepository.load(templateName);
  }

  @Override
  public String updateNotificationTemplate(String templateName, String template) {
    log.debug("Updating notification template.");
    String validatedTemplate = validateHtml(template);
    templateRepository.save(templateName, validatedTemplate);
    templateEngine.clearTemplateCache();
    return templateRepository.load(templateName);
  }

  @Override
  public String resetNotificationTemplate(String templateName) {
    log.debug("Resetting notification template.");
    templateRepository.reset(templateName);
    return templateRepository.load(templateName);
  }

  private String validateHtml(String html) {
    if (html == null) {
      throw new UnsupportedMediaTypeException("Template cannot be null.");
    }
    try {
      Document doc = Jsoup.parse(html);
      doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
      return doc.outerHtml();
    } catch (Exception e) {
      throw new UnsupportedMediaTypeException("Template is not valid HTML: " + e.getMessage());
    }
  }
}
