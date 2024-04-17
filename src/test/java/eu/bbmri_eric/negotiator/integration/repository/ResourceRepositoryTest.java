package eu.bbmri_eric.negotiator.integration.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.bbmri_eric.negotiator.database.model.DiscoveryService;
import eu.bbmri_eric.negotiator.database.model.Organization;
import eu.bbmri_eric.negotiator.database.model.Resource;
import eu.bbmri_eric.negotiator.database.repository.DiscoveryServiceRepository;
import eu.bbmri_eric.negotiator.database.repository.OrganizationRepository;
import eu.bbmri_eric.negotiator.database.repository.PersonRepository;
import eu.bbmri_eric.negotiator.database.repository.ResourceRepository;
import java.util.Set;
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

  @Autowired ResourceRepository resourceRepository;

  @Autowired DiscoveryServiceRepository discoveryServiceRepository;

  @Autowired OrganizationRepository organizationRepository;

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
}
