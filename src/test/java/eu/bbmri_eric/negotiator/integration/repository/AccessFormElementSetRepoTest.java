package eu.bbmri_eric.negotiator.integration.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.bbmri_eric.negotiator.NegotiatorApplication;
import eu.bbmri_eric.negotiator.database.model.AccessForm;
import eu.bbmri_eric.negotiator.database.model.AccessFormSection;
import eu.bbmri_eric.negotiator.database.repository.AccessFormElementRepository;
import eu.bbmri_eric.negotiator.database.repository.AccessFormRepository;
import eu.bbmri_eric.negotiator.database.repository.AccessFormSectionRepository;
import jakarta.transaction.Transactional;
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
public class AccessFormElementSetRepoTest {
  @Autowired private AccessFormRepository accessFormRepository;
  @Autowired private AccessFormSectionRepository accessFormSectionRepository;
  @Autowired private AccessFormElementRepository accessFormElementRepository;

  @Test
  void findAll_none_0() {
    assertTrue(accessFormRepository.findAll().isEmpty());
  }

  @Test
  void save_noSection_ok() {
    AccessForm accessForm = accessFormRepository.saveAndFlush(new AccessForm("test"));
    assertTrue(accessFormRepository.findById(accessForm.getId()).isPresent());
  }

  @Test
  @Transactional
  void testOrdering() {
    AccessForm accessForm = new AccessForm("different_form");
    accessForm = accessFormRepository.save(accessForm);
    for (int i = 0; i < 5; i++) {
      AccessFormSection accessFormSection =
          new AccessFormSection("different_section", "test", "test");
      accessFormSection = accessFormSectionRepository.save(accessFormSection);
      accessForm.addSection(accessFormSection, i);
    }
    accessForm = accessFormRepository.saveAndFlush(accessForm);
    assertEquals(accessForm, accessFormRepository.findById(accessForm.getId()).get());
  }
}
