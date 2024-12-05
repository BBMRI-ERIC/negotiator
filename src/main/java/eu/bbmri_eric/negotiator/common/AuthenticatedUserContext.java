package eu.bbmri_eric.negotiator.common;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Retrieving information about the currently authenticated User/Principal. */
@Component
@CommonsLog
public class AuthenticatedUserContext {
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
}
