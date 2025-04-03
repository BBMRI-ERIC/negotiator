package eu.bbmri_eric.negotiator.integration.api.v3;

import static org.hamcrest.core.Is.is;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.notification.Notification;
import eu.bbmri_eric.negotiator.notification.NotificationRepository;
import eu.bbmri_eric.negotiator.user.Person;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@IntegrationTest(loadTestData = true)
public class NotificationControllerTest {

  @Autowired private WebApplicationContext context;
  @Autowired private NotificationRepository notificationRepository;
  @Autowired private PersonRepository personRepository;
  @Autowired private NegotiationRepository negotiationRepository;
  private MockMvc mockMvc;

  @BeforeEach
  public void beforeEach() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @Test
  @WithUserDetails("TheResearcher")
  void getNotifications_empty() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("/v3/notifications"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().string("[]"));
  }

  @Test
  @WithUserDetails("TheResearcher")
  void getNotifications_3new_Ok() throws Exception {
    Person person = personRepository.findByName("TheResearcher").get();
    Negotiation negotiation = negotiationRepository.findById("negotiation-1").get();
    for (int i = 0; i < 3; i++) {
      notificationRepository.save(
          Notification.builder().recipient(person).negotiation(negotiation).build());
    }
    mockMvc
        .perform(MockMvcRequestBuilders.get("/v3/notifications"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.length()", is(3)));
  }
}
