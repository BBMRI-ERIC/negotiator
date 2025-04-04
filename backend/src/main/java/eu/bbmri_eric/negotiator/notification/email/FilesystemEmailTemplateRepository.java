package eu.bbmri_eric.negotiator.notification.email;

import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.common.exceptions.ForbiddenRequestException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.WritableResource;
import org.springframework.stereotype.Repository;
import org.testcontainers.shaded.com.google.common.annotations.VisibleForTesting;

@CommonsLog
@Repository
public class FilesystemEmailTemplateRepository implements EmailTemplateRepository {

  ResourceLoader resourceLoader;

  @Value("${spring.thymeleaf.prefix:classpath:/templates/}")
  private String thymeleafPrefix;

  @Value("${spring.thymeleaf.suffix:.html}")
  private String thymeleafSuffix;

  private final String defaultThymeleafPrefix = "classpath:/templates/";

  public FilesystemEmailTemplateRepository(ResourceLoader resourceLoader) {
    this.resourceLoader = resourceLoader;
  }

  @Override
  public String load(String templateName) {
    log.debug("Loading notification template: " + templateName);
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
  public void save(String templateName, String template) {
    log.debug("Saving notification template: " + templateName);
    if (thymeleafPrefix.equals(defaultThymeleafPrefix)) {
      throw new ForbiddenRequestException("Cannot update default templates");
    }
    validateTemplateName(templateName);
    String targetTemplatePath = thymeleafPrefix + templateName + thymeleafSuffix;

    org.springframework.core.io.Resource defaultTemplateResource =
        resourceLoader.getResource(targetTemplatePath);

    if (!defaultTemplateResource.exists()) {
      throw new ForbiddenRequestException(
          "Default template for " + targetTemplatePath + " does not exist");
    }

    writeTemplateToFile(targetTemplatePath, template);
  }

  @Override
  public void reset(String templateName) {
    log.debug("Resetting notification template: " + templateName);
    if (thymeleafPrefix.equals(defaultThymeleafPrefix)) {
      throw new ForbiddenRequestException("Cannot update default templates");
    }
    validateTemplateName(templateName);
    String defaultTemplatePath = defaultThymeleafPrefix + templateName + thymeleafSuffix;
    org.springframework.core.io.Resource defaultTemplateResource =
        resourceLoader.getResource(defaultTemplatePath);

    if (!defaultTemplateResource.exists()) {
      throw new EntityNotFoundException(defaultTemplatePath);
    }
    try (InputStream inputStream = defaultTemplateResource.getInputStream()) {
      String defaultTemplate = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

      String targetTemplatePath = thymeleafPrefix + templateName + thymeleafSuffix;
      writeTemplateToFile(targetTemplatePath, defaultTemplate);
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

  private void validateTemplateName(String templateName) {
    if (templateName == null || templateName.isEmpty()) {
      throw new ForbiddenRequestException("Template name cannot be null or empty");
    } else if (templateName.contains("..")
        || templateName.contains("/")
        || templateName.contains("\\")) {
      throw new ForbiddenRequestException("Invalid template name");
    }
  }

  @VisibleForTesting
  public void setThymeleafPrefix(String thymeleafPrefix) {
    this.thymeleafPrefix = thymeleafPrefix;
  }

  @VisibleForTesting
  public void setThymeleafSuffix(String thymeleafSuffix) {
    this.thymeleafSuffix = thymeleafSuffix;
  }
}
