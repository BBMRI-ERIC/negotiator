package eu.bbmri.eric.csit.service.negotiator.integration.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.bbmri.eric.csit.service.negotiator.database.model.DataSource;
import eu.bbmri.eric.csit.service.negotiator.database.model.Organization;
import eu.bbmri.eric.csit.service.negotiator.database.model.Person;
import eu.bbmri.eric.csit.service.negotiator.database.model.Resource;
import eu.bbmri.eric.csit.service.negotiator.database.repository.DataSourceRepository;
import eu.bbmri.eric.csit.service.negotiator.database.repository.OrganizationRepository;
import eu.bbmri.eric.csit.service.negotiator.database.repository.PersonRepository;
import eu.bbmri.eric.csit.service.negotiator.database.repository.ResourceRepository;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@TestPropertySource(properties = {"spring.sql.init.mode=never"})
public class PersonRepositoryTest {
  @Autowired PersonRepository personRepository;

  @Autowired ResourceRepository resourceRepository;

  @Autowired DataSourceRepository dataSourceRepository;

  @Autowired OrganizationRepository organizationRepository;

  @Test
  void existsByIdAndRepresentsResources_oneResource_Ok() {
    Organization organization =
        organizationRepository.save(
            Organization.builder().name("test").externalId("biobank:1").build());
    DataSource dataSource =
        dataSourceRepository.save(
            DataSource.builder()
                .sourcePrefix("")
                .apiPassword("")
                .apiType(DataSource.ApiType.MOLGENIS)
                .apiUrl("")
                .apiUsername("")
                .url("")
                .resourceBiobank("")
                .resourceCollection("")
                .resourceNetwork("")
                .name("")
                .syncActive(true)
                .build());
    Person person = savePerson("test");
    Resource resource =
        resourceRepository.save(
            Resource.builder()
                .organization(organization)
                .dataSource(dataSource)
                .sourceId("collection:1")
                .name("test")
                .representatives(Set.of(person))
                .build());
    assertTrue(personRepository.existsById(person.getId()));
    assertTrue(personRepository.existsByIdAndResourcesIn(person.getId(), Set.of(resource)));
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

  private Person savePerson(String subjectId) {
    return personRepository.save(
        Person.builder().subjectId(subjectId).name("John").email("test@test.com").build());
  }

  private Person savePerson(String subjectId, String name) {
    return personRepository.save(
        Person.builder().subjectId(subjectId).name(name).email("test@test.com").build());
  }
}
