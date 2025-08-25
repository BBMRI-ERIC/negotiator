package eu.bbmri_eric.negotiator.template;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class TemplateServiceImpl implements TemplateService {

  private static final Logger logger = LoggerFactory.getLogger(TemplateServiceImpl.class);

  private final TemplateRepository templateRepository;

  TemplateServiceImpl(TemplateRepository templateRepository) {
    this.templateRepository = templateRepository;
  }

  @Override
  public List<String> getAll() {
    return templateRepository.findAll().stream().map(Template::getName).toList();
  }

  @Override
  public String getByName(String templateName) {
    return templateRepository.findByName(templateName).map(Template::getContent).orElse("");
  }

  @Override
  @Transactional
  public String updateTemplate(String templateName, String template) {
    var existingTemplate = templateRepository.findByName(templateName);

    if (existingTemplate.isPresent()) {
      var templateEntity = existingTemplate.get();
      templateEntity.setContent(template);
      templateRepository.save(templateEntity);
      logger.info("Updated template '{}'", templateName);
      return templateEntity.getContent();
    } else {
      var newTemplate = Template.builder().name(templateName).content(template).build();
      templateRepository.save(newTemplate);
      logger.info("Created new template '{}'", templateName);
      return newTemplate.getContent();
    }
  }

  @Override
  @Transactional
  public String resetTemplate(String templateName) {
    var existingTemplate = templateRepository.findByName(templateName);
    if (existingTemplate.isEmpty()) {
      throw new IllegalArgumentException("%s template not found".formatted(templateName));
    }

    try {
      var originalContent = loadOriginalTemplateContent(templateName);
      var templateEntity = existingTemplate.get();
      templateEntity.setContent(originalContent);
      templateRepository.save(templateEntity);

      logger.info("Reset template '{}' to original content", templateName);
      return templateEntity.getContent();

    } catch (IOException e) {
      logger.error(
          "Failed to load original content for template '{}': {}", templateName, e.getMessage());
      return existingTemplate.get().getContent();
    }
  }

  private String loadOriginalTemplateContent(String templateName) throws IOException {
    var resourcePath = "templates/" + templateName.toUpperCase() + ".html";
    var resource = new ClassPathResource(resourcePath);

    if (!resource.exists()) {
      throw new IOException("Original template file not found: " + resourcePath);
    }

    return resource.getContentAsString(StandardCharsets.UTF_8);
  }
}
