package eu.bbmri.eric.csit.service.negotiator.integration.repository;

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
    Person person =
        personRepository.save(
            Person.builder().subjectId("823").name("John").email("test@test.com").build());
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
}
