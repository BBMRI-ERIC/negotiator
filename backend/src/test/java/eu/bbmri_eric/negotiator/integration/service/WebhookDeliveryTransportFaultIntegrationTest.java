package eu.bbmri_eric.negotiator.integration.service;

import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import eu.bbmri_eric.negotiator.webhook.DeliveryDTO;
import eu.bbmri_eric.negotiator.webhook.Webhook;
import eu.bbmri_eric.negotiator.webhook.WebhookRepository;
import eu.bbmri_eric.negotiator.webhook.WebhookService;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

@IntegrationTest
@TestPropertySource(properties = "negotiator.webhook.timeout.read=1s")
class WebhookDeliveryTransportFaultIntegrationTest {

  @RegisterExtension
  static WireMockExtension wireMockServer =
      WireMockExtension.newInstance()
          .options(WireMockConfiguration.options().dynamicPort())
          .build();

  @Autowired private WebhookRepository webhookRepository;

  @Autowired private WebhookService webhookService;

  @BeforeEach
  void setUp() {
    webhookRepository.deleteAll();
  }

  @Test
  void deliver_withResponseTimeout_setsNullStatusAndTimeoutMessage() {
    String url = wireMockServer.getRuntimeInfo().getHttpBaseUrl() + "/slow-endpoint";
    Webhook webhook = webhookRepository.save(new Webhook(url, true, true));

    wireMockServer.stubFor(
        post(urlEqualTo("/slow-endpoint")).willReturn(ok().withFixedDelay(3000).withBody("ok")));

    String payload = "{\"data\":\"success\"}";

    DeliveryDTO delivery = webhookService.deliver(payload, webhook.getId());

    assertNull(delivery.getHttpStatusCode());
    assertEquals("Request timeout", delivery.getErrorMessage());
    assertTrue(StringUtils.isNotBlank(delivery.getId()));
  }

  @ParameterizedTest
  @EnumSource(Fault.class)
  void deliver_withWireMockFault_setsErrorForAllFaultOptions(Fault fault) {
    String url = wireMockServer.getRuntimeInfo().getHttpBaseUrl() + "/fault-endpoint";
    Webhook webhook = webhookRepository.save(new Webhook(url, true, true));

    wireMockServer.stubFor(
        post(urlEqualTo("/fault-endpoint")).willReturn(ok().withFault(fault).withBody("fault")));

    String payload = "{\"data\":\"fault\"}";

    DeliveryDTO delivery = webhookService.deliver(payload, webhook.getId());

    assertTrue(delivery.getHttpStatusCode() == null || delivery.getHttpStatusCode() >= 500);
    assertTrue(StringUtils.isNotBlank(delivery.getErrorMessage()));
    assertTrue(StringUtils.isNotBlank(delivery.getId()));
  }
}
