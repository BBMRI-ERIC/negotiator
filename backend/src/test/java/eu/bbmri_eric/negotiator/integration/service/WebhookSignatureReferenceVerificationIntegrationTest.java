package eu.bbmri_eric.negotiator.integration.service;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.getAllServeEvents;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import eu.bbmri_eric.negotiator.webhook.DecryptedWebhookSecret;
import eu.bbmri_eric.negotiator.webhook.DeliveryDTO;
import eu.bbmri_eric.negotiator.webhook.Webhook;
import eu.bbmri_eric.negotiator.webhook.WebhookHeaders;
import eu.bbmri_eric.negotiator.webhook.WebhookRepository;
import eu.bbmri_eric.negotiator.webhook.WebhookSecretService;
import eu.bbmri_eric.negotiator.webhook.WebhookService;
import eu.bbmri_eric.negotiator.webhook.event.WebhookEventType;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

@IntegrationTest
@WireMockTest
class WebhookSignatureReferenceVerificationIntegrationTest {

  @Autowired private WebhookRepository webhookRepository;

  @Autowired private WebhookService webhookService;

  @Autowired private WebhookSecretService webhookSecretService;

  @BeforeEach
  void setUp() {
    webhookRepository.deleteAll();
  }

  @Test
  void deliver_withSecret_emitsSignatureVerifiedByReferenceImplementation(
      WireMockRuntimeInfo wmInfo) {
    String url = wmInfo.getHttpBaseUrl() + "/signed-endpoint";

    DecryptedWebhookSecret secret = webhookSecretService.createSecret(webhookSecretForBytes(32));

    var webhook = new Webhook(url, true, true);
    webhook.setSecretId(secret.id());
    webhook = webhookRepository.save(webhook);

    stubFor(
        post(urlEqualTo("/signed-endpoint"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .withBody("{\"status\":\"received\"}")));

    DeliveryDTO delivery =
        webhookService.deliver("{\"data\":\"signed\"}", WebhookEventType.CUSTOM, webhook.getId());

    assertEquals(200, delivery.getHttpStatusCode());
    verify(1, postRequestedFor(urlEqualTo("/signed-endpoint")));

    var serveEvents =
        getAllServeEvents().stream()
            .filter(event -> event.getRequest().getUrl().equals("/signed-endpoint"))
            .toList();
    assertEquals(1, serveEvents.size());

    var request = serveEvents.get(0).getRequest();
    assertTrue(request.containsHeader(WebhookHeaders.WEBHOOK_ID));
    assertTrue(request.containsHeader(WebhookHeaders.TIMESTAMP));
    assertTrue(request.containsHeader(WebhookHeaders.SIGNATURE));

    Map<String, List<String>> headers =
        Map.of(
            "webhook-id", List.of(request.getHeader(WebhookHeaders.WEBHOOK_ID)),
            "webhook-timestamp", List.of(request.getHeader(WebhookHeaders.TIMESTAMP)),
            "webhook-signature", List.of(request.getHeader(WebhookHeaders.SIGNATURE)));

    var referenceVerifier = new com.standardwebhooks.Webhook(secret.plainText());
    assertDoesNotThrow(() -> referenceVerifier.verify(request.getBodyAsString(), headers));
  }

  private static String webhookSecretForBytes(int bytesCount) {
    String material = "a".repeat(bytesCount);
    return "whsec_" + Base64.getEncoder().encodeToString(material.getBytes(StandardCharsets.UTF_8));
  }
}
