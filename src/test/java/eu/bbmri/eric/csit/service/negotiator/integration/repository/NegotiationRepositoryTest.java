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
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
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

  @Test
  void save_ok() {
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
    Request request =
        Request.builder()
            .id("1")
            .url("http://test")
            .resources(Set.of(resource))
            .dataSource(dataSource)
            .humanReadable("everything")
            .build();
    request = requestRepository.save(request);
    assertEquals(1, requestRepository.findAll().size());
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
    assertEquals(1, negotiationRepository.findAll().size());
  }

  private Person savePerson(String subjectId) {
    return personRepository.save(
        Person.builder().subjectId(subjectId).name("John").email("test@test.com").build());
  }
}
