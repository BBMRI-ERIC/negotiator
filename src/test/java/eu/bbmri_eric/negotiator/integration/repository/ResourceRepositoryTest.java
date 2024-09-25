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
            Organization.builder().name("test").externalId("biobank:1").build());

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

    Negotiation negotiation =
        Negotiation.builder()
            .currentState(NegotiationState.SUBMITTED)
            .resources(Set.of(res1, res2))
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
    assertEquals(res2.getSourceId(), resources.get(1).getSourceId());
  }
}
