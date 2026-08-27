package eu.bbmri_eric.negotiator.integration.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.bbmri_eric.negotiator.form.AccessForm;
import eu.bbmri_eric.negotiator.form.repository.AccessFormRepository;
import eu.bbmri_eric.negotiator.info_requirement.InformationRequirementCreateDTO;
import eu.bbmri_eric.negotiator.info_requirement.InformationRequirementDTO;
import eu.bbmri_eric.negotiator.info_requirement.InformationRequirementRepository;
import eu.bbmri_eric.negotiator.info_requirement.InformationRequirementService;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
public class InformationRequirementServiceTest {
  @Autowired private InformationRequirementService service;
  @Autowired private AccessFormRepository accessFormRepository;
  @Autowired private InformationRequirementRepository informationRequirementRepository;

  @Test
  void createInformationRequirement_passedWrongParameters_throwsNullPointerException() {
    assertThrows(NullPointerException.class, () -> service.createInformationRequirement(null));
    assertThrows(
        NullPointerException.class,
        () ->
            service.createInformationRequirement(new InformationRequirementCreateDTO(null, null)));
  }

  @Test
  void createInformationRequirement_correctParameters_saved() {
    InformationRequirementDTO saved =
        service.createInformationRequirement(new InformationRequirementCreateDTO(1L, "CONTACT"));

    assertNotNull(saved);
    assertEquals("CONTACT", saved.getForResourceEvent());
    assertTrue(informationRequirementRepository.existsByForEvent("CONTACT"));
    assertFalse(informationRequirementRepository.existsByForEvent("STEP_AWAY"));
  }

  @Test
  void updateInformationRequirement_newParameters_ok() {
    AccessForm accessForm = accessFormRepository.save(new AccessForm("test2"));
    InformationRequirementCreateDTO createDTO = new InformationRequirementCreateDTO(1L, "CONTACT");
    InformationRequirementDTO savedDTO = service.createInformationRequirement(createDTO);
    createDTO.setRequiredAccessFormId(accessForm.getId());
    createDTO.setForResourceEvent("CONTACT");
    savedDTO = service.updateInformationRequirement(createDTO, savedDTO.getId());
    assertEquals(accessForm.getId(), savedDTO.getRequiredAccessForm().getId());
    assertEquals("CONTACT", savedDTO.getForResourceEvent());
  }

  @Test
  void findAlL_1saved_ok() {
    informationRequirementRepository.deleteAll();
    assertEquals(0, informationRequirementRepository.findAll().size());
    assertNotNull(
        service.createInformationRequirement(new InformationRequirementCreateDTO(1L, "CONTACT")));
    assertEquals(1, service.getAllInformationRequirements().size());
  }
}
