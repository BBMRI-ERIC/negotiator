package eu.bbmri_eric.negotiator.integration.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import eu.bbmri_eric.negotiator.discovery.DiscoveryService;
import eu.bbmri_eric.negotiator.discovery.DiscoveryServiceRepository;
import eu.bbmri_eric.negotiator.governance.organization.Organization;
import eu.bbmri_eric.negotiator.governance.organization.OrganizationRepository;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.governance.resource.ResourceRepository;
import eu.bbmri_eric.negotiator.governance.resource.ResourceViewDTO;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.request.RequestRepository;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceState;
import eu.bbmri_eric.negotiator.util.RepositoryTest;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@RepositoryTest
public class ResourceRepositoryTest {

  @Autowired RequestRepository requestRepository;
  @Autowired NegotiationRepository negotiationRepository;

  @Autowired ResourceRepository resourceRepository;

  @Autowired DiscoveryServiceRepository discoveryServiceRepository;

  @Autowired OrganizationRepository organizationRepository;

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
            Organization.builder()
                .name("test")
                .description("test")
                .externalId("biobank:1")
                .withdrawn(false)
                .contactEmail("test@test.org")
                .contactEmail("test@test.org")
                .uri("http://test.org")
                .build());

    resourceRepository.save(
        Resource.builder()
            .organization(organization)
            .discoveryService(discoveryService)
            .sourceId("collection:1")
            .name("test")
            .description("test")
            .contactEmail("test@test.org")
            .uri("http://test.org")
            .build());

    resourceRepository.save(
        Resource.builder()
            .organization(organization)
            .discoveryService(discoveryService)
            .sourceId("collection:2")
            .name("test")
            .description("test")
            .contactEmail("test@test.org")
            .uri("http://test.org")
            .build());
    assertEquals(
        2, resourceRepository.findAllBySourceIdIn(Set.of("collection:1", "collection:2")).size());
    assertEquals(1, resourceRepository.findAllBySourceIdIn(Set.of("collection:2")).size());
    Resource resource = resourceRepository.findAllBySourceIdIn(Set.of("collection:2")).get(0);
    assertEquals("test", resource.getName());
    assertEquals("test", resource.getDescription());
    assertEquals("test@test.org", resource.getContactEmail());
    assertEquals("http://test.org", resource.getUri());
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
                .description("test")
                .contactEmail("test@test.org")
                .uri("http://test.org")
                .build());
    Resource res2 =
        resourceRepository.save(
            Resource.builder()
                .organization(organization)
                .discoveryService(discoveryService)
                .sourceId("collection:2")
                .name("test")
                .description("test")
                .contactEmail("test@test.org")
                .uri("http://test.org")
                .build());

    Negotiation negotiation =
        Negotiation.builder()
            .resources(Set.of(res1, res2))
            .currentState(NegotiationState.SUBMITTED)
            .humanReadable("#1 MaterialType: DNA")
            .discoveryService(discoveryService)
            .publicPostsEnabled(false)
            .payload(payload)
            .build();
    negotiation.setStateForResource(res1.getSourceId(), NegotiationResourceState.SUBMITTED);
    negotiation.setStateForResource(
        res2.getSourceId(), NegotiationResourceState.REPRESENTATIVE_CONTACTED);
    negotiation = negotiationRepository.save(negotiation);

    List<ResourceViewDTO> resources = resourceRepository.findByNegotiation(negotiation.getId());
    assertFalse(resources.isEmpty());
    assertEquals(res1.getSourceId(), resources.get(0).getSourceId());
    assertEquals(res1.getName(), resources.get(0).getName());
    assertEquals(res1.getDescription(), resources.get(0).getDescription());
    assertEquals(res1.getContactEmail(), resources.get(0).getContactEmail());
    assertEquals(res1.getUri(), resources.get(0).getUri());
    assertEquals(res2.getSourceId(), resources.get(1).getSourceId());
    assertEquals(res2.getSourceId(), resources.get(1).getSourceId());
    assertEquals(res2.getName(), resources.get(1).getName());
    assertEquals(res2.getDescription(), resources.get(1).getDescription());
    assertEquals(res2.getContactEmail(), resources.get(1).getContactEmail());
    assertEquals(res2.getUri(), resources.get(1).getUri());
  }
}
