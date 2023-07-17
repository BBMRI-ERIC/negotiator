package eu.bbmri.eric.csit.service.negotiator.unit.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.bbmri.eric.csit.service.negotiator.database.model.Person;
import eu.bbmri.eric.csit.service.negotiator.dto.person.PersonDTO;
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
  public void map_PersonToDTO_ok() {
    Person inputPerson =
        Person.builder()
            .authName("Lucifer Morningstar")
            .organization("Hell")
            .authSubject("666")
            .authEmail("devil@dieties.com")
            .build();
    PersonDTO outputMapping = mapper.map(inputPerson, PersonDTO.class);
    assertEquals(inputPerson.getAuthName(), outputMapping.getName());
    assertEquals(inputPerson.getOrganization(), outputMapping.getOrganization());
  }
}
