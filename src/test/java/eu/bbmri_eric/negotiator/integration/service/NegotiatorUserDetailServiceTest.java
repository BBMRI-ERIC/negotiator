package eu.bbmri_eric.negotiator.integration.service;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import eu.bbmri_eric.negotiator.NegotiatorApplication;
import eu.bbmri_eric.negotiator.configuration.security.auth.HttpBasicUserDetails;
import eu.bbmri_eric.negotiator.configuration.security.auth.NegotiatorUserDetailsService;
import eu.bbmri_eric.negotiator.database.model.Person;
import eu.bbmri_eric.negotiator.database.repository.PersonRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = NegotiatorApplication.class)
@ActiveProfiles("test")
public class NegotiatorUserDetailServiceTest {

  @Autowired NegotiatorUserDetailsService negotiatorUserDetailsService;
  @Autowired PersonRepository personRepository;

  // TODO: Fix basic auth
  @Test
  @Disabled
  public void testIsNotAuthenticated_whenPasswordIsMissing() throws Exception {

    // First check that the person exist in the db but doesn't have password assigned
    Person p = personRepository.findByName("test_biobank_manager").orElse(null);
    assertNotNull(p);
    assertNull(p.getPassword());

    // Then check that authentication fails
    assertThrows(
        UsernameNotFoundException.class,
        () -> {
          negotiatorUserDetailsService.loadUserByUsername("test_biobank_manager");
        });
  }

  @Test
  public void testIsAuthenticated_whenPasswordIsPresent() throws Exception {

    // First check that the person exist in the db but doesn't have password assigned
    Person p = personRepository.findByName("researcher").orElse(null);
    assertNotNull(p);
    assertNotNull(p.getPassword());

    assertInstanceOf(
        HttpBasicUserDetails.class, negotiatorUserDetailsService.loadUserByUsername("researcher"));
  }
}