package eu.bbmri_eric.negotiator.webhook.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.negotiator.info_submission.InformationSubmissionEvent;
import eu.bbmri_eric.negotiator.negotiation.NewNegotiationEvent;
import eu.bbmri_eric.negotiator.negotiation.NewResourcesAddedEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationStateChangeEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceState;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.ResourceStateChangeEvent;
import eu.bbmri_eric.negotiator.post.NewPostEvent;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEvent;

class WebhookEventMapperTest {

  private WebhookEventMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new WebhookEventMapper(new ObjectMapper());
  }

  @Test
  void map_whenEventIsUnsupported_returnsEmpty() {
    ApplicationEvent unsupportedEvent = new ApplicationEvent(this) {};

    Optional<WebhookEventEnvelope<?>> mapped = mapper.map(unsupportedEvent);

    assertThat(mapped).isEmpty();
  }

  @Test
  void map_whenInformationSubmissionEvent_returnsStableEventTypeAndData() {
    InformationSubmissionEvent event = new InformationSubmissionEvent(this, "negotiation-1");

    Optional<WebhookEventEnvelope<?>> mapped = mapper.map(event);

    assertThat(mapped).isPresent();
    assertThat(mapped.get().eventType()).isEqualTo(WebhookEventType.NEGOTIATION_INFO_UPDATED);
    assertThat(mapped.get().occurredAt()).isNotNull();
    assertThat(mapped.get().data())
        .isEqualTo(new NegotiationInfoUpdatedWebhookEvent("negotiation-1"));
  }

  @Test
  void map_whenNegotiationStateChangeEvent_returnsStableEventTypeAndData() {
    NegotiationStateChangeEvent event =
        new NegotiationStateChangeEvent(
            this,
            "negotiation-2",
            NegotiationState.DRAFT,
            NegotiationState.SUBMITTED,
            NegotiationEvent.SUBMIT,
            "post");

    Optional<WebhookEventEnvelope<?>> mapped = mapper.map(event);

    assertThat(mapped).isPresent();
    assertThat(mapped.get().eventType()).isEqualTo(WebhookEventType.NEGOTIATION_STATE_UPDATED);
    assertThat(mapped.get().occurredAt()).isNotNull();
    assertThat(mapped.get().data())
        .isEqualTo(
            new NegotiationStateUpdatedWebhookEvent(
                "negotiation-2",
                NegotiationState.DRAFT,
                NegotiationState.SUBMITTED,
                NegotiationEvent.SUBMIT,
                "post"));
  }

  @Test
  void map_whenNewNegotiationEvent_returnsStableEventTypeAndData() {
    NewNegotiationEvent event = new NewNegotiationEvent(this, "negotiation-3");

    Optional<WebhookEventEnvelope<?>> mapped = mapper.map(event);

    assertThat(mapped).isPresent();
    assertThat(mapped.get().eventType()).isEqualTo(WebhookEventType.NEGOTIATION_ADDED);
    assertThat(mapped.get().occurredAt()).isNotNull();
    assertThat(mapped.get().data()).isEqualTo(new NegotiationAddedWebhookEvent("negotiation-3"));
  }

  @Test
  void map_whenNewResourcesAddedEvent_returnsStableEventTypeAndData() {
    NewResourcesAddedEvent event = new NewResourcesAddedEvent(this, "negotiation-4");

    Optional<WebhookEventEnvelope<?>> mapped = mapper.map(event);

    assertThat(mapped).isPresent();
    assertThat(mapped.get().eventType()).isEqualTo(WebhookEventType.NEGOTIATION_RESOURCE_ADDED);
    assertThat(mapped.get().occurredAt()).isNotNull();
    assertThat(mapped.get().data())
        .isEqualTo(new NegotiationResourceUpdatedWebhookEvent("negotiation-4"));
  }

  @Test
  void map_whenNewPostEvent_returnsStableEventTypeAndData() {
    NewPostEvent event = new NewPostEvent(this, "post-1", "negotiation-5", 1000L, 2000L);

    Optional<WebhookEventEnvelope<?>> mapped = mapper.map(event);

    assertThat(mapped).isPresent();
    assertThat(mapped.get().eventType()).isEqualTo(WebhookEventType.NEGOTIATION_POST_ADDED);
    assertThat(mapped.get().occurredAt()).isNotNull();
    assertThat(mapped.get().data())
        .isEqualTo(new NegotiationPostAddedWebhookEvent("post-1", "negotiation-5", 1000L, 2000L));
  }

  @Test
  void map_whenResourceStateChangeEvent_returnsStableEventTypeAndData() {
    ResourceStateChangeEvent event =
        new ResourceStateChangeEvent(
            this,
            "negotiation-5",
            "resource-1",
            NegotiationResourceState.SUBMITTED,
            NegotiationResourceState.RESOURCE_AVAILABLE);

    Optional<WebhookEventEnvelope<?>> mapped = mapper.map(event);

    assertThat(mapped).isPresent();
    assertThat(mapped.get().eventType())
        .isEqualTo(WebhookEventType.NEGOTIATION_RESOURCE_STATE_UPDATED);
    assertThat(mapped.get().occurredAt()).isNotNull();
    assertThat(mapped.get().data())
        .isEqualTo(
            new NegotiationResourceStateUpdatedWebhookEvent(
                "negotiation-5",
                "resource-1",
                NegotiationResourceState.SUBMITTED,
                NegotiationResourceState.RESOURCE_AVAILABLE));
  }
}
