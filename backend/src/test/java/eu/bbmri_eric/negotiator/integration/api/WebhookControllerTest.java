package eu.bbmri_eric.negotiator.integration.api;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import eu.bbmri_eric.negotiator.common.JSONUtils;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import eu.bbmri_eric.negotiator.webhook.Webhook;
import eu.bbmri_eric.negotiator.webhook.WebhookCreateDTO;
import eu.bbmri_eric.negotiator.webhook.WebhookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@IntegrationTest
@WireMockTest
public class WebhookControllerTest {

  @RegisterExtension
  static WireMockExtension wireMockServer =
      WireMockExtension.newInstance()
          .options(WireMockConfiguration.options().dynamicPort())
          .build();

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

  @Test
  @WithMockUser(roles = "ADMIN")
  void testDeliverSuccess(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
    String url = wmRuntimeInfo.getHttpBaseUrl() + "/test-endpoint";
    Webhook webhook = new Webhook(url, true, true);
    webhook = webhookRepository.save(webhook);
    stubFor(
        post(urlEqualTo("/test-endpoint")).willReturn(aResponse().withStatus(200).withBody("OK")));
    String payload = objectMapper.writeValueAsString("{\"data\":\"fail\"}");
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(
                    String.format("/v3/webhooks/%d/deliveries", webhook.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isOk())
        .andDo(print())
        .andExpect(jsonPath("$.httpStatusCode", is(200)))
        .andExpect(jsonPath("$.errorMessage").doesNotExist());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void testDeliverInvalidJson(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
    String url = wmRuntimeInfo.getHttpBaseUrl() + "/test-endpoint";
    Webhook webhook = new Webhook(url, true, true);
    webhook = webhookRepository.save(webhook);
    String invalidPayload = "Not a JSON";
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(
                    String.format("/v3/webhooks/%d/deliveries", webhook.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidPayload))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail", is("Content is not a valid JSON")));
    invalidPayload = "{\"data\"\"fail\"}";
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(
                    String.format("/v3/webhooks/%d/deliveries", webhook.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidPayload))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail", is("Content is not a valid JSON")));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void testDeliverExternalServerError(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
    String url = wmRuntimeInfo.getHttpBaseUrl() + "/test-endpoint";
    Webhook webhook = new Webhook(url, true, true);
    webhook = webhookRepository.save(webhook);
    stubFor(
        post(urlEqualTo("/test-endpoint"))
            .willReturn(aResponse().withStatus(500).withBody("Internal Server Error")));
    String payload = objectMapper.writeValueAsString("{\"data\":\"fail\"}");
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(
                    String.format("/v3/webhooks/%d/deliveries", webhook.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isOk())
        .andDo(print())
        .andExpect(jsonPath("$.httpStatusCode", is(500)))
        .andExpect(jsonPath("$.errorMessage", containsString("Internal")));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void testDeliver_notActive_returns400(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
    String url = wmRuntimeInfo.getHttpBaseUrl() + "/test-endpoint";
    Webhook webhook = new Webhook(url, true, false);
    webhook = webhookRepository.save(webhook);
    stubFor(
        post(urlEqualTo("/test-endpoint"))
            .willReturn(aResponse().withStatus(500).withBody("Internal Server Error")));
    String payload = objectMapper.writeValueAsString("{\"data\":\"fail\"}");
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(
                    String.format("/v3/webhooks/%d/deliveries", webhook.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath(
                "$.detail", containsString("Webhook is not active, therefore cannot deliver")));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void testDeliver_active_receivesWebhook(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
    // Build the URL for the WireMock stub endpoint.
    String url = wmRuntimeInfo.getHttpBaseUrl() + "/test-endpoint";

    // Create an active webhook (active = true)
    Webhook webhook = new Webhook(url, true, true);
    webhook = webhookRepository.save(webhook);

    // Stub the endpoint to simulate a successful delivery.
    stubFor(
            post(urlEqualTo("/test-endpoint"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                            .withBody("{\"status\":\"received\"}")));

    // Prepare a JSON payload to send.
    String payload = "{\"data\":\"success\"}";

    // Perform the test delivery request via mockMvc.
    mockMvc.perform(
                    MockMvcRequestBuilders.post(String.format("/v3/webhooks/%d/deliveries", webhook.getId()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
            .andExpect(status().isOk());

    // Verify that the stub endpoint received a POST request with the expected JSON payload.
    verify(postRequestedFor(urlEqualTo("/test-endpoint"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withRequestBody(equalToJson(JSONUtils.toJSON(payload))));
  }

}
