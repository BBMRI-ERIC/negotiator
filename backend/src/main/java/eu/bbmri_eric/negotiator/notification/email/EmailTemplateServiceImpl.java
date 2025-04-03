package eu.bbmri_eric.negotiator.notification.email;

import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.common.exceptions.ForbiddenRequestException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import lombok.extern.apachecommons.CommonsLog;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.WritableResource;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.UnsupportedMediaTypeException;

@Service
@CommonsLog
public class EmailTemplateServiceImpl implements EmailTemplateService {
  ResourceLoader resourceLoader;

  @Value("${spring.thymeleaf.prefix:classpath:/templates/}")
  private String thymeleafPrefix;

  @Value("${spring.thymeleaf.suffix:.html}")
  private String thymeleafSuffix;

  private final String defaultThymeleafPrefix = "classpath:/templates/";

  public EmailTemplateServiceImpl(ResourceLoader resourceLoader) {
    this.resourceLoader = resourceLoader;
  }

  @Override
  public String getNotificationTemplate(String templateName) {
    log.info("Getting notification template.");
    validateTemplateName(templateName);
    try {
      org.springframework.core.io.Resource resource =
          resourceLoader.getResource(thymeleafPrefix + templateName + thymeleafSuffix);
      return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      log.error("Failed to get notification template", e);
      throw new EntityNotFoundException(templateName);
    }
  }

  @Override
  public String updateNotificationTemplate(String templateName, String template) {
    log.info("Updating notification template.");
    if (thymeleafPrefix.equals(defaultThymeleafPrefix)) {
      throw new ForbiddenRequestException("Cannot update default templates");
    }
    validateTemplateName(templateName);
    String validateTemplate = validateHtml(template);
    String targetTemplatePath = thymeleafPrefix + templateName + thymeleafSuffix;

    org.springframework.core.io.Resource defaultTemplateResource =
        resourceLoader.getResource(targetTemplatePath);

    if (!defaultTemplateResource.exists()) {
      throw new ForbiddenRequestException(
          "Default template for " + targetTemplatePath + " does not exist");
    }

    writeTemplateToFile(targetTemplatePath, validateTemplate);
    return validateTemplate;
  }

  @Override
  public String resetNotificationTemplate(String templateName) {
    log.info("Resetting notification template.");
    if (thymeleafPrefix.equals(defaultThymeleafPrefix)) {
      throw new ForbiddenRequestException("Cannot update default templates");
    }
    validateTemplateName(templateName);
    String defaultTemplatePath = "classpath:/templates/" + templateName + thymeleafSuffix;
    org.springframework.core.io.Resource defaultTemplateResource =
        resourceLoader.getResource(defaultTemplatePath);

    if (!defaultTemplateResource.exists()) {
      throw new EntityNotFoundException(defaultTemplatePath);
    }
    try (InputStream inputStream = defaultTemplateResource.getInputStream()) {
      String defaultTemplate = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

      String targetTemplatePath = thymeleafPrefix + templateName + thymeleafSuffix;
      writeTemplateToFile(targetTemplatePath, defaultTemplate);

      return defaultTemplate;

    } catch (IOException e) {
      log.error("Failed to reset notification template", e);
      throw new RuntimeException("Failed to reset notification template", e);
    }
  }

  private void writeTemplateToFile(String targetTemplatePath, String templateContent) {
    try {
      org.springframework.core.io.Resource targetTemplateResource =
          resourceLoader.getResource(targetTemplatePath);

      if (!(targetTemplateResource instanceof WritableResource writable)) {
        throw new IOException("Template is not writable: " + targetTemplatePath);
      }

      try (OutputStream outputStream = writable.getOutputStream()) {
        outputStream.write(templateContent.getBytes(StandardCharsets.UTF_8));
      }
    } catch (IOException e) {
      log.error("Failed to write template to file", e);
      throw new RuntimeException("Failed to write template to file", e);
    }
  }

  private String validateHtml(String html) {
    try {
      return Jsoup.parse(html).html();
    } catch (Exception e) {
      throw new UnsupportedMediaTypeException("Template is not valid HTML: " + e.getMessage());
    }
  }

  private void validateTemplateName(String templateName) {
    if (templateName.contains("..") || templateName.contains("/") || templateName.contains("\\")) {
      throw new ForbiddenRequestException("Invalid template name");
    }
  }
}
