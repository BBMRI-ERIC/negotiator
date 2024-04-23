package eu.bbmri_eric.negotiator.integration.service;

import eu.bbmri_eric.negotiator.NegotiatorApplication;
import eu.bbmri_eric.negotiator.configuration.security.auth.NegotiatorUserDetailsService;
import eu.bbmri_eric.negotiator.database.repository.PersonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = NegotiatorApplication.class)
@ActiveProfiles("test")
public class NegotiatorUserDetailServiceTest {

  @Autowired NegotiatorUserDetailsService negotiatorUserDetailsService;
  @Autowired PersonRepository personRepository;
}
