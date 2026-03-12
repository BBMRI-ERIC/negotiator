package eu.bbmri_eric.negotiator.integration.service;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import eu.bbmri_eric.negotiator.integration.api.WebhookSslTestConfig;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import eu.bbmri_eric.negotiator.webhook.DeliveryDTO;
import eu.bbmri_eric.negotiator.webhook.Webhook;
import eu.bbmri_eric.negotiator.webhook.WebhookRepository;
import eu.bbmri_eric.negotiator.webhook.WebhookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

@IntegrationTest
@Import(WebhookSslTestConfig.class)
public class WebhookDeliverySslIntegrationTest {
  private static final String SSL_STORE_PASSWORD = "changeit";

  @RegisterExtension
  static WireMockExtension trustedHttpsServer =
      WireMockExtension.newInstance()
          .options(
              WireMockConfiguration.options()
                  .dynamicPort()
                  .dynamicHttpsPort()
                  .keystorePath("src/test/resources/ssl/wiremock.jks")
                  .keystorePassword(SSL_STORE_PASSWORD)
                  .keyManagerPassword(SSL_STORE_PASSWORD))
          .build();

  @RegisterExtension
  static WireMockExtension untrustedHttpsServer =
      WireMockExtension.newInstance()
          .options(WireMockConfiguration.options().dynamicPort().dynamicHttpsPort())
          .build();

  @Autowired private WebhookRepository webhookRepository;

  @Autowired private WebhookService webhookService;

  @BeforeEach
  void setUp() {
    webhookRepository.deleteAll();
  }

  @Test
  void deliver_sslVerificationEnabled_withTrustedCert_sendsRequest() {
    String url = trustedHttpsServer.getRuntimeInfo().getHttpsBaseUrl() + "/test-endpoint";
    Webhook webhook = webhookRepository.save(new Webhook(url, true, true));

    trustedHttpsServer.stubFor(
        post(urlEqualTo("/test-endpoint"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .withBody("{\"status\":\"received\"}")));

    String payload = "{\"data\":\"success\"}";

    DeliveryDTO delivery = webhookService.deliver(payload, webhook.getId());

    assertEquals(200, delivery.getHttpStatusCode());
    trustedHttpsServer.verify(
        postRequestedFor(urlEqualTo("/test-endpoint"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withRequestBody(equalToJson(payload)));
  }

  @Test
  void deliver_sslVerificationEnabled_withUntrustedAndHostnameMismatchCert_doesNotSendRequest() {
    String baseUrl = untrustedHttpsServer.getRuntimeInfo().getHttpsBaseUrl();
    String url = baseUrl.replace("localhost", "127.0.0.1") + "/test-endpoint";
    Webhook webhook = webhookRepository.save(new Webhook(url, true, true));

    untrustedHttpsServer.stubFor(
        post(urlEqualTo("/test-endpoint"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .withBody("{\"status\":\"received\"}")));

    String payload = "{\"data\":\"success\"}";

    DeliveryDTO delivery = webhookService.deliver(payload, webhook.getId());

    assertNull(delivery.getHttpStatusCode());
    assertEquals("SSL certificate validation failed", delivery.getErrorMessage());
    untrustedHttpsServer.verify(0, postRequestedFor(urlEqualTo("/test-endpoint")));
  }

  @Test
  void deliver_sslVerificationDisabled_withTrustedCert_sendsRequest() {
    String url = trustedHttpsServer.getRuntimeInfo().getHttpsBaseUrl() + "/test-endpoint";
    Webhook webhook = webhookRepository.save(new Webhook(url, false, true));

    trustedHttpsServer.stubFor(
        post(urlEqualTo("/test-endpoint"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .withBody("{\"status\":\"received\"}")));

    String payload = "{\"data\":\"success\"}";

    DeliveryDTO delivery = webhookService.deliver(payload, webhook.getId());

    assertEquals(200, delivery.getHttpStatusCode());
    trustedHttpsServer.verify(
        postRequestedFor(urlEqualTo("/test-endpoint"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withRequestBody(equalToJson(payload)));
  }

  @Test
  void deliver_sslVerificationDisabled_withUntrustedAndHostnameMismatchCert_sendsRequest() {
    String baseUrl = untrustedHttpsServer.getRuntimeInfo().getHttpsBaseUrl();
    String url = baseUrl.replace("localhost", "127.0.0.1") + "/test-endpoint";
    Webhook webhook = webhookRepository.save(new Webhook(url, false, true));

    untrustedHttpsServer.stubFor(
        post(urlEqualTo("/test-endpoint"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .withBody("{\"status\":\"received\"}")));

    String payload = "{\"data\":\"success\"}";

    DeliveryDTO delivery = webhookService.deliver(payload, webhook.getId());

    assertEquals(200, delivery.getHttpStatusCode());
    untrustedHttpsServer.verify(
        postRequestedFor(urlEqualTo("/test-endpoint"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withRequestBody(equalToJson(payload)));
  }
}
