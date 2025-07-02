package eu.bbmri_eric.negotiator.integration.api.v3;

import static org.hamcrest.core.Is.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.notification.Notification;
import eu.bbmri_eric.negotiator.notification.NotificationRepository;
import eu.bbmri_eric.negotiator.user.Person;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import eu.bbmri_eric.negotiator.util.WithMockNegotiatorUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

@IntegrationTest(loadTestData = true)
@AutoConfigureMockMvc
public class NotificationControllerTest {

  public static final String USER_NOTIFICATIONS_ENDPOINT = "/v3/users/%s/notifications";
  @Autowired private WebApplicationContext context;
  @Autowired private NotificationRepository notificationRepository;
  @Autowired private PersonRepository personRepository;
  @Autowired private NegotiationRepository negotiationRepository;
  @Autowired private MockMvc mockMvc;

  @Test
  @WithMockNegotiatorUser(id = 101L)
  void getUserNotifications_nonExistentUSerID_throws403() throws Exception {
    mockMvc
        .perform(get(USER_NOTIFICATIONS_ENDPOINT.formatted("199999")))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockNegotiatorUser(id = 101L)
  void getUserNotifications_differentUserId_throws403() throws Exception {
    mockMvc
        .perform(get(USER_NOTIFICATIONS_ENDPOINT.formatted("102")))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockNegotiatorUser(id = 101L)
  void getUserNotifications_validID_ok() throws Exception {
    Person person = personRepository.findById(101L).get();
    notificationRepository.save(new Notification(person, "test Title", "test message"));
    notificationRepository.save(new Notification(person, "test Title 2", "test message"));
    notificationRepository.save(new Notification(person, "test Title 3", "test message"));

    mockMvc
        .perform(get(USER_NOTIFICATIONS_ENDPOINT.formatted("101")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.notifications[0].message", is("test message")))
        .andExpect(jsonPath("$._embedded.notifications[0].title", is("test Title 3")))
        .andExpect(jsonPath("$._embedded.notifications[1].title", is("test Title 2")))
        .andExpect(jsonPath("$._embedded.notifications[2].title", is("test Title")));
  }

  @Test
  @WithMockNegotiatorUser(id = 101L)
  void getUserNotifications_withPagination_ok() throws Exception {
    Person person = personRepository.findById(101L).get();
    notificationRepository.save(new Notification(person, "test Title", "test message"));
    notificationRepository.save(new Notification(person, "test Title 2", "test message"));
    notificationRepository.save(new Notification(person, "test Title 3", "test message"));
    mockMvc
            .perform(get(USER_NOTIFICATIONS_ENDPOINT.formatted("101") + "?page=0&size=1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.notifications[0].message", is("test message")))
            .andExpect(jsonPath("$._embedded.notifications[0].title", is("test Title 3")))
            .andExpect(jsonPath("$._embedded.notifications.length()", is(1)));
  }

  @Test
  @WithMockNegotiatorUser(id = 101L)
  void getNotification_validID_ok() throws Exception {
    Person person = personRepository.findById(101L).get();
    Notification notification =
        notificationRepository.save(new Notification(person, "test Title", "test message"));
    mockMvc
        .perform(get("/v3/notifications/%s".formatted(notification.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message", is("test message")))
        .andExpect(jsonPath("$.title", is("test Title")));
  }
}
