package eu.bbmri_eric.negotiator.config;

import eu.bbmri_eric.negotiator.user.Person;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import jakarta.transaction.Transactional;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@Profile("test")
public class MockUserDetailsService implements UserDetailsService {
  private final PersonRepository personRepository;

  public MockUserDetailsService(PersonRepository personRepository) {
    this.personRepository = personRepository;
  }

  @Override
  @Transactional
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    Person person =
        personRepository
            .findByName(username)
            .orElseThrow(() -> new UsernameNotFoundException(username));
    return new MockUserPrincipal(person);
  }
}
