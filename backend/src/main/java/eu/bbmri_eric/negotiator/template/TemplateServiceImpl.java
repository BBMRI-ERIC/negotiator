package eu.bbmri_eric.negotiator.template;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@CommonsLog
public class TemplateServiceImpl implements TemplateService {

  private final TemplateRepository templateRepository;
  private final TemplateEngine templateEngine;

  TemplateServiceImpl(TemplateRepository templateRepository, TemplateEngine templateEngine) {
    this.templateRepository = templateRepository;
    this.templateEngine = templateEngine;
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
      throw new IllegalArgumentException("Template not found: " + templateName);
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
      throw new NullPointerException("Original template not found: " + templateName);
    }
  }

  @Override
  public String processTemplate(Map<String, Object> variables, String templateName) {
    if (templateName == null || templateName.isBlank()) {
      throw new IllegalArgumentException("Template name cannot be null or blank");
    }
    templateRepository
        .findByName(templateName)
        .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateName));
    try {
      var context = new Context();
      if (variables != null && !variables.isEmpty()) {
        context.setVariables(variables);
      }
      var processedTemplate = templateEngine.process(templateName, context);
      log.debug(
          "Successfully processed template '%s' with %d variables"
              .formatted(templateName, variables != null ? variables.size() : 0));
      return processedTemplate;
    } catch (Exception e) {
      log.error("Failed to process template '%s': %s".formatted(templateName, e.getMessage()), e);
      throw new RuntimeException("Failed to process template '%s'".formatted(templateName), e);
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
