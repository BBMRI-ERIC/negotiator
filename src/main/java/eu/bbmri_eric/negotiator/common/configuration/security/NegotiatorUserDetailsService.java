package eu.bbmri_eric.negotiator.common.configuration.security;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class NegotiatorUserDetailsService {

  public static Long getCurrentlyAuthenticatedUserInternalId() throws ClassCastException {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return ((NegotiatorPrincipal) auth.getPrincipal()).getPerson().getId();
  }

  public static boolean isCurrentlyAuthenticatedUserAdmin() {
    return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
  }

  public static List<String> getRoles() {
    return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .collect(Collectors.toList());
  }
}
