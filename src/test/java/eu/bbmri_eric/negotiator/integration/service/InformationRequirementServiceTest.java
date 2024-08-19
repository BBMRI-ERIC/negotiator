package eu.bbmri_eric.negotiator.integration.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import eu.bbmri_eric.negotiator.NegotiatorApplication;
import eu.bbmri_eric.negotiator.configuration.state_machine.resource.NegotiationResourceEvent;
import eu.bbmri_eric.negotiator.info_requirement.InformationRequirementRepository;
import eu.bbmri_eric.negotiator.info_requirement.InformationRequirementCreateDTO;
import eu.bbmri_eric.negotiator.info_requirement.InformationRequirementDTO;
import eu.bbmri_eric.negotiator.form.AccessForm;
import eu.bbmri_eric.negotiator.form.AccessFormRepository;
import eu.bbmri_eric.negotiator.info_requirement.InformationRequirementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = NegotiatorApplication.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
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
    assertNotNull(
        service.createInformationRequirement(
            new InformationRequirementCreateDTO(1L, NegotiationResourceEvent.CONTACT)));
  }

  @Test
  void updateInformationRequirement_newParameters_ok() {
    AccessForm accessForm = accessFormRepository.save(new AccessForm("test2"));
    InformationRequirementCreateDTO createDTO =
        new InformationRequirementCreateDTO(1L, NegotiationResourceEvent.CONTACT);
    InformationRequirementDTO savedDTO = service.createInformationRequirement(createDTO);
    createDTO.setRequiredAccessFormId(accessForm.getId());
    createDTO.setForResourceEvent(NegotiationResourceEvent.CONTACT);
    savedDTO = service.updateInformationRequirement(createDTO, savedDTO.getId());
    assertEquals(accessForm.getId(), savedDTO.getRequiredAccessForm().getId());
    assertEquals(NegotiationResourceEvent.CONTACT, savedDTO.getForResourceEvent());
  }

  @Test
  void findAlL_1saved_ok() {
    informationRequirementRepository.deleteAll();
    assertEquals(0, informationRequirementRepository.findAll().size());
    assertNotNull(
        service.createInformationRequirement(
            new InformationRequirementCreateDTO(1L, NegotiationResourceEvent.CONTACT)));
    assertEquals(1, service.getAllInformationRequirements().size());
  }
}
