package eu.bbmri_eric.negotiator.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.bbmri_eric.negotiator.util.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class ThymeleafLoaderTest {

  @Autowired private TemplateRepository templateRepository;

  @Autowired private DatabaseTemplateLoader databaseTemplateLoader;

  @BeforeEach
  void setUp() {
    templateRepository.deleteAll();
  }

  @Test
  void run_whenDatabaseIsEmpty_loadsAllTemplatesFromClasspath() throws Exception {
    assertEquals(0, templateRepository.count());

    databaseTemplateLoader.run();

    var loadedTemplates = templateRepository.findAll();
    assertTrue(loadedTemplates.size() >= 4);

    var templateNames = loadedTemplates.stream().map(Template::getName).toList();

    assertTrue(templateNames.contains("email_body"));
    assertTrue(templateNames.contains("email_footer"));
    assertTrue(templateNames.contains("email_logo"));
    assertTrue(templateNames.contains("pdf_negotiation_summary"));

    for (Template template : loadedTemplates) {
      assertNotNull(template.getContent());
      assertFalse(template.getContent().trim().isEmpty());
      assertNotNull(template.getUpdatedAt());
    }
  }

  @Test
  void run_whenTemplatesAlreadyExist_doesNotCreateDuplicates() throws Exception {
    databaseTemplateLoader.run();
    int initialCount = templateRepository.findAll().size();
    assertTrue(initialCount > 0);

    databaseTemplateLoader.run();

    int finalCount = templateRepository.findAll().size();
    assertEquals(initialCount, finalCount);
  }

  @Test
  void run_whenSomeTemplatesExist_onlyLoadsMissing() throws Exception {
    var existingTemplate =
        Template.builder()
            .name("email_body")
            .html("<html><body>Existing content</body></html>")
            .build();
    templateRepository.save(existingTemplate);

    assertEquals(1, templateRepository.count());

    databaseTemplateLoader.run();

    var allTemplates = templateRepository.findAll();
    assertTrue(allTemplates.size() >= 4);

    var emailBodyTemplate = templateRepository.findByName("email_body");
    assertTrue(emailBodyTemplate.isPresent());
    assertEquals(
        "<html><body>Existing content</body></html>", emailBodyTemplate.get().getContent());
  }

  @Test
  void run_withCommandLineArguments_stillLoadsTemplates() throws Exception {
    assertEquals(0, templateRepository.count());

    databaseTemplateLoader.run("--some-arg", "--another-arg=value");

    assertTrue(templateRepository.count() >= 4);
  }

  private void assertNotNull(Object object) {
    org.junit.jupiter.api.Assertions.assertNotNull(object);
  }

  private void assertFalse(boolean condition) {
    org.junit.jupiter.api.Assertions.assertFalse(condition);
  }
}
