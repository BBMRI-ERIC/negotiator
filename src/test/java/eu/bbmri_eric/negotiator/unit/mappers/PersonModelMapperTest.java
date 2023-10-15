package eu.bbmri_eric.negotiator.unit.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.bbmri_eric.negotiator.database.model.Person;
import eu.bbmri_eric.negotiator.dto.person.PersonDTO;
import eu.bbmri_eric.negotiator.mappers.PersonModelMapper;
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
