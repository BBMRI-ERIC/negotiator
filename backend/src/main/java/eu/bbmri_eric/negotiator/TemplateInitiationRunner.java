package eu.bbmri_eric.negotiator;

import eu.bbmri_eric.negotiator.template.TemplateService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

@Component
public class TemplateInitiationRunner implements CommandLineRunner {

  private final TemplateService templateService;

  @Value("${spring.thymeleaf.prefix:classpath:/templates/}")
  private String thymeleafPrefix;

  @Value("${spring.thymeleaf.suffix:.html}")
  private String thymeleafSuffix;

  private final String defaultTemplatePath = "classpath:/templates/";

  @Autowired
  public TemplateInitiationRunner(TemplateService templateService) {
    this.templateService = templateService;
  }

  @Override
  public void run(String... args) throws Exception {
    if (isDefaultTemplatePathUsed()) {
      return;
    }
    Path templatesDir = resolveTemplateDirectory();
    createDirectoryIfNotExists(templatesDir);
    initializeTemplates(templatesDir);
  }

  private boolean isDefaultTemplatePathUsed() {
    Path normalizedThymeleafPrefix = Paths.get(thymeleafPrefix).toAbsolutePath().normalize();
    Path normalizedDefaultTemplatePath =
        Paths.get(defaultTemplatePath).toAbsolutePath().normalize();
    return normalizedThymeleafPrefix.equals(normalizedDefaultTemplatePath);
  }

  private Path resolveTemplateDirectory() {
    String cleanedPrefix = thymeleafPrefix.replaceFirst("^file:(//)?", "");
    return Paths.get(cleanedPrefix).toAbsolutePath().normalize();
  }

  private void createDirectoryIfNotExists(Path path) throws IOException {
    if (!Files.exists(path)) {
      Files.createDirectories(path);
    }
  }

  private void initializeTemplates(Path templatesDir) throws IOException {
    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    Resource[] resources = resolver.getResources("classpath:/templates/*" + thymeleafSuffix);

    for (Resource resource : resources) {
      String filename = resource.getFilename(); // e.g. email-footer.html
      if (filename == null) continue;

      Path fileOnDisk = templatesDir.resolve(filename);
      if (!Files.exists(fileOnDisk)) {
        String templateName = filename.replace(thymeleafSuffix, "");
        templateService.resetNotificationTemplate(templateName);
      }
    }
  }
}
