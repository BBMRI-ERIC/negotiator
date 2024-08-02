package eu.bbmri_eric.negotiator.integration.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import eu.bbmri_eric.negotiator.configuration.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.configuration.state_machine.resource.NegotiationResourceState;
import eu.bbmri_eric.negotiator.database.model.DiscoveryService;
import eu.bbmri_eric.negotiator.database.model.Negotiation;
import eu.bbmri_eric.negotiator.database.model.Organization;
import eu.bbmri_eric.negotiator.database.model.Request;
import eu.bbmri_eric.negotiator.database.model.Resource;
import eu.bbmri_eric.negotiator.database.model.views.ResourceViewDTO;
import eu.bbmri_eric.negotiator.database.repository.DiscoveryServiceRepository;
import eu.bbmri_eric.negotiator.database.repository.NegotiationRepository;
import eu.bbmri_eric.negotiator.database.repository.OrganizationRepository;
import eu.bbmri_eric.negotiator.database.repository.PersonRepository;
import eu.bbmri_eric.negotiator.database.repository.RequestRepository;
import eu.bbmri_eric.negotiator.database.repository.ResourceRepository;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@TestPropertySource(properties = {"spring.sql.init.mode=never"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class ResourceRepositoryTest {

  @Autowired PersonRepository personRepository;

  @Autowired RequestRepository requestRepository;
  @Autowired NegotiationRepository negotiationRepository;

  @Autowired ResourceRepository resourceRepository;

  @Autowired DiscoveryServiceRepository discoveryServiceRepository;

  @Autowired OrganizationRepository organizationRepository;

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

  @BeforeEach
  void setUp() {
    this.discoveryService =
        discoveryServiceRepository.save(DiscoveryService.builder().url("").name("").build());
  }

  @Test
  void findAll_empty_ok() {
    assertEquals(0, resourceRepository.findAll().size());
  }

  @Test
  void findAllBySourceIds_2_ok() {
    Organization organization =
        organizationRepository.save(
            Organization.builder().name("test").externalId("biobank:1").build());
    DiscoveryService discoveryService =
        discoveryServiceRepository.save(DiscoveryService.builder().url("").name("").build());

    resourceRepository.save(
        Resource.builder()
            .organization(organization)
            .discoveryService(discoveryService)
            .sourceId("collection:1")
            .name("test")
            .build());

    resourceRepository.save(
        Resource.builder()
            .organization(organization)
            .discoveryService(discoveryService)
            .sourceId("collection:2")
            .name("test")
            .build());
    assertEquals(
        2, resourceRepository.findAllBySourceIdIn(Set.of("collection:1", "collection:2")).size());
    assertEquals(1, resourceRepository.findAllBySourceIdIn(Set.of("collection:2")).size());
  }

  @Test
  void findAllByNegotiationId() {
    Organization organization =
        organizationRepository.save(
            Organization.builder().name("test").externalId("biobank:1").build());

    Resource res1 =
        resourceRepository.save(
            Resource.builder()
                .organization(organization)
                .discoveryService(discoveryService)
                .sourceId("collection:1")
                .name("test")
                .build());
    Resource res2 =
        resourceRepository.save(
            Resource.builder()
                .organization(organization)
                .discoveryService(discoveryService)
                .sourceId("collection:2")
                .name("test")
                .build());

    Request request =
        Request.builder()
            .url("http://test")
            .resources(Set.of(res1, res2))
            .discoveryService(discoveryService)
            .humanReadable("everything")
            .build();
    request = requestRepository.save(request);

    Negotiation negotiation =
        Negotiation.builder()
            .currentState(NegotiationState.SUBMITTED)
            .requests(Set.of(request))
            .publicPostsEnabled(false)
            .payload(payload)
            .build();
    request.setNegotiation(negotiation);
    negotiation.setStateForResource(res1.getSourceId(), NegotiationResourceState.SUBMITTED);
    negotiation.setStateForResource(
        res2.getSourceId(), NegotiationResourceState.REPRESENTATIVE_CONTACTED);
    negotiation = negotiationRepository.save(negotiation);

    List<ResourceViewDTO> resources = resourceRepository.findByNegotiation(negotiation.getId());
    assertFalse(resources.isEmpty());
  }
}
