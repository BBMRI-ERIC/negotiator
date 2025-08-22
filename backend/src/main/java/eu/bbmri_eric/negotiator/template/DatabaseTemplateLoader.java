package eu.bbmri_eric.negotiator.template;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
class DatabaseTemplateLoader implements CommandLineRunner {

  private static final Logger logger = LoggerFactory.getLogger(DatabaseTemplateLoader.class);

  private final TemplateRepository templateRepository;
  private final PathMatchingResourcePatternResolver resourceResolver;

  DatabaseTemplateLoader(TemplateRepository templateRepository) {
    this.templateRepository = templateRepository;
    this.resourceResolver = new PathMatchingResourcePatternResolver();
  }

  @Override
  @Transactional
  public void run(String... args) throws Exception {
    logger.info("Starting database template loading process...");

    var templateResources = loadTemplateResources();

    int importedCount = 0;
    int skippedCount = 0;

    for (Resource templateResource : templateResources) {
      var templateName = extractTemplateName(templateResource.getFilename());

      if (templateRepository.existsByName(templateName)) {
        logger.debug("Template '{}' already exists in database, skipping import", templateName);
        skippedCount++;
        continue;
      }

      try {
        var htmlContent = loadTemplateContent(templateResource);

        var template = Template.builder().name(templateName).html(htmlContent).build();
        templateRepository.save(template);
        logger.info(
            "Successfully imported template '{}' into database from '{}'",
            templateName,
            templateResource.getFilename());
        importedCount++;
      } catch (IOException e) {
        logger.error(
            "Failed to load template file '{}': {}",
            templateResource.getFilename(),
            e.getMessage());
      } catch (Exception e) {
        logger.error("Failed to save template '{}' to database: {}", templateName, e.getMessage());
      }
    }

    logger.info(
        "Database template loading completed. Imported: {}, Skipped: {}",
        importedCount,
        skippedCount);
  }

  /**
   * Loads all HTML template resources from the classpath:/templates/ directory.
   *
   * @return array of template resources
   * @throws IOException if unable to load resources
   */
  private Resource[] loadTemplateResources() throws IOException {
    var templateResources = resourceResolver.getResources("classpath:/templates/*.html");
    logger.debug("Found {} template files in classpath:/templates/", templateResources.length);
    return templateResources;
  }

  /**
   * Extracts template name from filename by removing the .html extension and converting to
   * lowercase for consistency.
   *
   * @param filename the template filename (e.g., "EMAIL_BODY.html")
   * @return the template name (e.g., "email_body")
   */
  private String extractTemplateName(String filename) {
    if (filename == null) {
      throw new IllegalArgumentException("Template filename cannot be null");
    }
    return filename.replaceFirst("\\.html$", "").toLowerCase();
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
