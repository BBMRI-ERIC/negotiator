package eu.bbmri_eric.negotiator.webhook;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "negotiator.webhook.timeout")
class WebhookTimeoutProperties {
  private Duration connect = Duration.ofSeconds(2);
  private Duration read = Duration.ofSeconds(8);
}
