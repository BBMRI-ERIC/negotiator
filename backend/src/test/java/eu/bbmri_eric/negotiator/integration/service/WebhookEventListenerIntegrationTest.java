package eu.bbmri_eric.negotiator.integration.service;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.awaitility.Awaitility.await;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import eu.bbmri_eric.negotiator.email.EmailService;
import eu.bbmri_eric.negotiator.info_submission.InformationSubmissionEvent;
import eu.bbmri_eric.negotiator.integration.api.WebhookSslTestConfig;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationStateChangeEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.ResourceStateChangeListener;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceState;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.ResourceStateChangeEvent;
import eu.bbmri_eric.negotiator.post.NewPostEvent;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import eu.bbmri_eric.negotiator.webhook.Webhook;
import eu.bbmri_eric.negotiator.webhook.WebhookHeaders;
import eu.bbmri_eric.negotiator.webhook.WebhookRepository;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@IntegrationTest
@Import(WebhookSslTestConfig.class)
class WebhookEventListenerIntegrationTest {

  @RegisterExtension
  static WireMockExtension wireMockServer =
      WireMockExtension.newInstance()
          .options(WireMockConfiguration.options().dynamicPort())
          .build();

  @Autowired private WebhookRepository webhookRepository;

  @Autowired private ApplicationEventPublisher eventPublisher;

  @MockitoBean private EmailService emailService;

  @MockitoBean private ResourceStateChangeListener resourceStateChangeListener;

  @BeforeEach
  void setUp() {
    webhookRepository.deleteAll();
  }

  @Test
  void publishNegotiationStateChangeEvent_dispatchesToActiveWebhooks() {
    String baseUrl = wireMockServer.getRuntimeInfo().getHttpBaseUrl();
    createWebhook(baseUrl + "/negotiation-one", true);
    createWebhook(baseUrl + "/negotiation-two", true);
    createWebhook(baseUrl + "/negotiation-inactive", false);

    wireMockServer.stubFor(post(urlEqualTo("/negotiation-one")));
    wireMockServer.stubFor(post(urlEqualTo("/negotiation-two")));

    eventPublisher.publishEvent(
        new NegotiationStateChangeEvent(
            this, "negotiation-1", NegotiationState.SUBMITTED, NegotiationEvent.SUBMIT, "post"));

    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(
            () -> {
              wireMockServer.verify(
                  1,
                  postRequestedFor(urlEqualTo("/negotiation-one"))
                      .withHeader(
                          WebhookHeaders.EVENT_TYPE, equalTo("NegotiationStateChangeEvent")));
              wireMockServer.verify(
                  1,
                  postRequestedFor(urlEqualTo("/negotiation-two"))
                      .withHeader(
                          WebhookHeaders.EVENT_TYPE, equalTo("NegotiationStateChangeEvent")));
              wireMockServer.verify(0, postRequestedFor(urlEqualTo("/negotiation-inactive")));
            });
  }

  @Test
  void publishResourceStateChangeEvent_dispatchesToActiveWebhooks() {
    String baseUrl = wireMockServer.getRuntimeInfo().getHttpBaseUrl();
    createWebhook(baseUrl + "/resource-one", true);
    createWebhook(baseUrl + "/resource-two", true);
    createWebhook(baseUrl + "/resource-inactive", false);

    wireMockServer.stubFor(post(urlEqualTo("/resource-one")));
    wireMockServer.stubFor(post(urlEqualTo("/resource-two")));

    eventPublisher.publishEvent(
        new ResourceStateChangeEvent(
            this,
            "negotiation-2",
            "resource-1",
            NegotiationResourceState.SUBMITTED,
            NegotiationResourceState.RESOURCE_AVAILABLE));

    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(
            () -> {
              wireMockServer.verify(
                  1,
                  postRequestedFor(urlEqualTo("/resource-one"))
                      .withHeader(WebhookHeaders.EVENT_TYPE, equalTo("ResourceStateChangeEvent")));
              wireMockServer.verify(
                  1,
                  postRequestedFor(urlEqualTo("/resource-two"))
                      .withHeader(WebhookHeaders.EVENT_TYPE, equalTo("ResourceStateChangeEvent")));
              wireMockServer.verify(0, postRequestedFor(urlEqualTo("/resource-inactive")));
            });
  }

  @Test
  void createPost_dispatchesToActiveWebhooks() {
    String baseUrl = wireMockServer.getRuntimeInfo().getHttpBaseUrl();
    createWebhook(baseUrl + "/post-one", true);
    createWebhook(baseUrl + "/post-two", true);
    createWebhook(baseUrl + "/post-inactive", false);

    wireMockServer.stubFor(post(urlEqualTo("/post-one")));
    wireMockServer.stubFor(post(urlEqualTo("/post-two")));

    eventPublisher.publishEvent(new NewPostEvent(this, "post-1", "negotiation-1", 10000L, 20000L));

    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(
            () -> {
              wireMockServer.verify(
                  1,
                  postRequestedFor(urlEqualTo("/post-one"))
                      .withHeader(WebhookHeaders.EVENT_TYPE, equalTo("NewPostEvent")));
              wireMockServer.verify(
                  1,
                  postRequestedFor(urlEqualTo("/post-two"))
                      .withHeader(WebhookHeaders.EVENT_TYPE, equalTo("NewPostEvent")));
              wireMockServer.verify(0, postRequestedFor(urlEqualTo("/post-inactive")));
            });
  }

  @Test
  void publishInformationSubmissionEvent_dispatchesToActiveWebhooks() {
    String baseUrl = wireMockServer.getRuntimeInfo().getHttpBaseUrl();
    createWebhook(baseUrl + "/info-one", true);
    createWebhook(baseUrl + "/info-two", true);
    createWebhook(baseUrl + "/info-inactive", false);

    wireMockServer.stubFor(post(urlEqualTo("/info-one")));
    wireMockServer.stubFor(post(urlEqualTo("/info-two")));

    eventPublisher.publishEvent(new InformationSubmissionEvent(this, "negotiation-3"));

    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(
            () -> {
              wireMockServer.verify(
                  1,
                  postRequestedFor(urlEqualTo("/info-one"))
                      .withHeader(
                          WebhookHeaders.EVENT_TYPE, equalTo("InformationSubmissionEvent")));
              wireMockServer.verify(
                  1,
                  postRequestedFor(urlEqualTo("/info-two"))
                      .withHeader(
                          WebhookHeaders.EVENT_TYPE, equalTo("InformationSubmissionEvent")));
              wireMockServer.verify(0, postRequestedFor(urlEqualTo("/info-inactive")));
            });
  }

  private Webhook createWebhook(String url, boolean active) {
    return webhookRepository.save(new Webhook(url, true, active));
  }
}
