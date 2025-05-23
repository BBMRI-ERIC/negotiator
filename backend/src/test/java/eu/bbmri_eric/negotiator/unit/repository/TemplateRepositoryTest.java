package eu.bbmri_eric.negotiator.unit.repository;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.common.exceptions.ForbiddenRequestException;
import eu.bbmri_eric.negotiator.template.FilesystemTemplateRepository;
import eu.bbmri_eric.negotiator.util.RepositoryTest;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.WritableResource;
import org.springframework.core.io.support.ResourcePatternResolver;

@RepositoryTest(loadTestData = true)
public class TemplateRepositoryTest {
  @Mock private ResourceLoader resourceLoader;

  @Mock private ResourcePatternResolver resourcePatResolver;

  @Mock private WritableResource writableResource;

  @Mock private Resource resource;

  @InjectMocks private FilesystemTemplateRepository repository;

  private final String thymeleafPrefix = "file:/templates/";

  private final String thymeleafSuffix = ".html";

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    repository.setThymeleafPrefix(thymeleafPrefix);
    repository.setThymeleafSuffix(thymeleafSuffix);
  }

  @Test
  void load_returnsTemplateContent_whenTemplateExists() throws IOException {
    String templateName = "template";
    String templateContent = "<html><body>Template</body></html>";
    when(resourceLoader.getResource(thymeleafPrefix + templateName + thymeleafSuffix))
        .thenReturn(resource);
    when(resource.getInputStream())
        .thenReturn(
            new java.io.ByteArrayInputStream(templateContent.getBytes(StandardCharsets.UTF_8)));

    String result = repository.load(templateName);

    assertEquals(templateContent, result);
  }

  @Test
  void load_throwsEntityNotFoundException_whenTemplateDoesNotExist() throws IOException {
    String templateName = "nonexistent";
    when(resourceLoader.getResource(thymeleafPrefix + templateName + thymeleafSuffix))
        .thenReturn(resource);
    when(resource.getInputStream()).thenThrow(new IOException("Template not found"));

    assertThrows(EntityNotFoundException.class, () -> repository.load(templateName));
  }

  @Test
  void save_throwsForbiddenRequestException_whenUpdatingDefaultTemplates() {
    repository.setThymeleafPrefix("classpath:/templates/");
    String templateName = "template";
    String templateContent = "<html><body>Template</body></html>";

    assertThrows(
        ForbiddenRequestException.class, () -> repository.save(templateName, templateContent));
  }

  @Test
  void save_writesTemplateToFile_whenTemplateIsValid() throws IOException {
    String templateName = "template";
    String templateContent = "<html><body>Template</body></html>";
    String targetTemplatePath = thymeleafPrefix + templateName + thymeleafSuffix;

    when(resourceLoader.getResource(targetTemplatePath)).thenReturn(writableResource);
    when(writableResource.exists()).thenReturn(true);
    OutputStream outputStream = mock(OutputStream.class);
    when(writableResource.getOutputStream()).thenReturn(outputStream);

    repository.save(templateName, templateContent);

    verify(outputStream).write(templateContent.getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void save_throwsForbiddenRequestException_whenDefaultTemplateDoesNotExist() {
    repository.setThymeleafPrefix("classpath:/templates/");
    String templateName = "template";
    String templateContent = "<html><body>Template</body></html>";
    String targetTemplatePath = "file:/templates/" + templateName + ".html";

    when(resourceLoader.getResource(targetTemplatePath)).thenReturn(writableResource);
    when(writableResource.exists()).thenReturn(false);

    assertThrows(
        ForbiddenRequestException.class, () -> repository.save(templateName, templateContent));
  }

  @Test
  void reset_resetsTemplateToDefault_whenTemplateExists() throws IOException {
    String templateName = "footer";
    String defaultTemplateContent = "<html><body>Default Template</body></html>";
    String defaultTemplatePath = "classpath:/templates/" + templateName + ".html";
    String targetTemplatePath = thymeleafPrefix + templateName + thymeleafSuffix;

    when(resourceLoader.getResource(defaultTemplatePath)).thenReturn(resource);
    when(resource.exists()).thenReturn(true);
    when(resource.getInputStream())
        .thenReturn(
            new java.io.ByteArrayInputStream(
                defaultTemplateContent.getBytes(StandardCharsets.UTF_8)));
    when(resourceLoader.getResource(targetTemplatePath)).thenReturn(writableResource);
    OutputStream outputStream = mock(OutputStream.class);
    when(writableResource.getOutputStream()).thenReturn(outputStream);

    repository.reset(templateName);

    verify(outputStream).write(defaultTemplateContent.getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void reset_throwsEntityNotFoundException_whenDefaultTemplateDoesNotExist() throws IOException {
    String templateName = "nonexistent";
    String defaultTemplatePath = "classpath:/templates/" + templateName + ".html";

    when(resourceLoader.getResource(defaultTemplatePath)).thenReturn(resource);
    when(resource.exists()).thenReturn(false);
    when(resource.getInputStream()).thenThrow(new IOException("Template not found"));

    assertThrows(EntityNotFoundException.class, () -> repository.reset(templateName));
  }

  @Test
  void reset_throwsForbiddenRequestException_whenUpdatingDefaultTemplates() {
    repository.setThymeleafPrefix("classpath:/templates/");
    String templateName = "template";

    assertThrows(ForbiddenRequestException.class, () -> repository.reset(templateName));
  }

  @Test
  void listAll_ReturnsAllTemplates() throws IOException {
    org.springframework.core.io.Resource resource1 =
        mock(org.springframework.core.io.Resource.class);
    org.springframework.core.io.Resource resource2 =
        mock(org.springframework.core.io.Resource.class);
    when(resource1.getFilename()).thenReturn("template1.html");
    when(resource2.getFilename()).thenReturn("template2.html");
    when(resourcePatResolver.getResources(anyString()))
        .thenReturn(new org.springframework.core.io.Resource[] {resource1, resource2});

    List<String> templates = repository.listAll();

    assertEquals(2, templates.size());
    assertTrue(templates.contains("template1"));
    assertTrue(templates.contains("template2"));
  }

  @Test
  void listAll_ThrowsEntityNotFoundException_WhenIOExceptionOccurs() throws IOException {
    when(resourcePatResolver.getResources(anyString())).thenThrow(new IOException());

    assertThrows(EntityNotFoundException.class, () -> repository.listAll());
  }

  @Test
  void listAll_ReturnsEmptyList_WhenNoTemplatesFound() throws IOException {
    when(resourcePatResolver.getResources(anyString()))
        .thenReturn(new org.springframework.core.io.Resource[] {});

    List<String> templates = repository.listAll();

    assertTrue(templates.isEmpty());
  }
}
