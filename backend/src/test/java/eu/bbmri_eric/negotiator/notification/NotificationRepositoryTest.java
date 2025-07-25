package eu.bbmri_eric.negotiator.notification;

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
import eu.bbmri_eric.negotiator.user.Person;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import eu.bbmri_eric.negotiator.util.RepositoryTest;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
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
            Organization.builder()
                .name("test")
                .description("test")
                .externalId("biobank:1")
                .build());
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
                .description("test")
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
            .resources(resources)
            .currentState(NegotiationState.SUBMITTED)
            .humanReadable("#1 Material Type: DNA")
            .publicPostsEnabled(false)
            .discoveryService(discoveryService)
            .payload(payload)
            .build();
    negotiation.setCreatedBy(author);
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
}
