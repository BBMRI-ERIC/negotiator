package eu.bbmri_eric.negotiator.integration.service;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import eu.bbmri_eric.negotiator.NegotiatorApplication;
import eu.bbmri_eric.negotiator.configuration.security.auth.HttpBasicUserDetails;
import eu.bbmri_eric.negotiator.configuration.security.auth.NegotiatorUserDetailsService;
import eu.bbmri_eric.negotiator.database.model.Person;
import eu.bbmri_eric.negotiator.database.repository.PersonRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = NegotiatorApplication.class)
@ActiveProfiles("test")
public class NegotiatorUserDetailServiceTest {

  @Autowired NegotiatorUserDetailsService negotiatorUserDetailsService;
  @Autowired PersonRepository personRepository;

  // TODO: Fix basic auth

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
