package eu.bbmri_eric.negotiator.template;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@CommonsLog
class TemplateServiceImpl implements TemplateService {

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
      log.info("Updated template '%s'".formatted(templateName));
      return templateEntity.getContent();
    } else {
      var newTemplate = Template.builder().name(templateName).content(template).build();
      templateRepository.save(newTemplate);
      log.info("Created new template '%s'".formatted(templateName));
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

      log.info("Reset template '%s' to original content".formatted(templateName));
      return templateEntity.getContent();

    } catch (IOException e) {
      log.error("Failed to load original content for template '%s': %s".formatted(templateName, e.getMessage()));
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
