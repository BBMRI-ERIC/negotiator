package eu.bbmri_eric.negotiator.template;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * CommandLineRunner that automatically loads all HTML template files from classpath:/templates/
 * into the database on application startup if they don't already exist. This enables migration from
 * file-based templates to database-stored templates while maintaining Thymeleaf composition
 * functionality.
 */
@Component
@CommonsLog
class DatabaseTemplateLoader implements CommandLineRunner {

  private final TemplateRepository templateRepository;
  private final PathMatchingResourcePatternResolver resourceResolver;

  DatabaseTemplateLoader(TemplateRepository templateRepository) {
    this.templateRepository = templateRepository;
    this.resourceResolver = new PathMatchingResourcePatternResolver();
  }

  @Override
  @Transactional
  public void run(String... args) throws Exception {
    log.info("Starting database template loading process...");

    var templateResources = loadTemplateResources();

    int importedCount = 0;
    int skippedCount = 0;

    for (Resource templateResource : templateResources) {
      var templateName = extractTemplateName(templateResource.getFilename());

      if (templateRepository.existsByName(templateName)) {
        log.debug(
            "Template '%s' already exists in database, skipping import".formatted(templateName));
        skippedCount++;
        continue;
      }

      try {
        var htmlContent = loadTemplateContent(templateResource);

        var template = Template.builder().name(templateName).content(htmlContent).build();
        templateRepository.save(template);
        log.info(
            "Successfully imported template '%s' into database from '%s'"
                .formatted(templateName, templateResource.getFilename()));
        importedCount++;
      } catch (IOException e) {
        log.error(
            "Failed to load template file '%s': %s"
                .formatted(templateResource.getFilename(), e.getMessage()));
      } catch (Exception e) {
        log.error(
            "Failed to save template '%s' to database: %s".formatted(templateName, e.getMessage()));
      }
    }

    log.info(
        "Database template loading completed. Imported: %d, Skipped: %d"
            .formatted(importedCount, skippedCount));
  }

  /**
   * Loads all HTML template resources from the classpath:/templates/ directory.
   *
   * @return array of template resources
   * @throws IOException if unable to load resources
   */
  private Resource[] loadTemplateResources() throws IOException {
    var templateResources = resourceResolver.getResources("classpath:/templates/*.html");
    log.debug(
        "Found %d template files in classpath:/templates/".formatted(templateResources.length));
    return templateResources;
  }

  /**
   * Extracts template name from filename by removing the .html extension while preserving
   * the original case.
   *
   * @param filename the template filename (e.g., "EMAIL_BODY.html")
   * @return the template name (e.g., "EMAIL_BODY")
   */
  private String extractTemplateName(String filename) {
    if (filename == null) {
      throw new IllegalArgumentException("Template filename cannot be null");
    }
    return filename.replaceFirst("\\.html$", "");
  }

  /**
   * Loads the HTML content from a template resource.
   *
   * @param templateResource the template resource
   * @return the HTML content as string
   * @throws IOException if unable to read the resource
   */
  private String loadTemplateContent(Resource templateResource) throws IOException {
    if (!templateResource.exists()) {
      throw new IOException("Template resource does not exist: " + templateResource.getFilename());
    }

    return templateResource.getContentAsString(StandardCharsets.UTF_8);
  }
}
