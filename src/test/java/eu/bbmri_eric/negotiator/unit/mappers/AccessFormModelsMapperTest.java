package eu.bbmri_eric.negotiator.unit.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.bbmri_eric.negotiator.database.model.AccessForm;
import eu.bbmri_eric.negotiator.database.model.AccessFormElement;
import eu.bbmri_eric.negotiator.database.model.AccessFormSection;
import eu.bbmri_eric.negotiator.dto.FormElementType;
import eu.bbmri_eric.negotiator.dto.access_form.AccessFormDTO;
import eu.bbmri_eric.negotiator.mappers.AccessFormModelsMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

public class AccessFormModelsMapperTest {
  public ModelMapper mapper = new ModelMapper();
  public AccessFormModelsMapper accessFormModelsMapper = new AccessFormModelsMapper(mapper);

  @BeforeEach
  public void setup() {
    this.accessFormModelsMapper.addMappings();
  }

  @Test
  void entityToDto_formWithSectionsAndElements_ok() {
    AccessForm form = new AccessForm("test");
    AccessFormSection section = new AccessFormSection("test", "test1", "test2");
    AccessFormElement element =
        new AccessFormElement("test3", "test4", "test5", FormElementType.TEXT);
    form.linkSection(section, 0);
    form.linkElementToSection(section, element, 0, true);
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
