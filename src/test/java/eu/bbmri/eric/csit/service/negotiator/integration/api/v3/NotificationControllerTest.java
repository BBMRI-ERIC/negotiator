package eu.bbmri.eric.csit.service.negotiator.integration.api.v3;

import static org.hamcrest.core.Is.is;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.bbmri.eric.csit.service.negotiator.NegotiatorApplication;
import eu.bbmri.eric.csit.service.negotiator.database.model.Negotiation;
import eu.bbmri.eric.csit.service.negotiator.database.model.Notification;
import eu.bbmri.eric.csit.service.negotiator.database.model.Person;
import eu.bbmri.eric.csit.service.negotiator.database.repository.NegotiationRepository;
import eu.bbmri.eric.csit.service.negotiator.database.repository.NotificationRepository;
import eu.bbmri.eric.csit.service.negotiator.database.repository.PersonRepository;
import lombok.extern.apachecommons.CommonsLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(classes = NegotiatorApplication.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@CommonsLog
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
