package eu.bbmri_eric.negotiator.unit.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import eu.bbmri_eric.negotiator.database.model.AccessForm;
import eu.bbmri_eric.negotiator.database.model.AccessFormElement;
import eu.bbmri_eric.negotiator.database.model.AccessFormSection;
import org.junit.jupiter.api.Test;

public class AccessFormTest {

  @Test
  void initAccessForm_noArgsConstructor_ok() {
    new AccessForm("test");
  }

  @Test
  void linkElementToSection_sectionNotInForm_throwsIllegalArg() {
    AccessForm accessForm = new AccessForm("test");
    assertThrows(
        IllegalArgumentException.class,
        () ->
            accessForm.linkElementToSection(
                new AccessFormSection("test", "test", "test"),
                new AccessFormElement("test", "test", "test", "test"),
                0,
                true));
  }

  @Test
  void linkSection() {
    AccessForm accessForm = new AccessForm("test");
    AccessFormSection allowedSection = new AccessFormSection("test", "test", "test");
    allowedSection.setId(2L);
    accessForm.linkSection(allowedSection, 0);
    assertEquals(allowedSection.getId(), accessForm.getLinkedSections().iterator().next().getId());
  }

  @Test
  void linkElementToSection_sectionNotLinkedToElement_throwsIllegalArg() {
    AccessForm accessForm = new AccessForm("test");
    AccessFormSection notAllowedSection = new AccessFormSection("test", "test", "test");
    notAllowedSection.setId(1L);
    AccessFormSection allowedSection = new AccessFormSection("test", "test", "test");
    allowedSection.setId(2L);
    AccessFormElement element = new AccessFormElement("test", "test", "test", "test");
    element.setLinkedSection(allowedSection);
    accessForm.linkSection(notAllowedSection, 0);
    assertThrows(
        IllegalArgumentException.class,
        () -> accessForm.linkElementToSection(notAllowedSection, element, 0, true));
  }
}
