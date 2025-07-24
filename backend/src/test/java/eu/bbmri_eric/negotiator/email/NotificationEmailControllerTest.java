package eu.bbmri_eric.negotiator.email;

import static org.hamcrest.core.Is.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.bbmri_eric.negotiator.util.IntegrationTest;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest(loadTestData = true)
@AutoConfigureMockMvc
public class NotificationEmailControllerTest {

  public static final String EMAILS_ENDPOINT = "/v3/emails";
  public static final String EMAIL_BY_ID_ENDPOINT = "/v3/emails/%s";

  @Autowired private NotificationEmailRepository notificationEmailRepository;
  @Autowired private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    notificationEmailRepository.deleteAll();
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getAllEmails_noFilters_ok() throws Exception {
    NotificationEmail email1 = createTestEmail("user1@example.com", "Test message 1");
    NotificationEmail email2 = createTestEmail("user2@example.com", "Test message 2");
    NotificationEmail email3 = createTestEmail("user3@example.com", "Test message 3");

    notificationEmailRepository.save(email1);
    notificationEmailRepository.save(email2);
    notificationEmailRepository.save(email3);

    mockMvc
        .perform(get(EMAILS_ENDPOINT))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.notificationEmails.length()").exists())
        .andExpect(jsonPath("$.page.size", is(20)))
        .andExpect(jsonPath("$.page.number", is(0)));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getAllEmails_withAddressFilter_ok() throws Exception {
    NotificationEmail email1 = createTestEmail("test@example.com", "Test message 1");
    NotificationEmail email2 = createTestEmail("other@domain.com", "Test message 2");

    notificationEmailRepository.save(email1);
    notificationEmailRepository.save(email2);

    mockMvc
        .perform(get(EMAILS_ENDPOINT + "?address=test@example.com"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.notificationEmails[0].address", is("test@example.com")));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getAllEmails_withPagination_ok() throws Exception {
    for (int i = 0; i < 25; i++) {
      NotificationEmail email = createTestEmail("user" + i + "@example.com", "Test message " + i);
      notificationEmailRepository.save(email);
    }

    mockMvc
        .perform(get(EMAILS_ENDPOINT + "?page=0&size=10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page.size", is(10)))
        .andExpect(jsonPath("$.page.number", is(0)))
        .andExpect(jsonPath("$._embedded.notificationEmails.length()", is(10)));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getAllEmails_withSorting_ok() throws Exception {
    NotificationEmail email1 =
        createTestEmailWithTime("user1@example.com", "Message 1", LocalDateTime.now().minusDays(2));
    NotificationEmail email2 =
        createTestEmailWithTime("user2@example.com", "Message 2", LocalDateTime.now().minusDays(1));

    notificationEmailRepository.save(email1);
    notificationEmailRepository.save(email2);

    mockMvc
        .perform(get(EMAILS_ENDPOINT + "?sort=sentAt,asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.notificationEmails[0].address", is("user1@example.com")));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getAllEmails_withDateFilter_ok() throws Exception {
    LocalDateTime pastDate = LocalDateTime.now().minusDays(5);
    LocalDateTime recentDate = LocalDateTime.now().minusDays(1);

    NotificationEmail oldEmail =
        createTestEmailWithTime("old@example.com", "Old message", pastDate);
    NotificationEmail recentEmail =
        createTestEmailWithTime("recent@example.com", "Recent message", recentDate);

    notificationEmailRepository.save(oldEmail);
    notificationEmailRepository.save(recentEmail);

    String filterDate = LocalDateTime.now().minusDays(2).toString();
    String futureDate = LocalDateTime.now().plusDays(2).toString();

    mockMvc
        .perform(get(EMAILS_ENDPOINT + "?sentAfter=" + filterDate))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.notificationEmails.length()", is(1)))
        .andExpect(jsonPath("$._embedded.notificationEmails[0].address", is("recent@example.com")));
    mockMvc
        .perform(get(EMAILS_ENDPOINT + "?sentBefore=" + filterDate))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.notificationEmails.length()", is(1)))
        .andExpect(jsonPath("$._embedded.notificationEmails[0].address", is("old@example.com")));
    mockMvc
        .perform(
            get(
                EMAILS_ENDPOINT
                    + "?sort=sentAt,desc&sentAfter=%s&sentBefore=%s"
                        .formatted(filterDate, futureDate)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.notificationEmails.length()", is(1)))
        .andExpect(jsonPath("$._embedded.notificationEmails[0].address", is("recent@example.com")));
    mockMvc
        .perform(get(EMAILS_ENDPOINT + "?sentAfter=%s".formatted(futureDate)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page.totalElements", is(0)));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getEmailById_validId_ok() throws Exception {
    NotificationEmail email = createTestEmail("test@example.com", "Test message");
    NotificationEmail savedEmail = notificationEmailRepository.save(email);

    mockMvc
        .perform(get(EMAIL_BY_ID_ENDPOINT.formatted(savedEmail.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.address", is("test@example.com")))
        .andExpect(jsonPath("$.message", is("Test message")))
        .andExpect(jsonPath("$.id", is(savedEmail.getId().intValue())));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getEmailById_nonExistentId_throws404() throws Exception {
    mockMvc.perform(get(EMAIL_BY_ID_ENDPOINT.formatted("99999"))).andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getAllEmails_invalidPageParams_throws400() throws Exception {
    mockMvc.perform(get(EMAILS_ENDPOINT + "?page=-1")).andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getAllEmails_invalidSizeParams_throws400() throws Exception {
    mockMvc.perform(get(EMAILS_ENDPOINT + "?size=0")).andExpect(status().isBadRequest());
  }

  @Test
  void getAllEmails_unauthenticated_throws401() throws Exception {
    mockMvc.perform(get(EMAILS_ENDPOINT)).andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser
  void getEmailById_unauthorized_throws403() throws Exception {
    mockMvc.perform(get(EMAIL_BY_ID_ENDPOINT.formatted("1"))).andExpect(status().isForbidden());
  }

  private NotificationEmail createTestEmail(String address, String message) {
    return createTestEmailWithTime(address, message, LocalDateTime.now());
  }

  private NotificationEmail createTestEmailWithTime(
      String address, String message, LocalDateTime sentAt) {
    NotificationEmail email = new NotificationEmail();
    email.setAddress(address);
    email.setMessage(message);
    email.setSentAt(sentAt);
    return email;
  }
}
