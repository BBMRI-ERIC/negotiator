package eu.bbmri_eric.negotiator.notification;

import static org.hamcrest.core.Is.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import eu.bbmri_eric.negotiator.util.WithMockNegotiatorUser;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest(loadTestData = true)
@AutoConfigureMockMvc
public class NotificationControllerTest {

  public static final String USER_NOTIFICATIONS_ENDPOINT = "/v3/users/%s/notifications";
  @Autowired private NotificationRepository notificationRepository;
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
    notificationRepository.save(new Notification(101L, "test Title", "test message"));
    notificationRepository.save(new Notification(101L, "test Title 2", "test message"));
    notificationRepository.save(new Notification(101L, "test Title 3", "test message"));

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
    notificationRepository.save(new Notification(101L, "test Title", "test message"));
    notificationRepository.save(new Notification(101L, "test Title 2", "test message"));
    notificationRepository.save(new Notification(101L, "test Title 3", "test message"));
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
    Notification notification =
        notificationRepository.save(new Notification(101L, "test Title", "test message"));
    mockMvc
        .perform(get("/v3/notifications/%s".formatted(notification.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message", is("test message")))
        .andExpect(jsonPath("$.title", is("test Title")));
  }

  @Test
  @WithMockNegotiatorUser(id = 101L)
  void updateNotifications_validRequest_ok() throws Exception {
    Notification notification1 =
        notificationRepository.save(new Notification(101L, "test Title 1", "test message 1"));
    Notification notification2 =
        notificationRepository.save(new Notification(101L, "test Title 2", "test message 2"));
    List<NotificationUpdateDTO> updates =
        Arrays.asList(
            NotificationUpdateDTO.builder().id(notification1.getId()).read(true).build(),
            NotificationUpdateDTO.builder().id(notification2.getId()).read(true).build());
    ObjectMapper objectMapper = new ObjectMapper();
    String updatesJson = objectMapper.writeValueAsString(updates);
    mockMvc
        .perform(
            patch(USER_NOTIFICATIONS_ENDPOINT.formatted("101"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatesJson))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.notifications[0].read", is(true)))
        .andExpect(jsonPath("$._embedded.notifications[1].read", is(true)));
  }

  @Test
  @WithMockNegotiatorUser(id = 101L)
  void updateNotifications_unauthorizedNotification_throwsForbidden() throws Exception {
    // Create notification for a different user
    Notification otherUserNotification =
        notificationRepository.save(new Notification(102L, "test Title", "test message"));

    List<NotificationUpdateDTO> updates =
        List.of(
            NotificationUpdateDTO.builder().id(otherUserNotification.getId()).read(true).build());

    ObjectMapper objectMapper = new ObjectMapper();
    String updatesJson = objectMapper.writeValueAsString(updates);

    mockMvc
        .perform(
            patch(USER_NOTIFICATIONS_ENDPOINT.formatted("101"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatesJson))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockNegotiatorUser(id = 101L)
  void updateNotifications_nonExistentNotification_throwsNotFound() throws Exception {
    List<NotificationUpdateDTO> updates =
        List.of(NotificationUpdateDTO.builder().id(99999L).read(true).build());

    ObjectMapper objectMapper = new ObjectMapper();
    String updatesJson = objectMapper.writeValueAsString(updates);

    mockMvc
        .perform(
            patch(USER_NOTIFICATIONS_ENDPOINT.formatted("101"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatesJson))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockNegotiatorUser(id = 101L)
  void updateNotifications_emptyList_ok() throws Exception {
    List<NotificationUpdateDTO> updates = List.of();

    ObjectMapper objectMapper = new ObjectMapper();
    String updatesJson = objectMapper.writeValueAsString(updates);

    mockMvc
        .perform(
            patch(USER_NOTIFICATIONS_ENDPOINT.formatted("101"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatesJson))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isEmpty());
  }

  @Test
  @WithMockNegotiatorUser(id = 101L)
  void updateNotifications_invalidJson_throwsBadRequest() throws Exception {
    mockMvc
        .perform(
            patch(USER_NOTIFICATIONS_ENDPOINT.formatted("101"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"invalid\": \"json\""))
        .andExpect(status().isBadRequest());
  }
}
