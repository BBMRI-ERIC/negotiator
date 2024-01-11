package eu.bbmri.eric.csit.service.negotiator.unit.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.bbmri.eric.csit.service.negotiator.database.model.DataSource;
import eu.bbmri.eric.csit.service.negotiator.database.model.Organization;
import eu.bbmri.eric.csit.service.negotiator.database.model.Person;
import eu.bbmri.eric.csit.service.negotiator.database.model.Resource;
import eu.bbmri.eric.csit.service.negotiator.dto.person.UserResponseModel;
import eu.bbmri.eric.csit.service.negotiator.mappers.PersonModelMapper;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

public class PersonModelMapperTest {

  public ModelMapper mapper = new ModelMapper();

  PersonModelMapper personModelMapper = new PersonModelMapper(mapper);

  @BeforeEach
  public void setup() {
    this.personModelMapper.addMappings();
  }

  @Test
  public void map_PersonToOauthUser_ok() {
    Resource resource =
        Resource.builder()
            .sourceId("test:collection")
            .name("My collection")
            .dataSource(new DataSource())
            .organization(Organization.builder().externalId("bb:1").name("BB").build())
            .build();
    Person inputPerson =
        Person.builder()
            .id(1L)
            .name("Lucifer Morningstar")
            .organization("Hell")
            .subjectId("666")
            .email("devil@dieties.com")
            .resources(Set.of(resource))
            .build();
    UserResponseModel outputMapping = mapper.map(inputPerson, UserResponseModel.class);
    assertEquals(inputPerson.getId().toString(), outputMapping.getId());
    assertEquals(inputPerson.getName(), outputMapping.getName());
    assertEquals(inputPerson.getSubjectId(), outputMapping.getSubjectId());
  }
}
