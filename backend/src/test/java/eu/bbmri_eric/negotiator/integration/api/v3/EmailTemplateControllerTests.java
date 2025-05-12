package eu.bbmri_eric.negotiator.integration.api.v3;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.notification.UserNotificationService;
import eu.bbmri_eric.negotiator.notification.email.NotificationEmail;
import eu.bbmri_eric.negotiator.notification.email.NotificationEmailRepository;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import jakarta.transaction.Transactional;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@IntegrationTest(loadTestData = true)
@Transactional
public class EmailTemplateControllerTests {

  @Autowired private WebApplicationContext context;
  @Autowired NotificationEmailRepository notificationEmailRepository;
  @Autowired NegotiationRepository negotiationRepository;
  @Autowired UserNotificationService userNotificationService;

  private MockMvc mockMvc;

  @BeforeEach
  public void beforeEach() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @Test
  @WithUserDetails("admin")
  void getNotificationTemplate_validTemplateName_Ok() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("/v3/email-templates/footer"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/xhtml+xml;charset=UTF-8"));
  }

  @Test
  @WithUserDetails("admin")
  void getNotificationTemplate_invalidTemplateName_NotFound() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("/v3/email-templates/invalidTemplate"))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithUserDetails("admin")
  void updateNotificationTemplate_validTemplateName_Ok() throws Exception {
    String newTemplateContent =
        "<html>\n"
            + " <head></head>\n"
            + " <body>\n"
            + "  Updated Template Content\n"
            + " </body>\n"
            + "</html>";
    mockMvc
        .perform(
            MockMvcRequestBuilders.put("/v3/email-templates/footer")
                .contentType(MediaType.APPLICATION_XHTML_XML)
                .content(newTemplateContent))
        .andExpect(status().isOk())
        .andExpect(content().string(Jsoup.parse(newTemplateContent).toString()));
  }

  @Test
  @WithUserDetails("admin")
  void updateNotificationTemplate_invalidTemplateName_Forbidden() throws Exception {
    String newTemplateContent = "Updated Template Content";
    mockMvc
        .perform(
            MockMvcRequestBuilders.put("/v3/email-templates/invalidTemplate")
                .contentType(MediaType.APPLICATION_XHTML_XML)
                .content(newTemplateContent))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithUserDetails("admin")
  void resetNotificationTemplate_validTemplateName_Ok() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/v3/email-templates/footer/operations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"operation\": \"RESET\"}"))
        .andExpect(status().isOk());
  }

  @Test
  @WithUserDetails("admin")
  void resetNotificationTemplate_invalidContent_BadRequest() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/v3/email-templates/footer/operations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"operation\": \"INVALID\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithUserDetails("admin")
  void resetNotificationTemplate_invalidTemplateName_NotFound() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/v3/email-templates/invalidTemplate/operations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"operation\": \"RESET\"}"))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithUserDetails("admin")
  @DirtiesContext
  void templateUsedInEmailContent() throws Exception {
    String templateContent =
        "<html><body><p>WelcomeVeryUnique, <span th:utext=\"${recipient.getName()}\"></span></p></body></html>";
    mockMvc
        .perform(
            MockMvcRequestBuilders.put("/v3/email-templates/email-notification")
                .contentType(MediaType.APPLICATION_XHTML_XML)
                .content(templateContent))
        .andExpect(status().isOk());

    assertTrue(notificationEmailRepository.findAll().isEmpty());
    Negotiation negotiation = negotiationRepository.findAll().get(0);
    assertTrue(
        negotiation.getResources().stream()
            .anyMatch(resource -> !resource.getRepresentatives().isEmpty()));
    userNotificationService.notifyRepresentativesAboutNewNegotiation(negotiation);
    userNotificationService.sendEmailsForNewNotifications();
    await()
        .atMost(1, SECONDS)
        .pollInterval(100, MILLISECONDS)
        .until(() -> !notificationEmailRepository.findAll().isEmpty());
    int numOfEmails = notificationEmailRepository.findAll().size();
    assertTrue(numOfEmails > 0);
    NotificationEmail notificationEmail = notificationEmailRepository.findAll().get(0);
    assertTrue(notificationEmail.getMessage().contains("WelcomeVeryUnique"));
  }

  @Test
  @WithUserDetails("admin")
  void getAllNotificationTemplates_Ok() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("/v3/email-templates"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json"));
  }

  @Test
  @WithUserDetails("admin")
  void getAllNotificationTemplates_NotEmpty() throws Exception {
    // Assuming the service returns an empty string or empty XML when there are no templates
    mockMvc
        .perform(MockMvcRequestBuilders.get("/v3/email-templates"))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(
                    "[\"email-notification\",\"footer\",\"logo\",\"negotiation-confirmation\",\"negotiation-reminder\",\"negotiation-status-change\"]"));
  }
}
