package eu.bbmri_eric.negotiator.integration.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.bbmri_eric.negotiator.configuration.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.database.model.DiscoveryService;
import eu.bbmri_eric.negotiator.database.model.Organization;
import eu.bbmri_eric.negotiator.database.model.Person;
import eu.bbmri_eric.negotiator.database.model.PersonNegotiationRole;
import eu.bbmri_eric.negotiator.database.model.Request;
import eu.bbmri_eric.negotiator.database.model.Resource;
import eu.bbmri_eric.negotiator.database.model.Role;
import eu.bbmri_eric.negotiator.database.model.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.database.repository.DiscoveryServiceRepository;
import eu.bbmri_eric.negotiator.database.repository.NegotiationRepository;
import eu.bbmri_eric.negotiator.database.repository.NegotiationSpecification;
import eu.bbmri_eric.negotiator.database.repository.OrganizationRepository;
import eu.bbmri_eric.negotiator.database.repository.PersonRepository;
import eu.bbmri_eric.negotiator.database.repository.RequestRepository;
import eu.bbmri_eric.negotiator.database.repository.ResourceRepository;
import eu.bbmri_eric.negotiator.database.repository.RoleRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@TestPropertySource(properties = {"spring.sql.init.mode=never"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class NegotiationRepositoryTest {
  @Autowired javax.sql.DataSource dbSource;

  @Autowired PersonRepository personRepository;

  @Autowired ResourceRepository resourceRepository;

  @Autowired RequestRepository requestRepository;

  @Autowired DiscoveryServiceRepository discoveryServiceRepository;

  @Autowired OrganizationRepository organizationRepository;
  @Autowired NegotiationRepository negotiationRepository;
  @Autowired RoleRepository roleRepository;
  private Organization organization;
  private DiscoveryService discoveryService;
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

  public void addH2Function() {
    String statementScript =
        "create DOMAIN IF NOT EXISTS JSONB AS JSON; \n"
            + "CREATE ALIAS IF NOT EXISTS JSONB_EXTRACT_PATH AS '\n"
            + "import com.jayway.jsonpath.JsonPath;\n"
            + "    @CODE\n"
            + "    String jsonbExtractPath(String jsonString, String...jsonPaths) {\n"
            + "      String overallPath = String.join(\".\", jsonPaths);\n"
            + "      try {\n"
            + "        Object result = JsonPath.read(jsonString, overallPath);\n"
            + "        if (result != null) {\n"
            + "          return result.toString();\n"
            + "        }\n"
            + "      } catch (Exception e) {\n"
            + "        e.printStackTrace();\n"
            + "      }\n"
            + "      return null;\n"
            + "    }';";
    JdbcTemplate jdbcTemplate = new JdbcTemplate(dbSource);
    jdbcTemplate.execute(statementScript);
  }

  @BeforeEach
  void setUp() {
    addH2Function();
    this.organization =
        organizationRepository.save(
            Organization.builder().name("test").externalId("biobank:1").build());
    this.discoveryService =
        discoveryServiceRepository.save(DiscoveryService.builder().url("").name("").build());
    this.person = savePerson("test");
    this.resource =
        resourceRepository.save(
            Resource.builder()
                .organization(organization)
                .discoveryService(discoveryService)
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
                  .discoveryService(discoveryService)
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
            .discoveryService(discoveryService)
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
            .findAll(
                NegotiationSpecification.hasState(List.of(NegotiationState.SUBMITTED)),
                PageRequest.of(0, 10, Sort.by("id")))
            .get()
            .iterator()
            .next()
            .getId());
    assertEquals(
        NegotiationState.SUBMITTED,
        negotiationRepository
            .findAll(
                NegotiationSpecification.hasState(List.of(NegotiationState.SUBMITTED)),
                PageRequest.of(0, 10, Sort.by("id")))
            .get()
            .iterator()
            .next()
            .getCurrentState());
    negotiation.setCurrentState(NegotiationState.APPROVED);
    negotiationRepository.save(negotiation);
    assertEquals(
        0,
        negotiationRepository
            .findAll(
                NegotiationSpecification.hasState(List.of(NegotiationState.SUBMITTED)),
                PageRequest.of(0, 10, Sort.by("id")))
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
            .findAll(
                NegotiationSpecification.hasResourcesIn(author.getResources()),
                PageRequest.of(0, 10))
            .getNumberOfElements());
  }

  @Test
  void findAllState_1submitted_ok() {
    saveNegotiation();
    assertEquals(person.getId(), negotiationRepository.findAll().get(0).getCreatedBy().getId());
    assertEquals(
        1,
        negotiationRepository
            .findAll(
                NegotiationSpecification.hasState(List.of(NegotiationState.SUBMITTED)),
                PageRequest.of(0, 10))
            .getNumberOfElements());
  }

  @Test
  void findAllCreated_1After_ok() {
    saveNegotiation();
    assertEquals(person.getId(), negotiationRepository.findAll().get(0).getCreatedBy().getId());
    assertEquals(
        1,
        negotiationRepository
            .findAll(
                NegotiationSpecification.createdBetween(LocalDate.now().minusDays(1), null),
                PageRequest.of(0, 10))
            .getNumberOfElements());
  }

  @Test
  void findAllCreated_1Before_ok() {
    saveNegotiation();
    assertEquals(person.getId(), negotiationRepository.findAll().get(0).getCreatedBy().getId());
    assertEquals(
        1,
        negotiationRepository
            .findAll(
                NegotiationSpecification.createdBetween(
                    LocalDate.now().minusDays(1), LocalDate.now().plusDays(1)),
                PageRequest.of(0, 10))
            .getNumberOfElements());
  }

  @Test
  void findAllCreated_1Between_ok() {
    saveNegotiation();
    assertEquals(person.getId(), negotiationRepository.findAll().get(0).getCreatedBy().getId());
    assertEquals(
        1,
        negotiationRepository
            .findAll(
                NegotiationSpecification.createdBetween(null, LocalDate.now().plusDays(1)),
                PageRequest.of(0, 10))
            .getNumberOfElements());
  }

  @Test
  void findAllRelated_1authored_ok() {
    saveNegotiation();
    assertEquals(person.getId(), negotiationRepository.findAll().get(0).getCreatedBy().getId());
    assertEquals(
        1,
        negotiationRepository
            .findAll(NegotiationSpecification.hasAuthor(person), PageRequest.of(0, 10))
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
            .findAll(
                NegotiationSpecification.hasAuthor(person)
                    .or(NegotiationSpecification.hasResourcesIn(person.getResources())),
                PageRequest.of(0, 50))
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
            .discoveryService(discoveryService)
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
    negotiation.setCreationDate(LocalDateTime.now());
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
            .discoveryService(discoveryService)
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
