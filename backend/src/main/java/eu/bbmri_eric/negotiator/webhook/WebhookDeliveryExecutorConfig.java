package eu.bbmri_eric.negotiator.webhook;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Provides the executor used for async per-endpoint webhook delivery tasks. */
@Configuration
class WebhookDeliveryExecutorConfig {

  @Bean(name = "webhookDeliveryExecutor")
  Executor webhookDeliveryExecutor() {
    return Executors.newVirtualThreadPerTaskExecutor();
  }
}
