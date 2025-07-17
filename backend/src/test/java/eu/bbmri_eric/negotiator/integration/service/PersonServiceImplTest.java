package eu.bbmri_eric.negotiator.integration.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import eu.bbmri_eric.negotiator.user.Person;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import eu.bbmri_eric.negotiator.user.PersonService;
import eu.bbmri_eric.negotiator.user.UserFilterDTO;
import eu.bbmri_eric.negotiator.user.UserResponseModel;
import eu.bbmri_eric.negotiator.user.UserSortField;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

@IntegrationTest
public class PersonServiceImplTest {

  @Autowired PersonService personService;
  @Autowired PersonRepository personRepository;

  @Test
  void findAll_page_ok() {
    int count = personRepository.findAll().size();
    UserFilterDTO filters =
        UserFilterDTO.builder()
            .sortBy(UserSortField.lastLogin)
            .sortOrder(Sort.Direction.DESC)
            .page(0)
            .size(1)
            .build();
    Iterable<UserResponseModel> result = personService.findAllByFilters(filters);
    assertInstanceOf(Page.class, result);
    assertEquals(count, ((Page<UserResponseModel>) result).getTotalElements());
  }

  @Test
  @Disabled
  void findAll_invalidSort_throwsWrongSortingPropertyException() {
    //    assertThrows(WrongSortingPropertyException.class, () -> personService.findAll(0, 1,
    // "invalid"));
  }

  @Test
  void findAll_pageAndSorted_ok() {
    Person person =
        personRepository.save(
            Person.builder().subjectId("test-id").name("AAAAA").email("test@test.com").build());
    UserFilterDTO filters =
        UserFilterDTO.builder()
            .sortBy(UserSortField.name)
            .sortOrder(Sort.Direction.DESC)
            .page(0)
            .size(1)
            .build();
    assertEquals(
        person.getId().toString(),
        ((Page<UserResponseModel>) personService.findAllByFilters(filters))
            .getContent()
            .get(0)
            .getId());
  }

  @Test
  void findAll_tooHighPageNumber_throwsIllegalArg() {
    UserFilterDTO filters =
        UserFilterDTO.builder().sortBy(UserSortField.lastLogin).page(999).size(1).build();
    assertThrows(IllegalArgumentException.class, () -> personService.findAllByFilters(filters));
  }
}
