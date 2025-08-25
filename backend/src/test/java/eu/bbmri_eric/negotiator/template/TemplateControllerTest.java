package eu.bbmri_eric.negotiator.template;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
@AutoConfigureMockMvc
class TemplateControllerTest {

  private static final String TEMPLATES_ENDPOINT = "/v3/templates";

  @Autowired private TemplateRepository templateRepository;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    templateRepository.deleteAll();
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getAllNotificationTemplates_returnsTemplateList() throws Exception {
    createTestTemplates();

    mockMvc
        .perform(get(TEMPLATES_ENDPOINT))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(3))
        .andExpect(jsonPath("$[0]").value("email_body"))
        .andExpect(jsonPath("$[1]").value("email_footer"))
        .andExpect(jsonPath("$[2]").value("email_logo"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getAllNotificationTemplates_whenNoTemplates_returnsEmptyList() throws Exception {
    mockMvc
        .perform(get(TEMPLATES_ENDPOINT))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getNotificationTemplate_whenTemplateExists_returnsTemplateContent() throws Exception {
    var templateContent = "<html><body>Test template</body></html>";
    var template = Template.builder().name("email_body").content(templateContent).build();
    templateRepository.save(template);

    mockMvc
        .perform(get(TEMPLATES_ENDPOINT + "/email_body"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/xhtml+xml;charset=UTF-8"))
        .andExpect(content().string(templateContent));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getNotificationTemplate_whenTemplateNotFound_returnsEmptyString() throws Exception {
    mockMvc
        .perform(get(TEMPLATES_ENDPOINT + "/nonexistent"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/xhtml+xml;charset=UTF-8"))
        .andExpect(content().string(""));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void updateNotificationTemplate_withValidContent_returnsUpdatedContent() throws Exception {
    var originalContent = "<html><body>Original template</body></html>";
    var newContent = "<html><body>Updated template</body></html>";
    var template = Template.builder().name("email_body").content(originalContent).build();
    templateRepository.save(template);

    mockMvc
        .perform(
            put(TEMPLATES_ENDPOINT + "/email_body")
                .contentType(MediaType.TEXT_PLAIN)
                .content(newContent))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/xhtml+xml;charset=UTF-8"))
        .andExpect(content().string(newContent));

    var updatedTemplate = templateRepository.findByName("email_body");
    assertTrue(updatedTemplate.isPresent());
    assertEquals(newContent, updatedTemplate.get().getContent());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void updateNotificationTemplate_createsNewTemplate_whenNotExists() throws Exception {
    var newContent = "<html><body>New template</body></html>";

    mockMvc
        .perform(
            put(TEMPLATES_ENDPOINT + "/new_template")
                .contentType(MediaType.TEXT_PLAIN)
                .content(newContent))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/xhtml+xml;charset=UTF-8"))
        .andExpect(content().string(newContent));

    var createdTemplate = templateRepository.findByName("new_template");
    assertTrue(createdTemplate.isPresent());
    assertEquals(newContent, createdTemplate.get().getContent());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void resetNotificationTemplate_withResetOperation_returnsOriginalContent() throws Exception {
    var modifiedContent = "<html><body>Modified template</body></html>";
    var template = Template.builder().name("email").content(modifiedContent).build();
    templateRepository.save(template);

    var request = new TemplateOperationRequest();
    request.setOperation(TemplateOperationRequest.Operation.RESET);

    mockMvc
        .perform(
            post(TEMPLATES_ENDPOINT + "/email/operations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/xhtml+xml;charset=UTF-8"));

    var resetTemplate = templateRepository.findByName("email");
    assertTrue(resetTemplate.isPresent());
    assertNotEquals(modifiedContent, resetTemplate.get().getContent());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void resetNotificationTemplate_withInvalidOperation_returnsBadRequest() throws Exception {
    var template =
        Template.builder().name("email_body").content("<html><body>Test</body></html>").build();
    templateRepository.save(template);

    var request = new TemplateOperationRequest();
    request.setOperation(null);

    mockMvc
        .perform(
            post(TEMPLATES_ENDPOINT + "/email_body/operations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getAllNotificationTemplates_withoutAuthentication_returnsUnauthorized() throws Exception {
    mockMvc.perform(get(TEMPLATES_ENDPOINT)).andExpect(status().isUnauthorized());
  }

  @Test
  void updateNotificationTemplate_withoutAuthentication_returnsUnauthorized() throws Exception {
    mockMvc
        .perform(
            put(TEMPLATES_ENDPOINT + "/email_body")
                .contentType(MediaType.TEXT_PLAIN)
                .content("<html>test</html>"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void resetNotificationTemplate_withoutAuthentication_returnsUnauthorized() throws Exception {
    var request = new TemplateOperationRequest();
    request.setOperation(TemplateOperationRequest.Operation.RESET);

    mockMvc
        .perform(
            post(TEMPLATES_ENDPOINT + "/email_body/operations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized());
  }

  private void createTestTemplates() {
    var template1 =
        Template.builder()
            .name("email_body")
            .content("<html><body>Body template</body></html>")
            .build();
    var template2 =
        Template.builder()
            .name("email_footer")
            .content("<html><body>Footer template</body></html>")
            .build();
    var template3 =
        Template.builder()
            .name("email_logo")
            .content("<html><body>Logo template</body></html>")
            .build();

    templateRepository.save(template1);
    templateRepository.save(template2);
    templateRepository.save(template3);
  }

  private void assertTrue(boolean condition) {
    org.junit.jupiter.api.Assertions.assertTrue(condition);
  }

  private void assertEquals(Object expected, Object actual) {
    org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
  }

  private void assertNotEquals(Object unexpected, Object actual) {
    org.junit.jupiter.api.Assertions.assertNotEquals(unexpected, actual);
  }
}
