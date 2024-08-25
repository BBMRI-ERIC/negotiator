package eu.bbmri_eric.negotiator.common;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Retrieving information about the currently authenticated User/Principal. */
@Component
public class AuthenticatedUserContext {
  /**
   * Retrieve an internal identifier for the currently Authenticated user.
   *
   * @return the internal identifier
   */
  public static Long getCurrentlyAuthenticatedUserInternalId() throws ClassCastException {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return ((NegotiatorPrincipal) auth.getPrincipal()).getPerson().getId();
  }

  /**
   * Check if the currently authenticated user has role Administrator.
   *
   * @return true or false
   */
  public static boolean isCurrentlyAuthenticatedUserAdmin() {
    return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
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
