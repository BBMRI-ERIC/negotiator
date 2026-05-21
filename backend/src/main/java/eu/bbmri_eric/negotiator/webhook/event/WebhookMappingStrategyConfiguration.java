package eu.bbmri_eric.negotiator.webhook.event;

import eu.bbmri_eric.negotiator.info_submission.InformationSubmissionEvent;
import eu.bbmri_eric.negotiator.negotiation.NewResourcesAddedEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.ResourceStateChangeEvent;
import eu.bbmri_eric.negotiator.post.NewPostEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class WebhookMappingStrategyConfiguration {

  @Bean
  WebhookMappingStrategy<InformationSubmissionEvent> informationSubmissionWebhookMappingStrategy() {
    return DefaultWebhookMappingStrategy.of(
        InformationSubmissionEvent.class,
        NegotiationInfoUpdatedWebhookEvent.class,
        WebhookEventType.NEGOTIATION_INFO_UPDATED);
  }

  @Bean
  WebhookMappingStrategy<NewResourcesAddedEvent> newResourcesAddedWebhookMappingStrategy() {
    return DefaultWebhookMappingStrategy.of(
        NewResourcesAddedEvent.class,
        NegotiationResourceUpdatedWebhookEvent.class,
        WebhookEventType.NEGOTIATION_RESOURCE_ADDED);
  }

  @Bean
  WebhookMappingStrategy<NewPostEvent> newPostWebhookMappingStrategy() {
    return DefaultWebhookMappingStrategy.of(
        NewPostEvent.class,
        NegotiationPostAddedWebhookEvent.class,
        WebhookEventType.NEGOTIATION_POST_ADDED);
  }

  @Bean
  WebhookMappingStrategy<ResourceStateChangeEvent> resourceStateChangeWebhookMappingStrategy() {
    return DefaultWebhookMappingStrategy.of(
        ResourceStateChangeEvent.class,
        NegotiationResourceStateUpdatedWebhookEvent.class,
        WebhookEventType.NEGOTIATION_RESOURCE_STATE_UPDATED);
  }
}
