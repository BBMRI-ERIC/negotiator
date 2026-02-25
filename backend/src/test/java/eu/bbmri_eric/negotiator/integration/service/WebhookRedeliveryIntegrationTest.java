package eu.bbmri_eric.negotiator.integration.service;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import eu.bbmri_eric.negotiator.webhook.DeliveryDTO;
import eu.bbmri_eric.negotiator.webhook.Webhook;
import eu.bbmri_eric.negotiator.webhook.WebhookRepository;
import eu.bbmri_eric.negotiator.webhook.WebhookService;
import eu.bbmri_eric.negotiator.webhook.event.WebhookEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

@IntegrationTest
@WireMockTest
public class WebhookRedeliveryIntegrationTest {

  @Autowired private WebhookRepository webhookRepository;

  @Autowired private WebhookService webhookService;

  @BeforeEach
  void setUp() {
    webhookRepository.deleteAll();
  }

  @Test
  void redeliver_redeliveryOfRedelivery_preservesOriginalRootId(WireMockRuntimeInfo wmInfo) {
    String url = wmInfo.getHttpBaseUrl() + "/redeliver-endpoint";
    Webhook webhook = webhookRepository.save(new Webhook(url, true, true));

    stubFor(
        post(urlEqualTo("/redeliver-endpoint"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .withBody("{\"status\":\"received\"}")));

    String payload = "{\"data\":\"redelivery\"}";

    DeliveryDTO firstDelivery =
        webhookService.deliver(payload, WebhookEventType.CUSTOM, webhook.getId());
    DeliveryDTO redelivery = webhookService.redeliver(webhook.getId(), firstDelivery.getId());
    DeliveryDTO secondRedelivery = webhookService.redeliver(webhook.getId(), redelivery.getId());

    assertEquals(200, redelivery.getHttpStatusCode());
    assertEquals(firstDelivery.getId(), redelivery.getRootId());
    assertTrue(redelivery.getRedelivery());
    assertNull(redelivery.getErrorMessage());

    assertEquals(200, secondRedelivery.getHttpStatusCode());
    assertEquals(firstDelivery.getId(), secondRedelivery.getRootId());
    assertTrue(secondRedelivery.getRedelivery());
    assertNull(secondRedelivery.getErrorMessage());

    verify(
        3,
        postRequestedFor(urlEqualTo("/redeliver-endpoint"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withRequestBody(equalToJson(payload)));
  }
}
