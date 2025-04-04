package eu.bbmri_eric.negotiator.unit.repository;

import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.common.exceptions.ForbiddenRequestException;
import eu.bbmri_eric.negotiator.notification.email.FilesystemEmailTemplateRepository;
import eu.bbmri_eric.negotiator.util.RepositoryTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.WritableResource;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

@RepositoryTest(loadTestData = true)
public class EmailTemplateRepositoryTest {
    @Mock
    private ResourceLoader resourceLoader;

    @Mock
    private WritableResource writableResource;

    @Mock
    private Resource resource;

    @InjectMocks
    private FilesystemEmailTemplateRepository repository;

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
        when(resourceLoader.getResource(thymeleafPrefix + templateName + thymeleafSuffix)).thenReturn(resource);
        when(resource.getInputStream()).thenReturn(new java.io.ByteArrayInputStream(templateContent.getBytes(StandardCharsets.UTF_8)));

        String result = repository.load(templateName);

        assertEquals(templateContent, result);
    }

    @Test
    void load_throwsEntityNotFoundException_whenTemplateDoesNotExist() throws IOException {
        String templateName = "nonexistent";
        when(resourceLoader.getResource(thymeleafPrefix + templateName + thymeleafSuffix)).thenReturn(resource);
        when(resource.getInputStream()).thenThrow(new IOException("Template not found"));

        assertThrows(EntityNotFoundException.class, () -> repository.load(templateName));
    }

    @Test
    void save_throwsForbiddenRequestException_whenUpdatingDefaultTemplates() {
        repository.setThymeleafPrefix("classpath:/templates/");
        String templateName = "template";
        String templateContent = "<html><body>Template</body></html>";

        assertThrows(ForbiddenRequestException.class, () -> repository.save(templateName, templateContent));
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

        assertThrows(ForbiddenRequestException.class, () -> repository.save(templateName, templateContent));
    }

    @Test
    void reset_resetsTemplateToDefault_whenTemplateExists() throws IOException {
        String templateName = "footer";
        String defaultTemplateContent = "<html><body>Default Template</body></html>";
        String defaultTemplatePath = "classpath:/templates/" + templateName + ".html";
        String targetTemplatePath = thymeleafPrefix + templateName + thymeleafSuffix;

        when(resourceLoader.getResource(defaultTemplatePath)).thenReturn(resource);
        when(resource.exists()).thenReturn(true);
        when(resource.getInputStream()).thenReturn(new java.io.ByteArrayInputStream(defaultTemplateContent.getBytes(StandardCharsets.UTF_8)));
        when(resourceLoader.getResource(targetTemplatePath)).thenReturn(writableResource);
        OutputStream outputStream = mock(OutputStream.class);
        when(writableResource.getOutputStream()).thenReturn(outputStream);

        repository.reset(templateName);

        verify(outputStream).write(defaultTemplateContent.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void reset_throwsEntityNotFoundException_whenDefaultTemplateDoesNotExist() throws IOException{
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
}
