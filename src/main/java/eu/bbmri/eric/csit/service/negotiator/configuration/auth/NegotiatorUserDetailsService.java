package eu.bbmri.eric.csit.service.negotiator.configuration.auth;

import eu.bbmri.eric.csit.service.negotiator.database.model.Person;
import eu.bbmri.eric.csit.service.negotiator.database.repository.PersonRepository;
import javax.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class NegotiatorUserDetailsService implements UserDetailsService {

  @Autowired
  private PersonRepository personRepository;

  @Override
  @Transactional
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    Person person =
        personRepository
            .findByAuthNameAndPasswordNotNull(username)
            .orElseThrow(() -> new UsernameNotFoundException(username));
    return new HttpBasicUserDetails(person);
  }

  public static String getCurrentlyAuthenticatedUserId(){
    return SecurityContextHolder.getContext().getAuthentication().getName();
  }
}
