package eu.bbmri_eric.negotiator.integration.repository;

import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.bbmri_eric.negotiator.NegotiatorApplication;
import eu.bbmri_eric.negotiator.database.model.AccessCriteriaSet;
import eu.bbmri_eric.negotiator.database.repository.AccessCriteriaRepository;
import eu.bbmri_eric.negotiator.database.repository.AccessCriteriaSectionRepository;
import eu.bbmri_eric.negotiator.database.repository.AccessCriteriaSetRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = NegotiatorApplication.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@TestPropertySource(properties = {"spring.sql.init.mode=never"})
public class AccessCriteriaSetRepoTest {
  @Autowired private AccessCriteriaSetRepository accessCriteriaSetRepository;
  @Autowired private AccessCriteriaSectionRepository accessCriteriaSectionRepository;
  @Autowired private AccessCriteriaRepository accessCriteriaRepository;

  @Test
  void findAll_none_0() {
    assertTrue(accessCriteriaSetRepository.findAll().isEmpty());
  }

  @Test
  void save_noSection_ok() {
    AccessCriteriaSet accessCriteriaSet =
        accessCriteriaSetRepository.saveAndFlush(new AccessCriteriaSet("test"));
    assertTrue(accessCriteriaSetRepository.findById(accessCriteriaSet.getId()).isPresent());
  }
  
}
