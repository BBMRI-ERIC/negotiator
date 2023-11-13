package eu.bbmri.eric.csit.service.negotiator.integration.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.bbmri.eric.csit.service.negotiator.database.repository.ResourceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@TestPropertySource(properties = {"spring.sql.init.mode=never"})
public class ResourceRepositoryTest {

  @Autowired private ResourceRepository resourceRepository;

  @Test
  void findAll_empty_ok() {
    assertEquals(0, resourceRepository.findAll().size());
  }
}
