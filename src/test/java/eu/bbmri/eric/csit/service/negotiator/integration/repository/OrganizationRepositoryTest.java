package eu.bbmri.eric.csit.service.negotiator.integration.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import eu.bbmri.eric.csit.service.negotiator.NegotiatorApplication;
import eu.bbmri.eric.csit.service.negotiator.database.model.Organization;
import eu.bbmri.eric.csit.service.negotiator.database.repository.OrganizationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = NegotiatorApplication.class, properties = "spring.sql.init.mode=never")
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class OrganizationRepositoryTest {

  @Autowired OrganizationRepository organizationRepository;

  @Test
  void count_emptyTable_0() {
    assertEquals(0, organizationRepository.count());
  }

  @Test
  void save_null_InvalidDataAccessApiUsageException() {
    assertThrows(
        InvalidDataAccessApiUsageException.class,
        () -> {
          organizationRepository.save(null);
        });
  }

  @Test
  void save_validId_uuidIsGenerated() {
    assertEquals(0, organizationRepository.count());
    Organization savedOrganization =
        organizationRepository.save(Organization.builder().externalId("ExternalId").build());
    assertEquals(1, organizationRepository.count());
    assertEquals("ExternalId", savedOrganization.getExternalId());
    assertNotNull(savedOrganization.getId());
  }
}
