package eu.bbmri_eric.negotiator.common.configuration.security.oauth2;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CacheManager {

  @Scheduled(cron = "0 */5 * ? * *")
  protected void clearCache() {
    CustomJWTAuthConverter.cleanCache();
    IntrospectionValidator.cleanCache();
  }
}
