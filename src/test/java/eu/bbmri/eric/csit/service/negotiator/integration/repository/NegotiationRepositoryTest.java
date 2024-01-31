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
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
                .representatives(new HashSet<>(List.of(person)))
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
    Role role = roleRepository.save(new Role(1L, "test"));
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

  @Test
  void findPagedAndFiltered_ok() {
    saveNegotiation();
    Negotiation negotiation = negotiationRepository.findAll().get(0);
    assertEquals(
        negotiation.getId(),
        negotiationRepository
            .findAllByCurrentState(PageRequest.of(0, 10, Sort.by("id")), NegotiationState.SUBMITTED)
            .get()
            .iterator()
            .next()
            .getId());
    assertEquals(
        NegotiationState.SUBMITTED,
        negotiationRepository
            .findAllByCurrentState(PageRequest.of(0, 10), NegotiationState.SUBMITTED)
            .get()
            .iterator()
            .next()
            .getCurrentState());
    negotiation.setCurrentState(NegotiationState.APPROVED);
    negotiationRepository.save(negotiation);
    assertEquals(
        0,
        negotiationRepository
            .findAllByCurrentState(PageRequest.of(0, 10), NegotiationState.SUBMITTED)
            .getNumberOfElements());
  }

  @Test
  void findAllConcerningUser_1rep_ok() {
    Person author = savePerson("author");
    saveNegotiation();
    author.addResource(resource);
    author = personRepository.save(author);
    assertEquals(
        1,
        negotiationRepository
            .findByCreatedByOrRequests_ResourcesIn(
                PageRequest.of(0, 10), author, author.getResources())
            .getNumberOfElements());
  }

  @Test
  void findAllRelated_1authored_ok() {
    saveNegotiation();
    assertEquals(person.getId(), negotiationRepository.findAll().get(0).getCreatedBy().getId());
    assertEquals(
        1,
        negotiationRepository
            .findByCreatedByOrRequests_ResourcesIn(
                PageRequest.of(0, 10), person, person.getResources())
            .getNumberOfElements());
  }

  @Test
  void findAllRelated_10authored30rep_ok() {
    person.addResource(resource);
    person = personRepository.save(person);
    Person firstUser = savePerson("firstUser");
    for (int i = 0; i < 10; i++) {
      saveNegotiation(person);
    }
    for (int i = 0; i < 30; i++) {
      saveNegotiation(firstUser);
    }
    assertEquals(1, person.getResources().size());
    assertEquals(
        40,
        negotiationRepository
            .findByCreatedByOrRequests_ResourcesIn(
                PageRequest.of(0, 50), person, person.getResources())
            .getNumberOfElements());
  }

  private void saveNegotiation() {
    Set<Request> requests = new HashSet<>();
    Set<Resource> resources = new HashSet<>();
    resources.add(resource);
    Request request =
        Request.builder()
            .url("http://test")
            .resources(resources)
            .dataSource(dataSource)
            .humanReadable("everything")
            .build();
    request = requestRepository.save(request);
    requests.add(request);
    Negotiation negotiation =
        Negotiation.builder()
            .currentState(NegotiationState.SUBMITTED)
            .requests(requests)
            .postsEnabled(false)
            .payload(payload)
            .build();
    negotiation.setCreatedBy(person);
    Role role = roleRepository.save(new Role(1L, "test"));
    Set<PersonNegotiationRole> roles = new HashSet<>();
    PersonNegotiationRole personRole = new PersonNegotiationRole(person, negotiation, role);
    roles.add(personRole);
    negotiation.setPersons(roles);
    request.setNegotiation(negotiation);
    negotiationRepository.save(negotiation);
  }

  private void saveNegotiation(Person author) {
    Set<Request> requests = new HashSet<>();
    Set<Resource> resources = new HashSet<>();
    resources.add(resource);
    Request request =
        Request.builder()
            .url("http://test")
            .resources(resources)
            .dataSource(dataSource)
            .humanReadable("everything")
            .build();
    request = requestRepository.save(request);
    requests.add(request);
    Negotiation negotiation =
        Negotiation.builder()
            .currentState(NegotiationState.SUBMITTED)
            .requests(requests)
            .postsEnabled(false)
            .payload(payload)
            .build();
    negotiation.setCreatedBy(author);
    Role role = roleRepository.save(new Role(1L, "test"));
    Set<PersonNegotiationRole> roles = new HashSet<>();
    PersonNegotiationRole personRole = new PersonNegotiationRole(author, negotiation, role);
    roles.add(personRole);
    negotiation.setPersons(roles);
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
