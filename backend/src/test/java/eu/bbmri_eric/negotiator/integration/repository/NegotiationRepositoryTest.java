package eu.bbmri_eric.negotiator.integration.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.bbmri_eric.negotiator.config.MockUserDetailsService;
import eu.bbmri_eric.negotiator.discovery.DiscoveryService;
import eu.bbmri_eric.negotiator.discovery.DiscoveryServiceRepository;
import eu.bbmri_eric.negotiator.governance.network.Network;
import eu.bbmri_eric.negotiator.governance.network.NetworkRepository;
import eu.bbmri_eric.negotiator.governance.organization.Organization;
import eu.bbmri_eric.negotiator.governance.organization.OrganizationRepository;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.governance.resource.ResourceRepository;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.NegotiationSpecification;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceState;
import eu.bbmri_eric.negotiator.post.Post;
import eu.bbmri_eric.negotiator.post.PostRepository;
import eu.bbmri_eric.negotiator.post.PostType;
import eu.bbmri_eric.negotiator.user.Person;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import eu.bbmri_eric.negotiator.util.RepositoryTest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@RepositoryTest
@Import(MockUserDetailsService.class)
public class NegotiationRepositoryTest {
  @Autowired PersonRepository personRepository;
  @Autowired ResourceRepository resourceRepository;
  @Autowired DiscoveryServiceRepository discoveryServiceRepository;
  @Autowired OrganizationRepository organizationRepository;
  @Autowired NegotiationRepository negotiationRepository;
  @Autowired NetworkRepository networkRepository;
  @Autowired PostRepository postRepository;

  private DiscoveryService discoveryService;
  private Person person;
  private Resource resource;
  private Network network;
  private Organization organization;

  String payload =
      """
         {
          "project": {
          "title": "Title",
          "description": "Description"
          },
           "samples": {
             "sample-type": "DNA",
             "num-of-subjects": 10,
             "num-of-samples": 20,
             "volume-per-sample": 5
           },
           "ethics-vote": {
             "ethics-vote": "My ethic vote"
           }
          }
          """;

  @BeforeEach
  void setUp() {
    this.organization =
        organizationRepository.save(
            Organization.builder()
                .name("test")
                .description("test")
                .externalId("biobank:1")
                .build());
    this.network =
        networkRepository.save(
            Network.builder()
                .externalId("idk")
                .name("idk")
                .description("idk")
                .uri("http://idk.org")
                .build());
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
                .description("test")
                .representatives(new HashSet<>(List.of(person)))
                .build());
    this.network.addResource(resource);
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
    // Batch insert for organizations and resources
    List<Organization> organizations = new ArrayList<>(10000);
    List<Resource> resources = new ArrayList<>(10000);

    for (int i = 0; i < 10000; i++) {
      Organization organization =
          Organization.builder()
              .name("test-%s".formatted(i))
              .description("test-%s".formatted(i))
              .externalId("biobank-%s".formatted(i))
              .build();
      organizations.add(organization);
    }
    organizationRepository.saveAll(organizations);
    for (int i = 0; i < 10000; i++) {
      Resource resource =
          Resource.builder()
              .organization(organizations.get(i))
              .discoveryService(discoveryService)
              .sourceId("collection:%s".formatted(i))
              .name("test")
              .description("test")
              .representatives(new HashSet<>())
              .build();
      resources.add(resource);
    }
    resourceRepository.saveAll(resources);

    // Batch insert for persons
    List<Person> persons = new ArrayList<>(10000 * 20);

    for (int i = 0; i < 10000; i++) {
      Resource resource = resources.get(i);
      for (int j = 0; j < 20; j++) {
        Person person =
            Person.builder()
                .subjectId("test-id-%s-%s".formatted(i, j))
                .name("John")
                .email("test@test.com")
                .resources(new HashSet<>())
                .build();
        person.addResource(resource);
        persons.add(person);
      }
    }
    personRepository.saveAll(persons);

    // Batch verify resource representatives count
    List<Long> resourceIds = resources.stream().map(Resource::getId).collect(Collectors.toList());
    List<Resource> updatedResources = resourceRepository.findAllById(resourceIds);
    for (Resource resource : updatedResources) {
      assertEquals(20, resource.getRepresentatives().size());
    }

    Negotiation negotiation =
        Negotiation.builder()
            .resources(new HashSet<>(resources))
            .currentState(NegotiationState.SUBMITTED)
            .publicPostsEnabled(false)
            .humanReadable("#1 Material Type: DNA")
            .discoveryService(discoveryService)
            .payload(payload)
            .build();
    negotiation = negotiationRepository.save(negotiation);

    // Verify negotiation and resources
    Negotiation retrievedNegotiation =
        negotiationRepository.findDetailedById(negotiation.getId()).get();
    assertTrue(retrievedNegotiation.getResources().size() > 9999);

    for (Resource resource : retrievedNegotiation.getResources()) {
      assertEquals(20, resource.getRepresentatives().size());
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

  @Test
  void findAllCreatedOn_ok() {
    saveNegotiation();
    Negotiation negotiation = negotiationRepository.findAll().get(0);
    negotiation.setCreationDate(LocalDateTime.now().minusDays(10));
    negotiationRepository.save(negotiation);
    assertFalse(
        negotiationRepository.findAllCreatedOn(LocalDateTime.now().minusDays(10)).isEmpty());
    assertTrue(negotiationRepository.findAllCreatedOn(LocalDateTime.now().minusDays(5)).isEmpty());
  }

  @Test
  void findIgnoredInNetwork_oneResource_ok() {
    saveNegotiation();
    Negotiation negotiation = negotiationRepository.findAll().get(0);
    negotiation.setCurrentState(NegotiationState.IN_PROGRESS);
    negotiation.setStateForResource(
        resource.getSourceId(), NegotiationResourceState.REPRESENTATIVE_CONTACTED);
    assertEquals(1, negotiationRepository.count());
    assertEquals(
        1,
        negotiationRepository
            .findAll()
            .get(0)
            .getResources()
            .iterator()
            .next()
            .getNetworks()
            .size());
    assertEquals(
        NegotiationResourceState.REPRESENTATIVE_CONTACTED,
        negotiationRepository.findAll().get(0).getCurrentStateForResource(resource.getSourceId()));
    assertEquals(
        1,
        networkRepository.countIgnoredForNetwork(
            LocalDate.now().minusYears(1), LocalDate.now(), network.getId()));
    negotiation.setStateForResource(
        resource.getSourceId(), NegotiationResourceState.REPRESENTATIVE_UNREACHABLE);
    assertEquals(
        1,
        networkRepository.countIgnoredForNetwork(
            LocalDate.now().minusYears(1), LocalDate.now(), network.getId()));
    negotiation.setStateForResource(
        resource.getSourceId(), NegotiationResourceState.RESOURCE_UNAVAILABLE);
    assertEquals(
        0,
        networkRepository.countIgnoredForNetwork(
            LocalDate.now().minusYears(1), LocalDate.now(), network.getId()));
  }

  @Test
  void findIgnoredInNetwork_oneResourceUnresponsive_ok() {
    saveNegotiation();
    Negotiation negotiation = negotiationRepository.findAll().get(0);
    negotiation.setCurrentState(NegotiationState.IN_PROGRESS);
    negotiation.setStateForResource(
        resource.getSourceId(), NegotiationResourceState.REPRESENTATIVE_CONTACTED);
    Resource resource2 =
        resourceRepository.save(
            Resource.builder()
                .organization(organization)
                .discoveryService(discoveryService)
                .sourceId("collection:2")
                .name("test")
                .description("test")
                .representatives(new HashSet<>(List.of(person)))
                .build());
    network.addResource(resource2);
    negotiation.addResource(resource2);
    negotiation.setStateForResource(
        resource2.getSourceId(), NegotiationResourceState.RESOURCE_UNAVAILABLE);
    assertEquals(
        1,
        networkRepository.countIgnoredForNetwork(
            LocalDate.now().minusYears(1), LocalDate.now(), network.getId()));
  }

  @Test
  void getMedianResponseTime_responseAfter10Days_lessThan0() {
    saveNegotiation();
    Negotiation negotiation = negotiationRepository.findAll().get(0);
    negotiation.setCreationDate(LocalDateTime.now().minusDays(10));
    negotiation.setCurrentState(NegotiationState.IN_PROGRESS);
    negotiation.setStateForResource(
        resource.getSourceId(), NegotiationResourceState.REPRESENTATIVE_CONTACTED);
    negotiation.setStateForResource(
        resource.getSourceId(), NegotiationResourceState.CHECKING_AVAILABILITY);
    assertEquals(
        10,
        networkRepository.getMedianResponseForNetwork(
            LocalDate.now().minusYears(1), LocalDate.now(), network.getId()),
        0.1);
  }

  @Test
  void getSuccessfulNegotiations_1suc_1() {
    saveNegotiation();
    Negotiation negotiation = negotiationRepository.findAll().get(0);
    negotiation.setCreationDate(LocalDateTime.now().minusDays(10));
    negotiation.setCurrentState(NegotiationState.IN_PROGRESS);
    negotiation.setStateForResource(
        resource.getSourceId(), NegotiationResourceState.RESOURCE_MADE_AVAILABLE);
    Resource resource2 =
        resourceRepository.save(
            Resource.builder()
                .organization(organization)
                .discoveryService(discoveryService)
                .sourceId("collection:2")
                .name("test")
                .description("test")
                .representatives(new HashSet<>(List.of(person)))
                .build());
    network.addResource(resource2);
    negotiation.addResource(resource2);
    negotiation.setStateForResource(
        resource2.getSourceId(), NegotiationResourceState.RESOURCE_UNAVAILABLE);
    assertEquals(
        1,
        networkRepository.getNumberOfSuccessfulNegotiationsForNetwork(
            LocalDate.now().minusYears(1), LocalDate.now(), network.getId()));
  }

  @Test
  void getSuccessfulNegotiation_1suc1not_1() {
    saveNegotiation();
    saveNegotiation();
    Negotiation negotiation = negotiationRepository.findAll().get(0);
    negotiation.setCreationDate(LocalDateTime.now().minusDays(10));
    negotiation.setCurrentState(NegotiationState.IN_PROGRESS);
    negotiation.setStateForResource(
        resource.getSourceId(), NegotiationResourceState.RESOURCE_MADE_AVAILABLE);
    Resource resource2 =
        resourceRepository.save(
            Resource.builder()
                .organization(organization)
                .discoveryService(discoveryService)
                .sourceId("collection:2")
                .name("test")
                .description("test")
                .representatives(new HashSet<>(List.of(person)))
                .build());
    network.addResource(resource2);
    negotiation.addResource(resource2);
    negotiation.setStateForResource(
        resource2.getSourceId(), NegotiationResourceState.RESOURCE_UNAVAILABLE);
    assertEquals(
        1,
        networkRepository.getNumberOfSuccessfulNegotiationsForNetwork(
            LocalDate.now().minusYears(1), LocalDate.now(), network.getId()));
  }

  @Test
  void getNewRequesters_1new_returns1() {
    saveNegotiation();
    Negotiation negotiation = negotiationRepository.findAll().get(0);
    negotiation.setCreationDate(LocalDateTime.now());
    assertEquals(
        1,
        networkRepository.getNumberOfNewRequesters(
            LocalDate.now().minusYears(1), LocalDate.now(), network.getId()));
  }

  @Test
  void getNewRequesters_requesterHasOldNegotiations_returns0() {
    saveNegotiation();
    saveNegotiation();
    List<Negotiation> negotiations = negotiationRepository.findAll();
    negotiations.get(0).setCreationDate(LocalDateTime.now().minusDays(10));
    negotiations.get(0).setCreatedBy(person);
    negotiations.get(1).setCreationDate(LocalDateTime.now().minusDays(1));
    negotiations.get(1).setCreatedBy(person);
    assertEquals(
        0,
        networkRepository.getNumberOfNewRequesters(
            LocalDate.now().minusDays(9), LocalDate.now(), network.getId()));
  }

  @Test
  void getActiveReps_1changedStateMachine_returns1() {
    saveNegotiation();
    Negotiation negotiation = negotiationRepository.findAll().get(0);
    negotiation.setCreationDate(LocalDateTime.now().minusDays(10));
    negotiation.setCurrentState(NegotiationState.IN_PROGRESS);
    negotiation.setStateForResource(
        resource.getSourceId(), NegotiationResourceState.RESOURCE_MADE_AVAILABLE);
    negotiation.getNegotiationResourceLifecycleRecords().iterator().next().setCreatedBy(person);
    assertEquals(
        1,
        networkRepository.getNumberOfActiveRepresentatives(
            LocalDate.now().minusDays(10), LocalDate.now().plusDays(10), network.getId()));
  }

  @Test
  void getActiveReps_1post_returns1() {
    saveNegotiation();
    Negotiation negotiation = negotiationRepository.findAll().get(0);
    negotiation.setCreationDate(LocalDateTime.now().minusDays(10));
    person.addResource(resource);
    Post post = Post.builder().negotiation(negotiation).text("test").type(PostType.PUBLIC).build();
    post.setCreatedBy(person);
    post.setCreationDate(LocalDateTime.now());
    post = postRepository.save(post);
    assertEquals(
        1,
        networkRepository.getNumberOfActiveRepresentatives(
            LocalDate.now().minusDays(9), LocalDate.now().plusDays(10), network.getId()));
  }

  @Test
  void getActiveReps_1post1update_returns1() {
    saveNegotiation();
    Negotiation negotiation = negotiationRepository.findAll().get(0);
    negotiation.setCreationDate(LocalDateTime.now().minusDays(10));
    person.addResource(resource);
    Post post = Post.builder().negotiation(negotiation).text("test").type(PostType.PUBLIC).build();
    post.setCreatedBy(person);
    post.setCreationDate(LocalDateTime.now());
    post = postRepository.save(post);
    saveNegotiation();
    negotiation = negotiationRepository.findAll().get(0);
    negotiation.setCreationDate(LocalDateTime.now().minusDays(10));
    negotiation.setCurrentState(NegotiationState.IN_PROGRESS);
    negotiation.setStateForResource(
        resource.getSourceId(), NegotiationResourceState.RESOURCE_MADE_AVAILABLE);
    negotiation.getNegotiationResourceLifecycleRecords().iterator().next().setCreatedBy(person);
    assertEquals(
        1,
        networkRepository.getNumberOfActiveRepresentatives(
            LocalDate.now().minusDays(9), LocalDate.now().plusDays(10), network.getId()));
  }

  private void saveNegotiation() {
    Set<Resource> resources = new HashSet<>();
    resources.add(resource);
    Negotiation negotiation =
        Negotiation.builder()
            .resources(resources)
            .currentState(NegotiationState.SUBMITTED)
            .humanReadable("#1 Material Type: DNA")
            .discoveryService(discoveryService)
            .publicPostsEnabled(false)
            .payload(payload)
            .build();
    negotiation.setCreationDate(LocalDateTime.now());
    negotiation.setCreatedBy(person);
    negotiationRepository.save(negotiation);
  }

  private void saveNegotiation(Person author) {
    Set<Resource> resources = new HashSet<>();
    resources.add(resource);
    Negotiation negotiation =
        Negotiation.builder()
            .resources(resources)
            .currentState(NegotiationState.SUBMITTED)
            .humanReadable("everything")
            .discoveryService(discoveryService)
            .publicPostsEnabled(false)
            .payload(payload)
            .build();
    negotiation.setCreatedBy(author);
    negotiationRepository.save(negotiation);
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
}
