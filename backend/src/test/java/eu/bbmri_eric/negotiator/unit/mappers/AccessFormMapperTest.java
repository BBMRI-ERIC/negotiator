package eu.bbmri_eric.negotiator.unit.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.bbmri_eric.negotiator.form.AccessForm;
import eu.bbmri_eric.negotiator.form.AccessFormElement;
import eu.bbmri_eric.negotiator.form.AccessFormSection;
import eu.bbmri_eric.negotiator.form.FormElementType;
import eu.bbmri_eric.negotiator.form.dto.AccessFormDTO;
import eu.bbmri_eric.negotiator.form.dto.AccessFormMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

public class AccessFormMapperTest {
  public ModelMapper mapper = new ModelMapper();
  public AccessFormMapper accessFormMapper = new AccessFormMapper(mapper);

  @BeforeEach
  public void setup() {
    this.accessFormMapper.addMappings();
  }

  @Test
  void entityToDto_formWithSectionsAndElements_ok() {
    AccessForm form = new AccessForm("test");
    AccessFormSection section = new AccessFormSection("test", "test1", "test2");
    AccessFormElement element =
        new AccessFormElement("test3", "test4", "test5", FormElementType.TEXT, "test5");
    AccessFormElement element1 =
        new AccessFormElement("test3", "test4", "test5", FormElementType.TEXT, null);
    form.linkSection(section, 0);
    form.linkElementToSection(section, element, 0, true);
    form.linkElementToSection(section, element1, 1, true);
    AccessFormDTO accessFormDTO = mapper.map(form, AccessFormDTO.class);
    assertEquals(form.getName(), accessFormDTO.getName());
    assertEquals(section.getName(), accessFormDTO.getSections().get(0).getName());
    assertEquals(
        element.getName(), accessFormDTO.getSections().get(0).getElements().get(0).getName());
    assertEquals(
        FormElementType.TEXT, accessFormDTO.getSections().get(0).getElements().get(0).getType());
    assertTrue(accessFormDTO.getSections().get(0).getElements().get(0).getRequired());
  }
}
