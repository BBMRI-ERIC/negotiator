package eu.bbmri_eric.negotiator.integration.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import eu.bbmri_eric.negotiator.NegotiatorApplication;
import eu.bbmri_eric.negotiator.database.model.DiscoveryService;
import eu.bbmri_eric.negotiator.database.model.Organization;
import eu.bbmri_eric.negotiator.database.model.Resource;
import eu.bbmri_eric.negotiator.database.repository.DiscoveryServiceRepository;
import eu.bbmri_eric.negotiator.database.repository.OrganizationRepository;
import eu.bbmri_eric.negotiator.database.repository.ResourceRepository;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = NegotiatorApplication.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class OrganizationRepositoryTest {

  @Autowired OrganizationRepository organizationRepository;

  @Autowired ResourceRepository resourceRepository;

  @Autowired
  DiscoveryServiceRepository discoveryServiceRepository;

  @Test
  void save_null_InvalidDataAccessApiUsageException() {
    assertThrows(InvalidDataAccessApiUsageException.class, () -> organizationRepository.save(null));
  }

  @Test
  void save_validId_uuidIsGenerated() {
    assertEquals(3, organizationRepository.count());
    Organization savedOrganization =
        organizationRepository.save(Organization.builder().externalId("ExternalId").build());
    assertEquals(4, organizationRepository.count());
    assertEquals("ExternalId", savedOrganization.getExternalId());
    assertNotNull(savedOrganization.getId());
  }

  @Test
  void getDetailedResources_ok() {
    DiscoveryService savedDiscoveryService =
        discoveryServiceRepository.save(
            DiscoveryService.builder()
                .url("")
                .name("")
                .build());

    Organization savedOrganization =
        organizationRepository.save(Organization.builder().externalId("ExternalId").build());

    resourceRepository.save(
        Resource.builder()
            .name("test Resource")
            .sourceId("collection:1")
            .discoveryService(savedDiscoveryService)
            .organization(savedOrganization)
            .build());
    assertEquals(
        "test Resource",
        organizationRepository
            .findDetailedById(savedOrganization.getId())
            .orElseThrow(() -> new NoSuchElementException("Value is empty"))
            .getResources()
            .iterator()
            .next()
            .getName());
  }
}
