package eu.bbmri_eric.negotiator.unit.mappers;

import eu.bbmri_eric.negotiator.mappers.AccessCriteriaModelsMapper;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.modelmapper.ModelMapper;

public class AccessFormElementMapperTest {
  @Spy public ModelMapper mapper = new ModelMapper();

  @InjectMocks
  public AccessCriteriaModelsMapper accessCriteriaModelsMapper =
      new AccessCriteriaModelsMapper(mapper);

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
    this.accessCriteriaModelsMapper.addMappings();
  }

  //  @Test
  //  void map_name_Ok() {
  //    SortedSet<AccessFormSection> sections = new TreeSet<>();
  //    AccessFormSection accessCriteriaSection = new AccessFormSection();
  //    AccessCriteriaSectionLink accessCriteriaSectionLink = new AccessCriteriaSectionLink();
  //    AccessFormElement accessCriteria = new AccessFormElement();
  //    accessCriteria.setName("test criteria");
  //    accessCriteria.setLabel("test label");
  //    accessCriteria.setDescription("test desc");
  //    accessCriteria.setType("test type");
  //    accessCriteriaSectionLink.setAccessFormElement(accessCriteria);
  //    accessCriteriaSectionLink.setRequired(true);
  //    //accessCriteriaSection.setAccessCriteriaSectionLink(List.of(accessCriteriaSectionLink));
  //    accessCriteriaSection.setId(1L);
  //    accessCriteriaSection.setName("test section");
  //    accessCriteriaSection.setLabel("test section label");
  //    accessCriteriaSection.setDescription("test section desc");
  //    sections.add(accessCriteriaSection);
  //    AccessForm accessForm =
  //        new AccessForm(1L, "test", Set.of(new Resource()), sections);
  //    AccessFormDTO accessCriteriaSetDTO =
  //        mapper.map(accessForm, AccessFormDTO.class);
  //    assertEquals(accessForm.getName(), accessCriteriaSetDTO.getName());
  //    assertEquals(
  //        accessCriteriaSection.getName(),
  //        accessCriteriaSetDTO.getSections().iterator().next().getName());
  //    assertEquals(
  //        accessCriteria.getName(),
  //        accessCriteriaSetDTO
  //            .getSections()
  //            .iterator()
  //            .next()
  //            .getAccessFormElement()
  //            .iterator()
  //            .next()
  //            .getName());
  //  }
}
