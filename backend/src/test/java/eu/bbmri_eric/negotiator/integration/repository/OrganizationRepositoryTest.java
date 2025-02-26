package eu.bbmri_eric.negotiator.integration.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import eu.bbmri_eric.negotiator.discovery.DiscoveryService;
import eu.bbmri_eric.negotiator.discovery.DiscoveryServiceRepository;
import eu.bbmri_eric.negotiator.governance.organization.Organization;
import eu.bbmri_eric.negotiator.governance.organization.OrganizationRepository;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.governance.resource.ResourceRepository;
import eu.bbmri_eric.negotiator.util.RepositoryTest;
import java.util.HashSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessApiUsageException;

@RepositoryTest(loadTestData = true)
public class OrganizationRepositoryTest {

  @Autowired OrganizationRepository organizationRepository;

  @Autowired ResourceRepository resourceRepository;

  @Autowired DiscoveryServiceRepository discoveryServiceRepository;

  @Test
  void save_null_InvalidDataAccessApiUsageException() {
    assertThrows(InvalidDataAccessApiUsageException.class, () -> organizationRepository.save(null));
  }

  @Test
  void save_validId_uuidIsGenerated() {
    assertEquals(3, organizationRepository.count());
    Organization savedOrganization =
        organizationRepository.save(
            Organization.builder()
                .externalId("ExternalId")
                .name("name")
                .description("description")
                .build());
    assertEquals(4, organizationRepository.count());
    assertEquals("ExternalId", savedOrganization.getExternalId());
    assertNotNull(savedOrganization.getId());
  }

  @Test
  void save_longURI() {
    String baseURI = "http://example.com";
    String repetition = "a".repeat(1000);
    String longURI = baseURI + repetition;
    assertEquals(3, organizationRepository.count());
    Organization savedOrganization =
        organizationRepository.save(
            Organization.builder()
                .externalId("ExternalId")
                .name("name")
                .description("description")
                .uri(longURI)
                .build());
    assertEquals(4, organizationRepository.count());
    assertEquals("ExternalId", savedOrganization.getExternalId());
    assertNotNull(savedOrganization.getId());
  }

  @Test
  void getDetailedResources_ok() {
    DiscoveryService savedDiscoveryService =
        discoveryServiceRepository.save(DiscoveryService.builder().url("").name("").build());

    Organization savedOrganization =
        organizationRepository.saveAndFlush(
            new Organization("testorg", "testname", "testdescription", new HashSet<>(), false));

    Resource resource =
        resourceRepository.saveAndFlush(
            new Resource(
                "test-resource", "test-desc", "test-id", savedDiscoveryService, savedOrganization));
    assertEquals(
        "test-resource", resource.getOrganization().getResources().iterator().next().getName());
  }
}
