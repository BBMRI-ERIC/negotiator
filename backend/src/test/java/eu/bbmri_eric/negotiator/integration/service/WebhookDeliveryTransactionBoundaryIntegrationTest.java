package eu.bbmri_eric.negotiator.integration.service;

import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import eu.bbmri_eric.negotiator.webhook.DeliveryDTO;
import eu.bbmri_eric.negotiator.webhook.Webhook;
import eu.bbmri_eric.negotiator.webhook.WebhookRepository;
import eu.bbmri_eric.negotiator.webhook.WebhookService;
import eu.bbmri_eric.negotiator.webhook.event.WebhookEventType;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

@IntegrationTest
@TestPropertySource(
    properties = {
      "spring.datasource.hikari.maximum-pool-size=2",
      "spring.datasource.hikari.minimum-idle=1",
      "spring.datasource.hikari.connection-timeout=500"
    })
class WebhookDeliveryTransactionBoundaryIntegrationTest {

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
  void deliver_withTwoSlowDeliveries_allowsConcurrentRepositoryRead() throws Exception {
    String baseUrl = wireMockServer.getRuntimeInfo().getHttpBaseUrl();
    Webhook firstWebhook = webhookRepository.save(new Webhook(baseUrl + "/slow-one", true, true));
    Webhook secondWebhook = webhookRepository.save(new Webhook(baseUrl + "/slow-two", true, true));

    wireMockServer.stubFor(
        post(urlEqualTo("/slow-one")).willReturn(ok().withFixedDelay(2500).withBody("ok")));
    wireMockServer.stubFor(
        post(urlEqualTo("/slow-two")).willReturn(ok().withFixedDelay(2500).withBody("ok")));

    try (ExecutorService executor = Executors.newFixedThreadPool(3)) {
      Future<DeliveryDTO> firstDeliveryFuture =
          executor.submit(
              () ->
                  webhookService.deliver(
                      "{\"delivery\":1}", WebhookEventType.CUSTOM, firstWebhook.getId()));
      Future<DeliveryDTO> secondDeliveryFuture =
          executor.submit(
              () ->
                  webhookService.deliver(
                      "{\"delivery\":2}", WebhookEventType.CUSTOM, secondWebhook.getId()));

      Thread.sleep(400);

      long startNanos = System.nanoTime();
      Future<Long> countFuture = executor.submit(() -> Long.valueOf(webhookRepository.count()));
      Long count = countFuture.get(1, TimeUnit.SECONDS);
      long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);

      assertTrue(count >= 2L);
      assertTrue(
          elapsedMillis < 1000,
          "Concurrent DB read should not wait for slow outbound webhook HTTP");

      DeliveryDTO firstDelivery = firstDeliveryFuture.get(6, TimeUnit.SECONDS);
      DeliveryDTO secondDelivery = secondDeliveryFuture.get(6, TimeUnit.SECONDS);

      assertEquals(200, firstDelivery.getHttpStatusCode());
      assertNull(firstDelivery.getErrorMessage());
      assertTrue(StringUtils.isNotBlank(firstDelivery.getId()));

      assertEquals(200, secondDelivery.getHttpStatusCode());
      assertNull(secondDelivery.getErrorMessage());
      assertTrue(StringUtils.isNotBlank(secondDelivery.getId()));
    }
  }
}
