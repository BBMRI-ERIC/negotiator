package eu.bbmri_eric.negotiator.integration.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.bbmri_eric.negotiator.form.AccessForm;
import eu.bbmri_eric.negotiator.form.AccessFormSection;
import eu.bbmri_eric.negotiator.form.repository.AccessFormRepository;
import eu.bbmri_eric.negotiator.form.repository.AccessFormSectionRepository;
import eu.bbmri_eric.negotiator.util.RepositoryTest;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@RepositoryTest
public class AccessFormElementSetRepoTest {
  @Autowired private AccessFormRepository accessFormRepository;
  @Autowired private AccessFormSectionRepository accessFormSectionRepository;

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
      accessForm.linkSection(accessFormSection, i);
    }
    accessForm = accessFormRepository.saveAndFlush(accessForm);
    assertEquals(accessForm, accessFormRepository.findById(accessForm.getId()).get());
  }
}
