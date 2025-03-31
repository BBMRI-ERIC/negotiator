package eu.bbmri_eric.negotiator.integration.api;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import eu.bbmri_eric.negotiator.webhook.Webhook;
import eu.bbmri_eric.negotiator.webhook.WebhookCreateDTO;
import eu.bbmri_eric.negotiator.webhook.WebhookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@IntegrationTest
public class WebhookControllerTest {

  private MockMvc mockMvc;

  @Autowired private WebhookRepository webhookRepository;

  @Autowired private WebApplicationContext context;

  @Autowired private ObjectMapper objectMapper;

  @BeforeEach
  public void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    webhookRepository.deleteAll();
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getWebhookById_validId_ok() throws Exception {
    Webhook webhook =
        webhookRepository.save(new Webhook("https://example.com/webhook", true, true));

    mockMvc
        .perform(
            MockMvcRequestBuilders.get(String.format("/v3/webhooks/%d", webhook.getId()))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(webhook.getId().intValue())))
        .andExpect(jsonPath("$.url", is(webhook.getUrl())));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getAllWebhooks_valid_ok() throws Exception {
    webhookRepository.save(new Webhook("https://example.com/webhook1", true, true));
    webhookRepository.save(new Webhook("https://example.com/webhook2", true, true));

    mockMvc
        .perform(MockMvcRequestBuilders.get("/v3/webhooks").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.webhookResponseDTOList", hasSize(2)));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createWebhook_valid_ok() throws Exception {
    WebhookCreateDTO createDTO = new WebhookCreateDTO("https://example.com/webhook", true, true);
    String json = objectMapper.writeValueAsString(createDTO);

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/v3/webhooks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.url", is("https://example.com/webhook")));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void updateWebhook_valid_ok() throws Exception {
    Webhook webhook =
        webhookRepository.save(new Webhook("https://example.com/webhook", true, true));
    WebhookCreateDTO updateDTO =
        new WebhookCreateDTO("https://example.com/webhook-updated", true, true);
    String json = objectMapper.writeValueAsString(updateDTO);

    mockMvc
        .perform(
            MockMvcRequestBuilders.patch(String.format("/v3/webhooks/%d", webhook.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.url", is("https://example.com/webhook-updated")));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void deleteWebhook_valid_ok() throws Exception {
    Webhook webhook =
        webhookRepository.save(new Webhook("https://example.com/webhook", true, true));

    mockMvc
        .perform(
            MockMvcRequestBuilders.delete(String.format("/v3/webhooks/%d", webhook.getId()))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(String.format("/v3/webhooks/%d", webhook.getId()))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().is4xxClientError());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getWebhookById_invalidId_notFound() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/v3/webhooks/99999")
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().is4xxClientError());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createWebhook_invalidUrl_badRequest() throws Exception {
    WebhookCreateDTO createDTO = new WebhookCreateDTO("invalid-url", true, true);
    String json = objectMapper.writeValueAsString(createDTO);

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/v3/webhooks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getWebhookById_unauthorized() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/v3/webhooks/1").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }
}
