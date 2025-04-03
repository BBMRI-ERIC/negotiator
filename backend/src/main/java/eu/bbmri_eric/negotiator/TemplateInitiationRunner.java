package eu.bbmri_eric.negotiator;

import eu.bbmri_eric.negotiator.notification.email.EmailTemplateService;
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

  private final EmailTemplateService emailTemplateService;

  @Value("${spring.thymeleaf.prefix:classpath:/templates/}")
  private String thymeleafPrefix;

  @Value("${spring.thymeleaf.suffix:.html}")
  private String thymeleafSuffix;

  private final String defaultTemplatePath = "classpath:/templates/";

  @Autowired
  public TemplateInitiationRunner(EmailTemplateService emailTemplateService) {
    this.emailTemplateService = emailTemplateService;
  }

  @Override
  public void run(String... args) throws Exception {
    Path normalizedThymeleafPrefix = Paths.get(thymeleafPrefix).toAbsolutePath().normalize();
    Path normalizedDefaultTemplatePath =
        Paths.get(defaultTemplatePath).toAbsolutePath().normalize();
    if (normalizedThymeleafPrefix.equals(normalizedDefaultTemplatePath)) {
      return;
    }
    String cleanedPrefix = thymeleafPrefix.replaceFirst("^file:(//)?", "");
    Path templatesDir = Paths.get(cleanedPrefix).toAbsolutePath().normalize();
    try {
      if (!Files.exists(templatesDir)) {
        Files.createDirectories(templatesDir);
      }
    } catch (IOException aE) {
      throw new RuntimeException("Could not create directory for templates: " + templatesDir, aE);
    }
    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    Resource[] resources = resolver.getResources("classpath:/templates/*" + thymeleafSuffix);

    for (Resource resource : resources) {
      String filename = resource.getFilename(); // e.g. footer.html
      if (filename == null) continue;

      Path fileOnDisk = templatesDir.resolve(filename);
      if (!Files.exists(fileOnDisk)) {
        String templateName = filename.replace(thymeleafSuffix, "");
        emailTemplateService.resetNotificationTemplate(templateName);
      }
    }
  }
}
