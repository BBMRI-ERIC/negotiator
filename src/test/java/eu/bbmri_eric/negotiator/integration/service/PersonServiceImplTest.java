package eu.bbmri_eric.negotiator.integration.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import eu.bbmri_eric.negotiator.common.exceptions.WrongSortingPropertyException;
import eu.bbmri_eric.negotiator.user.Person;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import eu.bbmri_eric.negotiator.user.PersonService;
import eu.bbmri_eric.negotiator.user.UserResponseModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class PersonServiceImplTest {

  @Autowired PersonService personService;
  @Autowired PersonRepository personRepository;

  @Test
  void loadContext() {}

  @Test
  void findAll_page_ok() {
    int count = personRepository.findAll().size();
    Iterable<UserResponseModel> result = personService.findAll(0, 1);
    assertInstanceOf(Page.class, result);
    assertEquals(count, ((Page<UserResponseModel>) result).getTotalElements());
  }

  @Test
  void findAll_invalidSort_throwsWrongSortingPropertyException() {
    assertThrows(WrongSortingPropertyException.class, () -> personService.findAll(0, 1, "invalid"));
  }

  @Test
  void findAll_pageAndSorted_ok() {
    Person person =
        personRepository.save(
            Person.builder().subjectId("test-id").name("AAAAA").email("test@test.com").build());
    assertEquals(
        person.getId().toString(),
        ((Page<UserResponseModel>) personService.findAll(0, 1, "name"))
            .getContent()
            .get(0)
            .getId());
  }

  @Test
  void findAll_tooHighPageNumber_throwsIllegalArg() {
    assertThrows(IllegalArgumentException.class, () -> personService.findAll(999, 1));
  }
}
