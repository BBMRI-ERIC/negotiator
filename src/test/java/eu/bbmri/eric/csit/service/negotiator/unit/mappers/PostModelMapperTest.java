package eu.bbmri.eric.csit.service.negotiator.unit.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.bbmri.eric.csit.service.negotiator.database.model.Person;
import eu.bbmri.eric.csit.service.negotiator.dto.person.PersonDTO;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

public class PostModelMapperTest {


  @Test
  public void testAddMappings() {
    Person inputPerson = new Person();
    PersonDTO mappedExpected = new PersonDTO();

    //set the input and the expected result
    String authName = "authName";
    String organization = "org";
    inputPerson.setAuthName(authName);
    inputPerson.setOrganization(organization);
    mappedExpected.setName(authName);
    mappedExpected.setOrganization(organization);

    ModelMapper modelMapper = new ModelMapper();
    PersonDTO outputMapping = modelMapper.map(inputPerson, PersonDTO.class);

    assertEquals(outputMapping.getName(), inputPerson.getAuthName());
    assertEquals(outputMapping.getOrganization(), inputPerson.getOrganization());

  }


}
