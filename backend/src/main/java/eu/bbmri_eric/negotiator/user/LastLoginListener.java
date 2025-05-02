package eu.bbmri_eric.negotiator.user;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@CommonsLog
public class LastLoginListener {

  private final PersonRepository personRepository;

  public final Map<String, LocalDateTime> lastUpdateCache = new ConcurrentHashMap<>();
  private static final long UPDATE_INTERVAL_MINUTES = 5;

  public LastLoginListener(PersonRepository personRepository) {
    this.personRepository = personRepository;
  }

  @EventListener
  @Transactional
  public void handleAuthenticationSuccess(AuthenticationSuccessEvent event) {
    Authentication authentication = event.getAuthentication();
    Object principal = authentication.getPrincipal();
    String subjectId;

    if (principal instanceof OidcUser oidcUser) {
      subjectId = oidcUser.getSubject();
    } else if (authentication instanceof JwtAuthenticationToken jwtAuth) {
      subjectId = jwtAuth.getToken().getSubject();
    } else {
      subjectId = null;
    }
    if (subjectId != null) {
      LocalDateTime now = LocalDateTime.now();
      LocalDateTime lastUpdate = lastUpdateCache.get(subjectId);
      if (lastUpdate == null || lastUpdate.plusMinutes(UPDATE_INTERVAL_MINUTES).isBefore(now)) {
        personRepository
            .findBySubjectId(subjectId)
            .ifPresent(
                person -> {
                  log.info("USER_LOGIN: User %s logged in.".formatted(person.getName()));
                  person.setLastLogin(now);
                  personRepository.saveAndFlush(person);
                  lastUpdateCache.put(subjectId, now);
                });
      }
    }
  }

  @Scheduled(fixedRate = 30 * 60 * 1000)
  public void cleanupCache() {
    LocalDateTime now = LocalDateTime.now();
    lastUpdateCache
        .entrySet()
        .removeIf(entry -> entry.getValue().plusMinutes(UPDATE_INTERVAL_MINUTES * 2).isBefore(now));
  }
}
