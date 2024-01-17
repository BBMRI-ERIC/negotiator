package eu.bbmri.eric.csit.service.negotiator.integration.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.bbmri.eric.csit.service.negotiator.configuration.state_machine.negotiation.NegotiationState;
import eu.bbmri.eric.csit.service.negotiator.database.model.DataSource;
import eu.bbmri.eric.csit.service.negotiator.database.model.Negotiation;
import eu.bbmri.eric.csit.service.negotiator.database.model.Organization;
import eu.bbmri.eric.csit.service.negotiator.database.model.Person;
import eu.bbmri.eric.csit.service.negotiator.database.model.PersonNegotiationRole;
import eu.bbmri.eric.csit.service.negotiator.database.model.Request;
import eu.bbmri.eric.csit.service.negotiator.database.model.Resource;
import eu.bbmri.eric.csit.service.negotiator.database.model.Role;
import eu.bbmri.eric.csit.service.negotiator.database.repository.DataSourceRepository;
import eu.bbmri.eric.csit.service.negotiator.database.repository.NegotiationRepository;
import eu.bbmri.eric.csit.service.negotiator.database.repository.OrganizationRepository;
import eu.bbmri.eric.csit.service.negotiator.database.repository.PersonRepository;
import eu.bbmri.eric.csit.service.negotiator.database.repository.RequestRepository;
import eu.bbmri.eric.csit.service.negotiator.database.repository.ResourceRepository;
import eu.bbmri.eric.csit.service.negotiator.database.repository.RoleRepository;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@TestPropertySource(properties = {"spring.sql.init.mode=never"})
public class NegotiationRepositoryTest {
  @Autowired PersonRepository personRepository;

  @Autowired ResourceRepository resourceRepository;

  @Autowired RequestRepository requestRepository;

  @Autowired DataSourceRepository dataSourceRepository;

  @Autowired OrganizationRepository organizationRepository;
  @Autowired NegotiationRepository negotiationRepository;
  @Autowired RoleRepository roleRepository;
  private Organization organization;
  private DataSource dataSource;
  private Person person;
  private Resource resource;

  String payload =
      "    {\n"
          + "\"project\": {\n"
          + "\"title\": \"Title\",\n"
          + "\"description\": \"Description\"\n"
          + "},\n"
          + " \"samples\": {\n"
          + "   \"sample-type\": \"DNA\",\n"
          + "   \"num-of-subjects\": 10,\n"
          + "   \"num-of-samples\": 20,\n"
          + "   \"volume-per-sample\": 5\n"
          + " },\n"
          + " \"ethics-vote\": {\n"
          + "   \"ethics-vote\": \"My ethic vote\"\n"
          + " }\n"
          + "}\n";

  @BeforeEach
  void setUp() {
    this.organization =
        organizationRepository.save(
            Organization.builder().name("test").externalId("biobank:1").build());
    this.dataSource =
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
    this.person = savePerson("test");
    this.resource =
        resourceRepository.save(
            Resource.builder()
                .organization(organization)
                .dataSource(dataSource)
                .sourceId("collection:1")
                .name("test")
                .representatives(Set.of(person))
                .build());
  }

  @Test
  void save_1_ok() {
    saveNegotiation();
    assertEquals(1, negotiationRepository.findAll().size());
  }

  @Test
  void save_1000_ok() {
    for (int i = 0; i < 1000; i++) {
      saveNegotiation();
    }
    assertEquals(1000, negotiationRepository.findAll().size());
  }

  @Test
  void getPaged_1000negotiations_ok() {
    assertEquals(0, negotiationRepository.findAll().size());
    for (int i = 0; i < 1000; i++) {
      saveNegotiation();
    }
    assertEquals(100, negotiationRepository.findAll(PageRequest.of(0, 100)).getSize());
    assertEquals(10, negotiationRepository.findAll(PageRequest.of(0, 100)).getTotalPages());
  }

  @Test
  void save_10000differentResources_ok() {
    resourceRepository.deleteAll();
    for (int i = 0; i < 10000; i++) {
      Organization organization1 =
          organizationRepository.save(
              Organization.builder()
                  .name("test-%s".formatted(i))
                  .externalId("biobank-%s".formatted(i))
                  .build());
      Resource resource1 =
          resourceRepository.save(
              Resource.builder()
                  .organization(organization1)
                  .dataSource(dataSource)
                  .sourceId("collection:%s".formatted(i))
                  .name("test")
                  .representatives(new HashSet<>())
                  .build());
      for (int j = 0; j < 20; j++) {
        Person person1 = savePerson("test-%s-%s".formatted(i, j));
        person1.addResource(resource1);
        person1 = personRepository.save(person1);
        assertEquals(1, person1.getResources().size());
        assertEquals(resource1.getId(), person1.getResources().iterator().next().getId());
      }
      assertEquals(
          20, resourceRepository.findById(resource1.getId()).get().getRepresentatives().size());
    }
    Request request =
        Request.builder()
            .url("http://test")
            .resources(new HashSet<>(resourceRepository.findAll()))
            .dataSource(dataSource)
            .humanReadable("everything")
            .build();
    request = requestRepository.save(request);
    Negotiation negotiation =
        Negotiation.builder()
            .currentState(NegotiationState.SUBMITTED)
            .requests(Set.of(request))
            .postsEnabled(false)
            .payload(payload)
            .build();
    Role role = roleRepository.save(new Role("test"));
    PersonNegotiationRole personRole = new PersonNegotiationRole(person, negotiation, role);
    negotiation.setPersons(Set.of(personRole));
    request.setNegotiation(negotiation);
    negotiation = negotiationRepository.save(negotiation);
    Negotiation retrievedNegotiation =
        negotiationRepository.findDetailedById(negotiation.getId()).get();
    assertEquals(10000, retrievedNegotiation.getResources().size());
    for (Resource resource1 : retrievedNegotiation.getResources()) {
      assertEquals(20, resource1.getRepresentatives().size());
    }
  }

  private void saveNegotiation() {
    Request request =
        Request.builder()
            .url("http://test")
            .resources(Set.of(resource))
            .dataSource(dataSource)
            .humanReadable("everything")
            .build();
    request = requestRepository.save(request);
    Negotiation negotiation =
        Negotiation.builder()
            .currentState(NegotiationState.SUBMITTED)
            .requests(Set.of(request))
            .postsEnabled(false)
            .payload(payload)
            .build();
    Role role = roleRepository.save(new Role("test"));
    PersonNegotiationRole personRole = new PersonNegotiationRole(person, negotiation, role);
    negotiation.setPersons(Set.of(personRole));
    request.setNegotiation(negotiation);
    negotiationRepository.save(negotiation);
  }


  private Person savePerson(String subjectId) {
    return personRepository.save(
        Person.builder()
            .subjectId(subjectId)
            .name("John")
            .email("test@test.com")
            .resources(new HashSet<>())
            .build());
  }
}
