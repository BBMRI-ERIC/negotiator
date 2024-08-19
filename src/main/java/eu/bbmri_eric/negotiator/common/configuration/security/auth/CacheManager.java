package eu.bbmri_eric.negotiator.common.configuration.security.auth;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
public class CacheManager {

  @Scheduled(cron = "0 */5 * ? * *")
  @Async
  protected void clearCache() {
    CustomJWTAuthConverter.cleanCache();
    IntrospectionValidator.cleanCache();
  }
}
