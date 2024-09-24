package eu.bbmri_eric.negotiator.integration.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.bbmri_eric.negotiator.discovery.DiscoveryService;
import eu.bbmri_eric.negotiator.discovery.DiscoveryServiceRepository;
import eu.bbmri_eric.negotiator.governance.organization.Organization;
import eu.bbmri_eric.negotiator.governance.organization.OrganizationRepository;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.governance.resource.ResourceRepository;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.request.Request;
import eu.bbmri_eric.negotiator.negotiation.request.RequestRepository;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.notification.Notification;
import eu.bbmri_eric.negotiator.notification.NotificationRepository;
import eu.bbmri_eric.negotiator.notification.NotificationViewDTO;
import eu.bbmri_eric.negotiator.notification.email.NotificationEmailStatus;
import eu.bbmri_eric.negotiator.user.Person;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import eu.bbmri_eric.negotiator.util.RepositoryTest;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.sql.DataSource;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@RepositoryTest
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
      """
                      {
                  "project": {
                  "title": "Title",
                  "description": "Description"
                  },
                   "samples": {
                     "sample-type": "DNA",
                     "num-of-subjects": 10,
                     "num-of-samples": 20,
                     "volume-per-sample": 5
                   },
                   "ethics-vote": {
                     "ethics-vote": "My ethic vote"
                   }
                  }
                  """;
  private DiscoveryService discoveryService;
  private Person person;
  private Resource resource;
  private Negotiation negotiation;

  @BeforeEach
  void setUp() {
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
            .resources(resources)
            .humanReadable("#1 Material Type: DNA")
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
    System.out.println(notificationViewDTO.getNegotiationTitle());
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
