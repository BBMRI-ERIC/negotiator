package eu.bbmri.eric.csit.service.negotiator.configuration.auth;

import eu.bbmri.eric.csit.service.negotiator.database.model.Person;
import eu.bbmri.eric.csit.service.negotiator.database.repository.PersonRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class NegotiatorUserDetailsService implements UserDetailsService {

  @Autowired private PersonRepository personRepository;

  public static Long getCurrentlyAuthenticatedUserInternalId() throws ClassCastException {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return ((NegotiatorUserDetails) auth.getPrincipal()).getPerson().getId();
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

  @Override
  @Transactional
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    Person person =
        personRepository
            .findByName(username)
            .orElseThrow(() -> new UsernameNotFoundException(username));
    return new HttpBasicUserDetails(person);
  }
}
