package eu.bbmri_eric.negotiator.integration.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import eu.bbmri_eric.negotiator.discovery.DiscoveryService;
import eu.bbmri_eric.negotiator.discovery.DiscoveryServiceRepository;
import eu.bbmri_eric.negotiator.governance.network.Network;
import eu.bbmri_eric.negotiator.governance.network.NetworkRepository;
import eu.bbmri_eric.negotiator.governance.organization.Organization;
import eu.bbmri_eric.negotiator.governance.organization.OrganizationRepository;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.governance.resource.ResourceRepository;
import eu.bbmri_eric.negotiator.user.Person;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import eu.bbmri_eric.negotiator.util.RepositoryTest;
import jakarta.transaction.Transactional;
import java.util.HashSet;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@RepositoryTest
public class NetworkRepositoryTest {

  @Autowired private NetworkRepository networkRepository;

  @Autowired ResourceRepository resourceRepository;

  @Autowired DiscoveryServiceRepository discoveryServiceRepository;

  @Autowired OrganizationRepository organizationRepository;
  @Autowired private PersonRepository personRepository;

  @Test
  void findByExternalIdReturnsNetworkWhenIdExists() {
    Network network = Network.builder().uri("http://example.com").externalId("validId").build();
    network.setExternalId("known-id");
    networkRepository.save(network);

    Optional<Network> foundNetwork = networkRepository.findByExternalId("known-id");

    assertThat(foundNetwork).isPresent();
  }

  @Test
  void findByExternalIdReturnsEmptyWhenIdDoesNotExist() {
    Optional<Network> foundNetwork = networkRepository.findByExternalId("unknown-id");

    assertThat(foundNetwork).isEmpty();
  }

  @Test
  void existsByExternalIdReturnsTrueWhenIdExists() {
    Network network = Network.builder().uri("http://example.com").externalId("validId").build();
    network.setExternalId("known-id");
    networkRepository.save(network);

    boolean exists = networkRepository.existsByExternalId("known-id");

    assertTrue(exists);
  }

  @Test
  void existsByExternalIdReturnsFalseWhenIdDoesNotExist() {
    boolean exists = networkRepository.existsByExternalId("unknown-id");

    assertFalse(exists);
  }

  @Test
  void existsByUriReturnsTrueWhenUriExists() {
    Network network = Network.builder().uri("http://example.com").externalId("validId").build();
    network.setUri("known-uri");
    networkRepository.save(network);

    boolean exists = networkRepository.existsByUri("known-uri");

    assertTrue(exists);
  }

  @Test
  void existsByUriReturnsFalseWhenUriDoesNotExist() {
    boolean exists = networkRepository.existsByUri("unknown-uri");

    assertFalse(exists);
  }

  @Test
  @Transactional
  void addResource() {
    Network network =
        networkRepository.saveAndFlush(
            Network.builder().uri("http://example.com").externalId("validId").build());
    Resource resource = createResource();
    network.addResource(resource);
    networkRepository.saveAndFlush(network);

    assertTrue(networkRepository.existsById(network.getId()));
    assertTrue(networkRepository.findById(network.getId()).get().getResources().contains(resource));
    assertTrue(resourceRepository.findById(resource.getId()).get().getNetworks().contains(network));
  }

  @Test
  @Transactional
  void addManager() {
    Network network =
        networkRepository.saveAndFlush(
            Network.builder().uri("http://example.com").externalId("validId").build());
    Person person = createPerson();
    network.addManager(person);
    networkRepository.saveAndFlush(network);

    assertTrue(networkRepository.existsById(network.getId()));
    assertTrue(networkRepository.findById(network.getId()).get().getManagers().contains(person));
    assertTrue(personRepository.findById(person.getId()).get().getNetworks().contains(network));
  }

  private Resource createResource() {
    Organization organization =
        organizationRepository.save(
            Organization.builder().name("test").externalId("biobank:1").build());
    DiscoveryService discoveryService =
        discoveryServiceRepository.save(DiscoveryService.builder().url("").name("").build());
    Resource resource =
        resourceRepository.saveAndFlush(
            Resource.builder()
                .organization(organization)
                .discoveryService(discoveryService)
                .sourceId("collection:1")
                .representatives(new HashSet<>())
                .name("test")
                .build());
    return resource;
  }

  private Person createPerson() {
    return personRepository.saveAndFlush(
        Person.builder()
            .subjectId("test")
            .name("John")
            .email("test@test.com")
            .resources(new HashSet<>())
            .networks(new HashSet<>())
            .build());
  }
}
