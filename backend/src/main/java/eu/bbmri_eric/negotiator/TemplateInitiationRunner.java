package eu.bbmri_eric.negotiator;


import eu.bbmri_eric.negotiator.notification.UserNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

@Component
public class TemplateInitiationRunner implements CommandLineRunner {

    private final UserNotificationService userNotificationService;
    @Value("${spring.thymeleaf.prefix:classpath:/templates/}")
    private String thymeleafPrefix;

    @Value("${spring.thymeleaf.suffix:.html}")
    private String thymeleafSuffix;

    @Autowired
    public TemplateInitiationRunner(UserNotificationService userNotificationService) {
        this.userNotificationService = userNotificationService;
    }

    @Override
    public void run(String... args) throws Exception {
        String cleanedPrefix = thymeleafPrefix.replaceFirst("^file:(//)?", "");
        Path templatesDir = Paths.get(cleanedPrefix).toAbsolutePath().normalize();
        try (Stream<Path> paths = Files.walk(templatesDir)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(thymeleafSuffix + ".default"))
                    .forEach(path -> resetTemplateIfNotExists(path.getFileName().toString().replace(".default", "")));
        }
    }

    private void resetTemplateIfNotExists(String templateFile) {
        if (!Files.exists(Paths.get(thymeleafPrefix+templateFile))) {
            userNotificationService.resetNotificationTemplate(templateFile.replace(thymeleafSuffix,""));
        }
    }
}