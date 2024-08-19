package eu.bbmri_eric.negotiator.integration.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.bbmri_eric.negotiator.configuration.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.database.model.DiscoveryService;
import eu.bbmri_eric.negotiator.database.model.Negotiation;
import eu.bbmri_eric.negotiator.database.model.Person;
import eu.bbmri_eric.negotiator.database.model.Request;
import eu.bbmri_eric.negotiator.database.repository.DiscoveryServiceRepository;
import eu.bbmri_eric.negotiator.database.repository.NegotiationRepository;
import eu.bbmri_eric.negotiator.database.repository.PersonRepository;
import eu.bbmri_eric.negotiator.database.repository.RequestRepository;
import eu.bbmri_eric.negotiator.governance.organization.Organization;
import eu.bbmri_eric.negotiator.governance.organization.OrganizationRepository;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.governance.resource.ResourceRepository;
import eu.bbmri_eric.negotiator.notification.Notification;
import eu.bbmri_eric.negotiator.notification.NotificationEmailStatus;
import eu.bbmri_eric.negotiator.notification.NotificationRepository;
import eu.bbmri_eric.negotiator.notification.NotificationViewDTO;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.sql.DataSource;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@TestPropertySource(properties = {"spring.sql.init.mode=never"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class NotificationRepositoryTest {
  @Autowired DataSource dbSource;
  @Autowired PersonRepository personRepository;
  @Autowired ResourceRepository resourceRepository;
  @Autowired RequestRepository requestRepository;
  @Autowired DiscoveryServiceRepository discoveryServiceRepository;
  @Autowired OrganizationRepository organizationRepository;
  @Autowired NegotiationRepository negotiationRepository;
  @Autowired NotificationRepository notificationRepository;

  String payload =
      "    {\n"
          + "\"project\": {\n"
          + "\"title\": \"Title\",\n"
          + "\"description\": \"Description\"\n"
          + "},\n"
          + " \"samples\": {\n"
          + "   \"sample-type\": \"DNA\",\n"
          + "   \"num-of-subjects\": 10,\n"
          + "   \"num-of-samples\": 20,\n"
          + "   \"volume-per-sample\": 5\n"
          + " },\n"
          + " \"ethics-vote\": {\n"
          + "   \"ethics-vote\": \"My ethic vote\"\n"
          + " }\n"
          + "}\n";
  private DiscoveryService discoveryService;
  private Person person;
  private Resource resource;
  private Negotiation negotiation;

  public void addH2Function() {
    String statementScript =
        "create DOMAIN IF NOT EXISTS JSONB AS JSON; \n"
            + "CREATE ALIAS IF NOT EXISTS JSONB_EXTRACT_PATH AS '\n"
            + "import com.jayway.jsonpath.JsonPath;\n"
            + "    @CODE\n"
            + "    String jsonbExtractPath(String jsonString, String...jsonPaths) {\n"
            + "      String overallPath = String.join(\".\", jsonPaths);\n"
            + "      try {\n"
            + "        Object result = JsonPath.read(jsonString, overallPath);\n"
            + "        if (result != null) {\n"
            + "          return result.toString();\n"
            + "        }\n"
            + "      } catch (Exception e) {\n"
            + "        e.printStackTrace();\n"
            + "      }\n"
            + "      return null;\n"
            + "    }';";
    JdbcTemplate jdbcTemplate = new JdbcTemplate(dbSource);
    jdbcTemplate.execute(statementScript);
  }

  @BeforeEach
  void setUp() {
    addH2Function();
    Organization organization =
        organizationRepository.save(
            Organization.builder().name("test").externalId("biobank:1").build());
    this.discoveryService =
        discoveryServiceRepository.save(DiscoveryService.builder().url("").name("").build());
    this.person = savePerson("test");
    this.resource =
        resourceRepository.save(
            Resource.builder()
                .organization(organization)
                .discoveryService(discoveryService)
                .sourceId("collection:1")
                .name("test")
                .representatives(new HashSet<>(List.of(person)))
                .build());
    this.negotiation = saveNegotiation(this.person);
  }

  private Negotiation saveNegotiation(Person author) {
    Set<Request> requests = new HashSet<>();
    Set<Resource> resources = new HashSet<>();
    resources.add(resource);
    Request request =
        Request.builder()
            .url("http://test")
            .resources(resources)
            .discoveryService(discoveryService)
            .humanReadable("everything")
            .build();
    request = requestRepository.save(request);
    requests.add(request);
    Negotiation negotiation =
        Negotiation.builder()
            .currentState(NegotiationState.SUBMITTED)
            .requests(requests)
            .publicPostsEnabled(false)
            .payload(payload)
            .build();
    negotiation.setCreatedBy(author);
    request.setNegotiation(negotiation);
    negotiationRepository.save(negotiation);
    return negotiation;
  }

  private Person savePerson(String subjectId) {
    return personRepository.save(
        Person.builder()
            .subjectId(subjectId)
            .name("John")
            .email("test@test.com")
            .resources(new HashSet<>())
            .build());
  }

  private Notification saveNotification() {
    Notification notification =
        Notification.builder()
            .id(1L)
            .negotiation(this.negotiation)
            .recipient(this.person)
            .emailStatus(NotificationEmailStatus.EMAIL_NOT_SENT)
            .build();
    return notificationRepository.save(notification);
  }

  @Test
  public void testFindByRecipientAndEmailStatus_ok() {
    saveNotification();
    List<NotificationViewDTO> notificationViewDTOs =
        notificationRepository.findViewByRecipientIdAndEmailStatus(
            this.person.getId(), NotificationEmailStatus.EMAIL_NOT_SENT);
    NotificationViewDTO notificationViewDTO = notificationViewDTOs.get(0);
    assertEquals(person.getId(), notificationViewDTO.getRecipient().getId());
    assertEquals(NotificationEmailStatus.EMAIL_NOT_SENT, notificationViewDTO.getEmailStatus());
    assertEquals(this.negotiation.getId(), notificationViewDTO.getNegotiationId());
    assertEquals(parseTitleFromNegotiation(negotiation), notificationViewDTO.getNegotiationTitle());
  }

  private static String parseTitleFromNegotiation(Negotiation negotiation) {
    String title;
    try {
      JSONObject payloadJson = new JSONObject(negotiation.getPayload());
      title = payloadJson.getJSONObject("project").getString("title");
    } catch (JSONException e) {
      title = "Untitled negotiation";
    }
    return title;
  }
}
