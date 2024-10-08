package eu.bbmri_eric.negotiator.unit.model;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import eu.bbmri_eric.negotiator.user.Person;
import org.junit.jupiter.api.Test;

public class PersonTest {
  @Test
  void testInit() {
    assertInstanceOf(Person.class, new Person());
  }

  @Test
  void getPersonId() {
    Person person = new Person();
    assertNull(person.getId());
  }
}
