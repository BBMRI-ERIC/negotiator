package eu.bbmri.eric.csit.service.negotiator.unit.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.bbmri.eric.csit.service.negotiator.database.model.AccessCriteria;
import eu.bbmri.eric.csit.service.negotiator.database.model.AccessCriteriaSection;
import eu.bbmri.eric.csit.service.negotiator.database.model.AccessCriteriaSectionLink;
import eu.bbmri.eric.csit.service.negotiator.database.model.AccessCriteriaSet;
import eu.bbmri.eric.csit.service.negotiator.database.model.Resource;
import eu.bbmri.eric.csit.service.negotiator.dto.access_criteria.AccessCriteriaSetDTO;
import eu.bbmri.eric.csit.service.negotiator.mappers.AccessCriteriaModelsMapper;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.modelmapper.ModelMapper;

public class AccessCriteriaMapperTest {
  @Spy public ModelMapper mapper = new ModelMapper();

  @InjectMocks
  public AccessCriteriaModelsMapper accessCriteriaModelsMapper =
      new AccessCriteriaModelsMapper(mapper);

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
    this.accessCriteriaModelsMapper.addMappings();
  }

  @Test
  void map_name_Ok() {
    SortedSet<AccessCriteriaSection> sections = new TreeSet<>();
    AccessCriteriaSection accessCriteriaSection = new AccessCriteriaSection();
    AccessCriteriaSectionLink accessCriteriaSectionLink = new AccessCriteriaSectionLink();
    AccessCriteria accessCriteria = new AccessCriteria();
    accessCriteria.setName("test criteria");
    accessCriteria.setLabel("test label");
    accessCriteria.setDescription("test desc");
    accessCriteria.setType("test type");
    accessCriteriaSectionLink.setAccessCriteria(accessCriteria);
    accessCriteriaSectionLink.setRequired(true);
    accessCriteriaSection.setAccessCriteriaSectionLink(List.of(accessCriteriaSectionLink));
    accessCriteriaSection.setId(1L);
    accessCriteriaSection.setName("test section");
    accessCriteriaSection.setLabel("test section label");
    accessCriteriaSection.setDescription("test section desc");
    sections.add(accessCriteriaSection);
    AccessCriteriaSet accessCriteriaSet =
        new AccessCriteriaSet("test", Set.of(new Resource()), sections);
    AccessCriteriaSetDTO accessCriteriaSetDTO =
        mapper.map(accessCriteriaSet, AccessCriteriaSetDTO.class);
    assertEquals(accessCriteriaSet.getName(), accessCriteriaSetDTO.getName());
    assertEquals(
        accessCriteriaSection.getName(),
        accessCriteriaSetDTO.getSections().iterator().next().getName());
    assertEquals(
        accessCriteria.getName(),
        accessCriteriaSetDTO
            .getSections()
            .iterator()
            .next()
            .getAccessCriteria()
            .iterator()
            .next()
            .getName());
  }
}