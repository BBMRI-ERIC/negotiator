package eu.bbmri_eric.negotiator.integration.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.bbmri_eric.negotiator.database.model.DiscoveryService;
import eu.bbmri_eric.negotiator.database.model.Organization;
import eu.bbmri_eric.negotiator.database.model.Person;
import eu.bbmri_eric.negotiator.database.model.Resource;
import eu.bbmri_eric.negotiator.database.repository.DiscoveryServiceRepository;
import eu.bbmri_eric.negotiator.database.repository.OrganizationRepository;
import eu.bbmri_eric.negotiator.database.repository.PersonRepository;
import eu.bbmri_eric.negotiator.database.repository.PersonSpecifications;
import eu.bbmri_eric.negotiator.database.repository.ResourceRepository;
import jakarta.transaction.Transactional;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@TestPropertySource(properties = {"spring.sql.init.mode=never"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class PersonRepositoryTest {
  @Autowired PersonRepository personRepository;

  @Autowired ResourceRepository resourceRepository;

  @Autowired DiscoveryServiceRepository discoveryServiceRepository;

  @Autowired OrganizationRepository organizationRepository;

  @Test
  @Transactional
  void existsByIdAndRepresentsResources_oneResource_Ok() {
    Organization organization =
        organizationRepository.save(
            Organization.builder().name("test").externalId("biobank:1").build());
    DiscoveryService discoveryService =
        discoveryServiceRepository.save(DiscoveryService.builder().url("").name("").build());
    Person person = savePerson("test");
    Resource resource =
        resourceRepository.saveAndFlush(
            Resource.builder()
                .organization(organization)
                .discoveryService(discoveryService)
                .sourceId("collection:1")
                .representatives(new HashSet<>())
                .name("test")
                .build());
    person.addResource(resource);
    personRepository.saveAndFlush(person);
    assertTrue(personRepository.existsById(person.getId()));
    assertTrue(personRepository.existsByIdAndResourcesIn(person.getId(), Set.of(resource)));
    assertEquals(
        resource.getId(),
        personRepository
            .findDetailedById(person.getId())
            .get()
            .getResources()
            .iterator()
            .next()
            .getId());
    assertEquals(
        person.getId(),
        resourceRepository
            .findById(resource.getId())
            .get()
            .getRepresentatives()
            .iterator()
            .next()
            .getId());
  }

  @Test
  void findAllPage_onePerson_ok() {
    savePerson("test");
    assertEquals(1L, personRepository.findAll(PageRequest.of(0, 1)).getTotalElements());
    assertEquals(1, personRepository.findAll(PageRequest.of(0, 1)).getSize());
  }

  @Test
  void findAllPage_1000People_ok() {
    for (int i = 0; i < 1000; i++) {
      savePerson("test-" + i);
    }
    assertEquals(1000L, personRepository.findAll(PageRequest.of(0, 1)).getTotalElements());
    assertEquals(50, personRepository.findAll(PageRequest.of(0, 50)).getSize());
  }

  @Test
  void findAllPageWithSort_1001People_nameWithAIsFirst() {
    for (int i = 0; i < 1000; i++) {
      savePerson("test-" + i);
    }
    Person person = savePerson("a-test", "AAAA");
    assertEquals(
        person.getSubjectId(),
        personRepository
            .findAll(PageRequest.of(0, 50, Sort.by("name")))
            .getContent()
            .get(0)
            .getSubjectId());
    Page<Person> pageDescending =
        personRepository.findAll(PageRequest.of(20, 50, Sort.by("name").descending()));
    assertEquals(person.getSubjectId(), pageDescending.getContent().get(0).getSubjectId());
  }

  @Test
  void findAllPageWithFilterByName_personIsPresent_isFound() {
    Person person = savePerson("a-test", "AAAA");
    Page<Person> page = personRepository.findAllByName(person.getName(), PageRequest.of(0, 50));
    assertEquals(person.getSubjectId(), page.getContent().get(0).getSubjectId());
    savePerson("a-test2", "AAAA");
    assertEquals(
        2,
        personRepository.findAllByName(person.getName(), PageRequest.of(0, 50)).getTotalElements());
  }

  @Test
  void findAll_filterWithSpecificationNameContains_ok() {
    Person person = savePerson("a-test", "AAAA");
    assertTrue(personRepository.findAll(PersonSpecifications.nameContains("AA")).contains(person));
    assertFalse(
        personRepository
            .findAll(PersonSpecifications.propertyEquals("name", "AA"))
            .contains(person));
    assertTrue(
        personRepository
            .findAll(
                PersonSpecifications.propertyEquals("name", person.getName()),
                PageRequest.of(0, 10))
            .getContent()
            .contains(person));
  }

  private Person savePerson(String subjectId) {
    return personRepository.saveAndFlush(
        Person.builder()
            .subjectId(subjectId)
            .name("John")
            .email("test@test.com")
            .resources(new HashSet<>())
            .build());
  }

  private Person savePerson(String subjectId, String name) {
    return personRepository.save(
        Person.builder().subjectId(subjectId).name(name).email("test@test.com").build());
  }
}
