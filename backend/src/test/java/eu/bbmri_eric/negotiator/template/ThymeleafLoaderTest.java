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

    assertTrue(templateNames.contains("EMAIL"));
    assertTrue(templateNames.contains("EMAIL_FOOTER"));
    assertTrue(templateNames.contains("LOGO"));
    assertTrue(templateNames.contains("PDF_NEGOTIATION_SUMMARY"));

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
            .content("<html><body>Existing content</body></html>")
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

  @Test
  void run_whenTemplateIsCustomized_doesNotOverrideContent() throws Exception {
    var customizedTemplate =
        Template.builder()
            .name("EMAIL")
            .content("<html><body>Customized content</body></html>")
            .isCustomized(true)
            .build();
    templateRepository.save(customizedTemplate);

    databaseTemplateLoader.run();

    var templateOpt = templateRepository.findByName("EMAIL");
    assertTrue(templateOpt.isPresent());
    assertEquals("<html><body>Customized content</body></html>", templateOpt.get().getContent());
  }

  @Test
  void run_whenTemplateIsNotCustomized_overridesContent() throws Exception {
    var defaultTemplate =
        Template.builder()
            .name("EMAIL")
            .content("<html><body>Old default content</body></html>")
            .isCustomized(false)
            .build();
    templateRepository.save(defaultTemplate);

    databaseTemplateLoader.run();

    var templateOpt = templateRepository.findByName("EMAIL");
    assertTrue(templateOpt.isPresent());
    assertFalse(
        "<html><body>Old default content</body></html>".equals(templateOpt.get().getContent()));
    assertNotNull(templateOpt.get().getContent());
    assertFalse(templateOpt.get().getContent().trim().isEmpty());
  }

  private void assertNotNull(Object object) {
    org.junit.jupiter.api.Assertions.assertNotNull(object);
  }

  private void assertFalse(boolean condition) {
    org.junit.jupiter.api.Assertions.assertFalse(condition);
  }
}
