package eu.bbmri.eric.csit.service.negotiator.unit.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.bbmri.eric.csit.service.negotiator.database.model.Person;
import eu.bbmri.eric.csit.service.negotiator.dto.person.OauthUser;
import eu.bbmri.eric.csit.service.negotiator.mappers.PersonModelMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.modelmapper.ModelMapper;

public class PersonModelMapperTest {

  @Spy public ModelMapper mapper = new ModelMapper();

  @InjectMocks PersonModelMapper personModelMapper;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
    this.personModelMapper.addMappings();
  }

  @Test
  public void map_PersonToOauthUser_ok() {
    Person inputPerson =
        Person.builder()
            .name("Lucifer Morningstar")
            .organization("Hell")
            .subjectId("666")
            .email("devil@dieties.com")
            .build();
    OauthUser outputMapping = mapper.map(inputPerson, OauthUser.class);
    assertEquals(inputPerson.getName(), outputMapping.getName());
    assertEquals(inputPerson.getSubjectId(), outputMapping.getSubjectId());
  }
}
