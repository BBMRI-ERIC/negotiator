package eu.bbmri_eric.negotiator.integration.api;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import eu.bbmri_eric.negotiator.webhook.Webhook;
import eu.bbmri_eric.negotiator.webhook.WebhookHeaders;
import eu.bbmri_eric.negotiator.webhook.WebhookRepository;
import eu.bbmri_eric.negotiator.webhook.WebhookSecretRepository;
import eu.bbmri_eric.negotiator.webhook.WebhookSecretService;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
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

  @Autowired private WebhookSecretRepository webhookSecretRepository;

  @Autowired private WebhookSecretService webhookSecretService;

  @Autowired private WebApplicationContext context;

  @Autowired private ObjectMapper objectMapper;

  @BeforeEach
  public void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    webhookRepository.deleteAll();
    webhookSecretRepository.deleteAll();
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
    String payload =
        "{"
            + "\"url\":\"https://example.com/webhook\","
            + "\"sslVerification\":true,"
            + "\"active\":true"
            + "}";

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/v3/webhooks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.url", is("https://example.com/webhook")));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void updateWebhook_valid_ok() throws Exception {
    Webhook webhook =
        webhookRepository.save(new Webhook("https://example.com/webhook", true, true));
    String payload =
        "{"
            + "\"url\":\"https://example.com/webhook-updated\","
            + "\"sslVerification\":true,"
            + "\"active\":true"
            + "}";

    mockMvc
        .perform(
            MockMvcRequestBuilders.patch(String.format("/v3/webhooks/%d", webhook.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(webhook.getId().intValue())))
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
    String payload =
        "{" + "\"url\":\"invalid-url\"," + "\"sslVerification\":true," + "\"active\":true" + "}";

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/v3/webhooks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
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
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .withBody("{\"status\":\"received\"}")));

    // Prepare a JSON payload to send.
    String payload = "{\"data\":\"success\"}";

    // Perform the test delivery request via mockMvc.
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(
                    String.format("/v3/webhooks/%d/deliveries", webhook.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isOk());

    // Verify that the stub endpoint received a POST request with the expected JSON payload.
    verify(
        postRequestedFor(urlEqualTo("/test-endpoint"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withRequestBody(matchingJsonPath("$.type", equalTo("custom")))
            .withRequestBody(matchingJsonPath("$.timestamp"))
            .withRequestBody(matchingJsonPath("$.data.data", equalTo("success"))));

    var requests = findAll(postRequestedFor(urlEqualTo("/test-endpoint")));
    assertEquals(1, requests.size());
    var request = requests.get(0);
    assertTrue(request.containsHeader(WebhookHeaders.WEBHOOK_ID));
    assertTrue(request.getHeader(WebhookHeaders.TIMESTAMP).matches("\\d+"));
    assertFalse(request.containsHeader(WebhookHeaders.SIGNATURE));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void redeliver_validDelivery_returnsCreatedAndLinksToOriginal(WireMockRuntimeInfo wmRuntimeInfo)
      throws Exception {
    String url = wmRuntimeInfo.getHttpBaseUrl() + "/test-endpoint";
    Webhook webhook = webhookRepository.save(new Webhook(url, true, true));
    stubFor(
        post(urlEqualTo("/test-endpoint"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .withBody("{\"status\":\"received\"}")));

    String payload = "{\"data\":\"redelivery\"}";

    MvcResult firstDeliveryResult =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post(
                        String.format("/v3/webhooks/%d/deliveries", webhook.getId()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload))
            .andExpect(status().isOk())
            .andReturn();

    String sourceDeliveryId =
        objectMapper
            .readTree(firstDeliveryResult.getResponse().getContentAsString())
            .get("id")
            .asText();

    mockMvc
        .perform(
            MockMvcRequestBuilders.post(
                    String.format(
                        "/v3/webhooks/%d/deliveries/%s/redeliver",
                        webhook.getId(), sourceDeliveryId))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id", not(sourceDeliveryId)))
        .andExpect(jsonPath("$.rootId", is(sourceDeliveryId)))
        .andExpect(jsonPath("$.redelivery", is(true)))
        .andExpect(jsonPath("$.httpStatusCode", is(200)))
        .andExpect(jsonPath("$.errorMessage").doesNotExist());

    verify(
        2,
        postRequestedFor(urlEqualTo("/test-endpoint"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withRequestBody(matchingJsonPath("$.type", equalTo("custom")))
            .withRequestBody(matchingJsonPath("$.timestamp"))
            .withRequestBody(matchingJsonPath("$.data.data", equalTo("redelivery"))));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void redeliver_unknownDelivery_returns4xx(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
    String url = wmRuntimeInfo.getHttpBaseUrl() + "/test-endpoint";
    Webhook webhook = webhookRepository.save(new Webhook(url, true, true));

    mockMvc
        .perform(
            MockMvcRequestBuilders.post(
                    String.format(
                        "/v3/webhooks/%d/deliveries/%s/redeliver",
                        webhook.getId(), "unknown-delivery"))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().is4xxClientError());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void redeliver_inactiveWebhook_returnsBadRequest(WireMockRuntimeInfo wmRuntimeInfo)
      throws Exception {
    String url = wmRuntimeInfo.getHttpBaseUrl() + "/test-endpoint";
    Webhook webhook = webhookRepository.save(new Webhook(url, true, true));
    stubFor(
        post(urlEqualTo("/test-endpoint")).willReturn(aResponse().withStatus(200).withBody("OK")));

    String payload = "{\"data\":\"redelivery\"}";
    MvcResult firstDeliveryResult =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post(
                        String.format("/v3/webhooks/%d/deliveries", webhook.getId()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload))
            .andExpect(status().isOk())
            .andReturn();
    String sourceDeliveryId =
        objectMapper
            .readTree(firstDeliveryResult.getResponse().getContentAsString())
            .get("id")
            .asText();

    webhook.setActive(false);
    webhookRepository.save(webhook);

    mockMvc
        .perform(
            MockMvcRequestBuilders.post(
                    String.format(
                        "/v3/webhooks/%d/deliveries/%s/redeliver",
                        webhook.getId(), sourceDeliveryId))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath(
                "$.detail", containsString("Webhook is not active, therefore cannot deliver")));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void redeliver_redeliveryOfRedelivery_preservesOriginalRootId(WireMockRuntimeInfo wmRuntimeInfo)
      throws Exception {
    String url = wmRuntimeInfo.getHttpBaseUrl() + "/test-endpoint";
    Webhook webhook = webhookRepository.save(new Webhook(url, true, true));
    stubFor(
        post(urlEqualTo("/test-endpoint"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .withBody("{\"status\":\"received\"}")));

    String payload = "{\"data\":\"redelivery\"}";

    MvcResult sourceDeliveryResult =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post(
                        String.format("/v3/webhooks/%d/deliveries", webhook.getId()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload))
            .andExpect(status().isOk())
            .andReturn();

    String sourceDeliveryId =
        objectMapper
            .readTree(sourceDeliveryResult.getResponse().getContentAsString())
            .get("id")
            .asText();

    MvcResult firstRedeliveryResult =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post(
                        String.format(
                            "/v3/webhooks/%d/deliveries/%s/redeliver",
                            webhook.getId(), sourceDeliveryId))
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.rootId", is(sourceDeliveryId)))
            .andExpect(jsonPath("$.redelivery", is(true)))
            .andReturn();

    String firstRedeliveryId =
        objectMapper
            .readTree(firstRedeliveryResult.getResponse().getContentAsString())
            .get("id")
            .asText();

    mockMvc
        .perform(
            MockMvcRequestBuilders.post(
                    String.format(
                        "/v3/webhooks/%d/deliveries/%s/redeliver",
                        webhook.getId(), firstRedeliveryId))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id", not(firstRedeliveryId)))
        .andExpect(jsonPath("$.rootId", is(sourceDeliveryId)))
        .andExpect(jsonPath("$.redelivery", is(true)))
        .andExpect(jsonPath("$.httpStatusCode", is(200)))
        .andExpect(jsonPath("$.errorMessage").doesNotExist());

    verify(
        3,
        postRequestedFor(urlEqualTo("/test-endpoint"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withRequestBody(matchingJsonPath("$.type", equalTo("custom")))
            .withRequestBody(matchingJsonPath("$.timestamp"))
            .withRequestBody(matchingJsonPath("$.data.data", equalTo("redelivery"))));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createWebhook_withSecret_returnsResponseWithoutPlainTextAndStoresSecret() throws Exception {
    String plainTextSecret = webhookSecretForBytes(24);
    String payload =
        "{"
            + "\"url\":\"https://example.com/webhook\","
            + "\"sslVerification\":true,"
            + "\"active\":true,"
            + "\"secret\":\""
            + plainTextSecret
            + "\""
            + "}";

    MvcResult createResult =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/v3/webhooks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.secretId").isString())
            .andExpect(jsonPath("$.secret").doesNotExist())
            .andReturn();

    String responseBody = createResult.getResponse().getContentAsString();
    assertFalse(responseBody.contains(plainTextSecret));
    var createdJson = objectMapper.readTree(responseBody);
    assertTrue(webhookSecretRepository.existsById(createdJson.get("secretId").asText()));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void updateWebhook_withSecret_replacesExistingSecret() throws Exception {
    String firstSecret = webhookSecretForBytes(24);
    String secondSecret = webhookSecretForBytes(32);

    String createPayload =
        "{"
            + "\"url\":\"https://example.com/webhook\","
            + "\"sslVerification\":true,"
            + "\"active\":true,"
            + "\"secret\":\""
            + firstSecret
            + "\""
            + "}";

    MvcResult createResult =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/v3/webhooks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createPayload))
            .andExpect(status().isOk())
            .andReturn();

    var createdJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
    long webhookId = createdJson.get("id").asLong();
    String firstSecretId = createdJson.get("secretId").asText();

    String updatePayload =
        "{"
            + "\"url\":\"https://example.com/webhook-updated\","
            + "\"sslVerification\":true,"
            + "\"active\":true,"
            + "\"secret\":\""
            + secondSecret
            + "\""
            + "}";

    MvcResult updateResult =
        mockMvc
            .perform(
                MockMvcRequestBuilders.patch(String.format("/v3/webhooks/%d", webhookId))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(updatePayload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.url", is("https://example.com/webhook-updated")))
            .andExpect(jsonPath("$.secretId").isString())
            .andReturn();

    String updateResponse = updateResult.getResponse().getContentAsString();
    assertFalse(updateResponse.contains(secondSecret));

    var updatedJson = objectMapper.readTree(updateResponse);
    String secondSecretId = updatedJson.get("secretId").asText();
    assertNotEquals(firstSecretId, secondSecretId);
    assertFalse(webhookSecretRepository.existsById(firstSecretId));
    assertTrue(webhookSecretRepository.existsById(secondSecretId));
    assertEquals(secondSecret, webhookSecretService.decryptSecret(secondSecretId).plainText());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void updateWebhook_withNullSecret_removesExistingSecret() throws Exception {
    String firstSecret = webhookSecretForBytes(24);
    String createPayload =
        "{"
            + "\"url\":\"https://example.com/webhook\","
            + "\"sslVerification\":true,"
            + "\"active\":true,"
            + "\"secret\":\""
            + firstSecret
            + "\""
            + "}";

    MvcResult createResult =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/v3/webhooks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createPayload))
            .andExpect(status().isOk())
            .andReturn();

    var createdJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
    long webhookId = createdJson.get("id").asLong();
    String firstSecretId = createdJson.get("secretId").asText();

    String updatePayload =
        "{"
            + "\"url\":\"https://example.com/webhook\","
            + "\"sslVerification\":true,"
            + "\"active\":true,"
            + "\"secret\":null"
            + "}";

    MvcResult updateResult =
        mockMvc
            .perform(
                MockMvcRequestBuilders.patch(String.format("/v3/webhooks/%d", webhookId))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(updatePayload))
            .andExpect(status().isOk())
            .andReturn();

    var updatedJson = objectMapper.readTree(updateResult.getResponse().getContentAsString());
    assertTrue(
        updatedJson.path("secretId").isMissingNode() || updatedJson.path("secretId").isNull());

    Webhook reloaded = webhookRepository.findById(webhookId).orElseThrow();
    assertNull(reloaded.getSecretId());
    assertFalse(webhookSecretRepository.existsById(firstSecretId));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void updateWebhook_withoutSecretField_keepsExistingSecret() throws Exception {
    String firstSecret = webhookSecretForBytes(24);
    String createPayload =
        "{"
            + "\"url\":\"https://example.com/webhook\","
            + "\"sslVerification\":true,"
            + "\"active\":true,"
            + "\"secret\":\""
            + firstSecret
            + "\""
            + "}";

    MvcResult createResult =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/v3/webhooks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createPayload))
            .andExpect(status().isOk())
            .andReturn();

    var createdJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
    long webhookId = createdJson.get("id").asLong();
    String firstSecretId = createdJson.get("secretId").asText();

    String updatePayload =
        "{"
            + "\"url\":\"https://example.com/webhook-updated\","
            + "\"sslVerification\":true,"
            + "\"active\":true"
            + "}";

    MvcResult updateResult =
        mockMvc
            .perform(
                MockMvcRequestBuilders.patch(String.format("/v3/webhooks/%d", webhookId))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(updatePayload))
            .andExpect(status().isOk())
            .andReturn();

    var updatedJson = objectMapper.readTree(updateResult.getResponse().getContentAsString());
    assertEquals(firstSecretId, updatedJson.get("secretId").asText());
    assertTrue(webhookSecretRepository.existsById(firstSecretId));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createWebhook_withLegacySecretWithoutWhsecPrefix_returnsBadRequest() throws Exception {
    String payload =
        "{"
            + "\"url\":\"https://example.com/webhook\","
            + "\"sslVerification\":true,"
            + "\"active\":true,"
            + "\"secret\":\""
            + Base64.getEncoder().encodeToString("a".repeat(24).getBytes(StandardCharsets.UTF_8))
            + "\""
            + "}";

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/v3/webhooks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isBadRequest());

    assertEquals(0L, webhookRepository.count());
    assertEquals(0L, webhookSecretRepository.count());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createWebhook_withUppercaseWhsecPrefix_returnsBadRequest() throws Exception {
    String payload =
        "{"
            + "\"url\":\"https://example.com/webhook\","
            + "\"sslVerification\":true,"
            + "\"active\":true,"
            + "\"secret\":\""
            + "WHSEC_"
            + Base64.getEncoder().encodeToString("a".repeat(24).getBytes(StandardCharsets.UTF_8))
            + "\""
            + "}";

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/v3/webhooks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isBadRequest());

    assertEquals(0L, webhookRepository.count());
    assertEquals(0L, webhookSecretRepository.count());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createWebhook_withInvalidBase64Encoding_returnsBadRequest() throws Exception {
    String payload =
        "{"
            + "\"url\":\"https://example.com/webhook\","
            + "\"sslVerification\":true,"
            + "\"active\":true,"
            + "\"secret\":\"whsec_invalid-base64-$$$\""
            + "}";

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/v3/webhooks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isBadRequest());

    assertEquals(0L, webhookRepository.count());
    assertEquals(0L, webhookSecretRepository.count());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createWebhook_withDecodedSecretShorterThan24Bytes_returnsBadRequest() throws Exception {
    String payload =
        "{"
            + "\"url\":\"https://example.com/webhook\","
            + "\"sslVerification\":true,"
            + "\"active\":true,"
            + "\"secret\":\""
            + webhookSecretForBytes(23)
            + "\""
            + "}";

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/v3/webhooks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isBadRequest());

    assertEquals(0L, webhookRepository.count());
    assertEquals(0L, webhookSecretRepository.count());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createWebhook_withDecodedSecretLongerThan64Bytes_returnsBadRequest() throws Exception {
    String payload =
        "{"
            + "\"url\":\"https://example.com/webhook\","
            + "\"sslVerification\":true,"
            + "\"active\":true,"
            + "\"secret\":\""
            + webhookSecretForBytes(65)
            + "\""
            + "}";

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/v3/webhooks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isBadRequest());

    assertEquals(0L, webhookRepository.count());
    assertEquals(0L, webhookSecretRepository.count());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createWebhook_withSecretWrappedInWhitespace_returnsBadRequest() throws Exception {
    String plainTextSecret = webhookSecretForBytes(24);
    String payload =
        "{"
            + "\"url\":\"https://example.com/webhook\","
            + "\"sslVerification\":true,"
            + "\"active\":true,"
            + "\"secret\":\"  "
            + plainTextSecret
            + "  \""
            + "}";

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/v3/webhooks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail", is("Secret must start with whsec_")));

    assertEquals(0L, webhookRepository.count());
    assertEquals(0L, webhookSecretRepository.count());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createWebhook_withUrlSafeBase64Secret_returnsBadRequest() throws Exception {
    String plainTextSecret = "whsec_raTaNRqLwV0RCO-cLBa3QTie4mrgcUgQ0xSq1kVLyiE";
    String payload =
        "{"
            + "\"url\":\"https://example.com/webhook\","
            + "\"sslVerification\":true,"
            + "\"active\":true,"
            + "\"secret\":\""
            + plainTextSecret
            + "\""
            + "}";

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/v3/webhooks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isBadRequest());

    assertEquals(0L, webhookRepository.count());
    assertEquals(0L, webhookSecretRepository.count());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createWebhook_withWhitespaceOnlySecret_returnsBadRequest() throws Exception {
    String payload =
        "{"
            + "\"url\":\"https://example.com/webhook\","
            + "\"sslVerification\":true,"
            + "\"active\":true,"
            + "\"secret\":\"   \""
            + "}";

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/v3/webhooks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail", is("Secret must not be blank")));

    assertEquals(0L, webhookRepository.count());
    assertEquals(0L, webhookSecretRepository.count());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void updateWebhook_withSecretWrappedInWhitespace_returnsBadRequestAndKeepsExistingSecret()
      throws Exception {
    String firstSecret = webhookSecretForBytes(24);
    String createPayload =
        "{"
            + "\"url\":\"https://example.com/webhook\","
            + "\"sslVerification\":true,"
            + "\"active\":true,"
            + "\"secret\":\""
            + firstSecret
            + "\""
            + "}";

    MvcResult createResult =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/v3/webhooks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createPayload))
            .andExpect(status().isOk())
            .andReturn();

    var createdJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
    long webhookId = createdJson.get("id").asLong();
    String firstSecretId = createdJson.get("secretId").asText();

    String updatePayload =
        "{"
            + "\"url\":\"https://example.com/webhook-updated\","
            + "\"sslVerification\":true,"
            + "\"active\":true,"
            + "\"secret\":\"  "
            + webhookSecretForBytes(24)
            + "  \""
            + "}";

    mockMvc
        .perform(
            MockMvcRequestBuilders.patch(String.format("/v3/webhooks/%d", webhookId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatePayload))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail", is("Secret must start with whsec_")));

    Webhook reloaded = webhookRepository.findById(webhookId).orElseThrow();
    assertEquals(firstSecretId, reloaded.getSecretId());
    assertTrue(webhookSecretRepository.existsById(firstSecretId));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void secretEndpoints_removed_returnClientError() throws Exception {
    Webhook webhook =
        webhookRepository.save(new Webhook("https://example.com/webhook", true, true));
    String payload = "{\"secret\":\"" + webhookSecretForBytes(24) + "\"}";

    mockMvc
        .perform(
            MockMvcRequestBuilders.post(String.format("/v3/webhooks/%d/secret", webhook.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().is4xxClientError());

    mockMvc
        .perform(
            MockMvcRequestBuilders.delete(String.format("/v3/webhooks/%d/secret", webhook.getId()))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().is4xxClientError());

    mockMvc
        .perform(
            MockMvcRequestBuilders.post(
                    String.format("/v3/webhooks/%d/secret/regenerate", webhook.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().is4xxClientError());
  }

  private static String webhookSecretForBytes(int bytesCount) {
    String material = "a".repeat(bytesCount);
    return "whsec_" + Base64.getEncoder().encodeToString(material.getBytes(StandardCharsets.UTF_8));
  }
}
