package eu.bbmri_eric.negotiator.common;

import eu.bbmri_eric.negotiator.user.Person;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Retrieving information about the currently authenticated User/Principal. */
@Component
@CommonsLog
public class AuthenticatedUserContext {

  private final PersonRepository personRepository;

  public AuthenticatedUserContext(PersonRepository personRepository) {
    this.personRepository = personRepository;
  }

  /**
   * Retrieve an internal identifier for the currently Authenticated user.
   *
   * @return the internal identifier
   */
  public static Long getCurrentlyAuthenticatedUserInternalId()
      throws AuthenticationCredentialsNotFoundException {
    try {
      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      return ((NegotiatorPrincipal) auth.getPrincipal()).getPerson().getId();
    } catch (Exception e) {
      log.error(e.getMessage());
      throw new AuthenticationCredentialsNotFoundException("No authenticated user found");
    }
  }

  /**
   * Method for temporary elevation of auth, useful for running automatic operations.
   *
   * @param task a runnable method
   */
  public void runAsSystemUser(Runnable task) {
    SecurityContext originalContext = SecurityContextHolder.getContext();
    try {
      Person person = personRepository.findById(0L).get();
      Authentication systemAuth =
          new UsernamePasswordAuthenticationToken(
              new UserPrincipal(person), null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
      SecurityContext newContext = SecurityContextHolder.createEmptyContext();
      newContext.setAuthentication(systemAuth);
      SecurityContextHolder.setContext(newContext);
      task.run();
    } catch (NoSuchElementException e) {
      log.error("Could not execute automated actions as the system user does not exist");
    } finally {
      SecurityContextHolder.setContext(originalContext);
    }
  }

  /**
   * Check if the currently authenticated user has role Administrator.
   *
   * @return true or false
   */
  public static boolean isCurrentlyAuthenticatedUserAdmin() {
    return !Objects.isNull(SecurityContextHolder.getContext().getAuthentication())
        && SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
  }

  /**
   * Retrieve roles associated with the currently authenticated user.
   *
   * @return a list of roles, e.g. REPRESENTATIVE.
   */
  public static List<String> getRoles() {
    return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .collect(Collectors.toList());
  }

  /**
   * Checks if user is allowed to access resource based on their role or if they are accessing their
   * own
   *
   * @param personId the id of the person to be checked for authorization.
   */
  public static void checkUserAccess(Long personId) {
    final List<String> roles = getRoles();
    if (roles.contains("ROLE_ADMIN") || roles.contains("ROLE_AUTHORIZATION_MANAGER")) {
      return;
    }
    try {
      final Long currentId = getCurrentlyAuthenticatedUserInternalId();
      if (!Objects.equals(currentId, personId)) {
        log.warn("Forbidden access: user " + currentId + " attempted to act on user " + personId);
        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
      }
    } catch (AuthenticationCredentialsNotFoundException ex) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
  }
}
