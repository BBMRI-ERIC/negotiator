package eu.bbmri_eric.negotiator.integration.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import eu.bbmri_eric.negotiator.user.LastLoginListener;
import eu.bbmri_eric.negotiator.user.Person;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

class LastLoginListenerTest {

  private PersonRepository personRepository;
  private LastLoginListener lastLoginListener;

  @BeforeEach
  void setUp() {
    personRepository = mock(PersonRepository.class);
    lastLoginListener = new LastLoginListener(personRepository);
  }

  @Test
  void testHandleAuthenticationSuccess_FirstLogin_UpdatesLastLogin() {
    String subjectId = "test-user";
    LocalDateTime before = LocalDateTime.now();

    OidcUser oidcUser = mock(OidcUser.class);
    when(oidcUser.getClaim("sub")).thenReturn(subjectId);
    when(oidcUser.getSubject()).thenReturn(subjectId);

    Authentication auth = mock(Authentication.class);
    when(auth.getPrincipal()).thenReturn(oidcUser);

    AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(auth);

    Person person = new Person();
    person.setSubjectId(subjectId);

    when(personRepository.findBySubjectId(subjectId)).thenReturn(Optional.of(person));

    lastLoginListener.handleAuthenticationSuccess(event);

    assertNotNull(person.getLastLogin());
    assertFalse(person.getLastLogin().isBefore(before), "Last login should be recent");

    verify(personRepository).saveAndFlush(person);
  }

  @Test
  void testHandleAuthenticationSuccess_PersonNotFound_DoesNothing() {
    String subjectId = "missing-user";

    OidcUser oidcUser = mock(OidcUser.class);
    when(oidcUser.getClaim("sub")).thenReturn(subjectId);

    Authentication auth = mock(Authentication.class);
    when(auth.getPrincipal()).thenReturn(oidcUser);

    when(personRepository.findBySubjectId(subjectId)).thenReturn(Optional.empty());

    AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(auth);

    lastLoginListener.handleAuthenticationSuccess(event);

    verify(personRepository, never()).save(any());
  }

  @Test
  void testHandleAuthenticationSuccess_TooSoon_DoesNotUpdate() {
    String subjectId = "test-user";

    // Cache says we recently updated
    lastLoginListener.lastUpdateCache.put(subjectId, LocalDateTime.now().minusMinutes(2));

    OidcUser oidcUser = mock(OidcUser.class);
    when(oidcUser.getClaim("sub")).thenReturn(subjectId);

    Authentication auth = mock(Authentication.class);
    when(auth.getPrincipal()).thenReturn(oidcUser);

    AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(auth);

    lastLoginListener.handleAuthenticationSuccess(event);

    verify(personRepository, never()).findBySubjectId(any());
    verify(personRepository, never()).save(any());
  }

  @Test
  void testHandleAuthenticationSuccess_NonOidcUser_DoesNothing() {
    Authentication auth = mock(Authentication.class);
    when(auth.getPrincipal()).thenReturn("some-string");

    AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(auth);
    lastLoginListener.handleAuthenticationSuccess(event);

    verifyNoInteractions(personRepository);
  }

  @Test
  void testCleanupCache_RemovesExpiredEntries() {
    String user1 = "user1";
    String user2 = "user2";

    LocalDateTime now = LocalDateTime.now();
    lastLoginListener.lastUpdateCache.put(user1, now.minusMinutes(15)); // expired
    lastLoginListener.lastUpdateCache.put(user2, now.minusMinutes(5)); // valid

    lastLoginListener.cleanupCache();

    assertFalse(lastLoginListener.lastUpdateCache.containsKey(user1));
    assertTrue(lastLoginListener.lastUpdateCache.containsKey(user2));
  }
}
