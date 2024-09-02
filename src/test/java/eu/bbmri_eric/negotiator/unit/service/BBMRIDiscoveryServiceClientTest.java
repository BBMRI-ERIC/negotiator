package eu.bbmri_eric.negotiator.unit.service;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import eu.bbmri_eric.negotiator.common.exceptions.EntityNotStorableException;
import eu.bbmri_eric.negotiator.discovery.DiscoveryService;
import eu.bbmri_eric.negotiator.discovery.DiscoveryServiceRepository;
import eu.bbmri_eric.negotiator.discovery.synchronization.DirectoryClient;
import eu.bbmri_eric.negotiator.form.AccessForm;
import eu.bbmri_eric.negotiator.form.repository.AccessFormRepository;
import eu.bbmri_eric.negotiator.governance.network.Network;
import eu.bbmri_eric.negotiator.governance.network.NetworkRepository;
import eu.bbmri_eric.negotiator.governance.organization.Organization;
import eu.bbmri_eric.negotiator.governance.organization.OrganizationRepository;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.governance.resource.ResourceRepository;
import eu.bbmri_eric.negotiator.governance.resource.dto.MolgenisBiobank;
import eu.bbmri_eric.negotiator.governance.resource.dto.MolgenisCollection;
import eu.bbmri_eric.negotiator.governance.resource.dto.MolgenisNetwork;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersUriSpec;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@WireMockTest(httpPort = 8080)
@ExtendWith(MockitoExtension.class)
public class BBMRIDiscoveryServiceClientTest {

  @Mock DiscoveryServiceRepository discoveryServiceRepository;

  @Mock OrganizationRepository organizationRepository;

  @Mock AccessFormRepository accessFormRepository;

  @Mock ResourceRepository resourceRepository;

  @Mock NetworkRepository networkRepository;

  @Mock private WebClient.Builder webClientBuilder;

  @Mock private WebClient webClient;

  @Mock private RequestHeadersUriSpec requestHeadersUriSpec;

  @Mock private RequestHeadersSpec requestHeadersSpec;

  @Mock private ResponseSpec responseSpec;

  @InjectMocks private DirectoryClient discoveryService;

  private AutoCloseable closeable;

  @BeforeEach
  void before() {
    closeable = MockitoAnnotations.openMocks(this);
  }

  @AfterEach
  void after() throws Exception {
    closeable.close();
  }

  private List<MolgenisBiobank> testBiobanks;

  private List<MolgenisCollection> testCollections;

  private List<MolgenisNetwork> testNetworks;

  @BeforeEach
  public void createBiobanksAndCollections() {
    MolgenisBiobank bb1 = new MolgenisBiobank("test_bb1", "test_bb1", "/assembler/v2/test_bb1");
    MolgenisBiobank bb2 = new MolgenisBiobank("test_bb2", "test_bb2", "/assembler/v2/test_bb2");
    MolgenisBiobank bb3 = new MolgenisBiobank("test_bb3", "test_bb3", "/assembler/v2/test_bb3");
    MolgenisBiobank bb4 = new MolgenisBiobank("test_bb4", "test_bb4", "/assembler/v2/test_bb4");
  }

  public void createBiobanksCollectionsAndNetworks() {
    MolgenisBiobank bb1 = new MolgenisBiobank("test_bb1", "test_bb1", "/api/v2/test_bb1");
    MolgenisBiobank bb2 = new MolgenisBiobank("test_bb2", "test_bb2", "/api/v2/test_bb2");
    MolgenisBiobank bb3 = new MolgenisBiobank("test_bb3", "test_bb3", "/api/v2/test_bb3");
    MolgenisBiobank bb4 = new MolgenisBiobank("test_bb4", "test_bb4", "/api/v2/test_bb4");

    MolgenisCollection coll1 =
        new MolgenisCollection("test_coll1", "test_coll1", "test_coll1", bb1);
    MolgenisCollection coll2 =
        new MolgenisCollection("test_coll2", "test_coll2", "test_coll2", bb2);
    MolgenisCollection coll3 =
        new MolgenisCollection("test_coll3", "test_coll3", "test_coll3", bb3);
    MolgenisCollection coll4 =
        new MolgenisCollection("test_coll4", "test_coll4", "test_coll4", bb4);

    MolgenisNetwork ntw1 =
        new MolgenisNetwork("test_ntw1", "test_ntw1", "test_ntw1@test.it", "https://test_ntw1.it");
    MolgenisNetwork ntw2 =
        new MolgenisNetwork("test_ntw2", "test_ntw2", "test_ntw2@test.it", "https://test_ntw2.it");
    MolgenisNetwork ntw3 =
        new MolgenisNetwork("test_ntw3", "test_ntw3", "test_ntw3@test.it", "https://test_ntw3.it");
    MolgenisNetwork ntw4 =
        new MolgenisNetwork("test_ntw4", "test_ntw4", "test_ntw4@test.it", "https://test_ntw4.it");

    this.testBiobanks = new ArrayList<>(Arrays.asList(bb1, bb2, bb3, bb4));
    this.testCollections = new ArrayList<>(Arrays.asList(coll1, coll2, coll3, coll4));
  }

  ArrayNode getTestBiobanks() {
    ObjectMapper mapper = new ObjectMapper();
    ArrayNode biobanks = mapper.createArrayNode();
    ObjectNode biobank1 = mapper.createObjectNode();
    ObjectNode biobank2 = mapper.createObjectNode();
    ObjectNode biobank3 = mapper.createObjectNode();
    ObjectNode biobank4 = mapper.createObjectNode();
    biobank1.put("_href", "/assembler/v2/test_bb1");
    biobank1.put("id", "test_bb1");
    biobank1.put("name", "test_bb1");
    biobank2.put("_href", "/assembler/v2/test_bb2");
    biobank2.put("id", "test_bb2");
    biobank2.put("name", "test_bb2");
    biobank3.put("_href", "/assembler/v2/test_bb3");
    biobank3.put("id", "test_bb3");
    biobank3.put("name", "test_bb3");
    biobank4.put("_href", "/assembler/v2/test_bb4");
    biobank4.put("id", "test_bb4");
    biobank4.put("name", "test_bb4");

    biobanks.add(biobank1);
    biobanks.add(biobank2);
    biobanks.add(biobank3);
    biobanks.add(biobank4);
    return biobanks;
  }

  ArrayNode getTestCollections() {
    String baseUrl = "http://localhost:8080/directory";
    ObjectMapper mapper = new ObjectMapper();

    ObjectNode collection1 = mapper.createObjectNode();
    ObjectNode collection2 = mapper.createObjectNode();
    ObjectNode collection3 = mapper.createObjectNode();
    ObjectNode collection4 = mapper.createObjectNode();

    ArrayNode biobanks = getTestBiobanks();

    ArrayNode collections = mapper.createArrayNode();
    collection1.put("id", "test_coll1");
    collection1.put("name", "test_coll1");
    collection1.put("description", "test_coll1");
    collection1.put("biobank", biobanks.get(0));
    collection2.put("id", "test_coll2");
    collection2.put("name", "test_coll2");
    collection2.put("description", "test_coll2");
    collection2.put("biobank", biobanks.get(1));
    collection3.put("id", "test_coll3");
    collection3.put("name", "test_coll3");
    collection3.put("description", "test_coll3");
    collection3.put("biobank", biobanks.get(2));
    collection4.put("id", "test_coll4");
    collection4.put("name", "test_coll4");
    collection4.put("description", "test_coll4");
    collection4.put("biobank", biobanks.get(3));

    collections.add(collection1);
    collections.add(collection2);
    collections.add(collection3);
    collections.add(collection4);

    return collections;
  }

  ArrayNode getTestNetworks() {
    String baseUrl = "http://localhost:8080/directory";
    ObjectMapper mapper = new ObjectMapper();

    ObjectNode network1 = mapper.createObjectNode();
    ObjectNode network2 = mapper.createObjectNode();
    ObjectNode network3 = mapper.createObjectNode();
    ObjectNode network4 = mapper.createObjectNode();

    ArrayNode networks = mapper.createArrayNode();

    network1.put("id", "test_ntw1");
    network1.put("name", "test_ntw1");
    network1.put("url", "https://test_ntw1.it");
    ObjectNode network1contact = mapper.createObjectNode();
    network1contact.put("_href", "https://network1contact.it");
    network1contact.put("name", "network1contact");
    network1contact.put("email", "test_ntw1@test.it");
    network1.put("contact", network1contact);
    network2.put("id", "test_ntw2");
    network2.put("name", "test_ntw2");
    network2.put("url", "https://test_ntw2.it");
    ObjectNode network2contact = mapper.createObjectNode();
    network2contact.put("_href", "https://network2contact.it");
    network2contact.put("name", "network2contact");
    network2contact.put("email", "test_ntw2@test.it");
    network2.put("contact", network2contact);
    network3.put("id", "test_ntw3");
    network3.put("name", "test_ntw3");
    network3.put("url", "https://test_ntw3.it");
    ObjectNode network3contact = mapper.createObjectNode();
    network3contact.put("_href", "https://network3contact.it");
    network3contact.put("name", "network3contact");
    network3contact.put("email", "test_ntw3@test.it");
    network3.put("contact", network3contact);
    network4.put("id", "test_ntw4");
    network4.put("name", "test_ntw4");
    network4.put("url", "https://test_ntw4.it");
    ObjectNode network4contact = mapper.createObjectNode();
    network4contact.put("_href", "https://network4contact.it");
    network4contact.put("name", "network4contact");
    network4contact.put("email", "test_ntw4@test.it");
    network4.put("contact", network4contact);

    networks.add(network1);
    networks.add(network2);
    networks.add(network3);
    networks.add(network4);

    return networks;
  }

  @Test
  void testSyncAllOrganizationsWhenAllMissing() {
    String baseUrl = "http://localhost:8080/directory";
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode root = mapper.createObjectNode();
    ArrayNode biobanks = getTestBiobanks();
    root.set("items", biobanks);

    String uriString = "/api/v2/eu_bbmri_eric_biobanks?num=10000&q=withdrawn==false&attrs=id,name";

    when(webClient.get()).thenReturn(requestHeadersUriSpec);
    when(requestHeadersUriSpec.uri(uriString)).thenReturn(requestHeadersSpec);
    when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(root.toString()));

    discoveryService.syncAllOrganizations();
    verify(organizationRepository, times(1))
        .save(Organization.builder().externalId("test_bb1").name("test_bb1").build());
    verify(organizationRepository, times(1))
        .save(Organization.builder().externalId("test_bb2").name("test_bb2").build());
    verify(organizationRepository, times(1))
        .save(Organization.builder().externalId("test_bb3").name("test_bb3").build());
    verify(organizationRepository, times(1))
        .save(Organization.builder().externalId("test_bb4").name("test_bb4").build());
  }

  @Test
  void testSyncAllOrganizationsWhenAllMissingStorageError() {
    String baseUrl = "http://localhost:8080/directory";
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode root = mapper.createObjectNode();
    ArrayNode biobanks = getTestBiobanks();
    ObjectNode bb1 = (ObjectNode) biobanks.get(0);
    root.set("items", biobanks);

    String uriString = "/api/v2/eu_bbmri_eric_biobanks?num=10000&q=withdrawn==false&attrs=id,name";

    when(webClient.get()).thenReturn(requestHeadersUriSpec);
    when(requestHeadersUriSpec.uri(uriString)).thenReturn(requestHeadersSpec);
    when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(root.toString()));

    Organization org1 = Organization.builder().externalId("test_bb1").name("test_bb1").build();

    when(organizationRepository.save(org1))
        .thenThrow(new DataIntegrityViolationException("Constraint violation"));

    assertThrows(
        EntityNotStorableException.class,
        () -> {
          discoveryService.syncAllOrganizations();
        });
  }

  @Test
  void testSyncAllResourcesWhenAllMissing() {
    String baseUrl = "http://localhost:8080/directory";
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode root = mapper.createObjectNode();
    ArrayNode collections = getTestCollections();
    root.set("items", collections);
    String uriString =
        "/api/v2/eu_bbmri_eric_collections?num=10000&attrs=id,name,description,biobank";

    when(webClient.get()).thenReturn(requestHeadersUriSpec);
    when(requestHeadersUriSpec.uri(uriString)).thenReturn(requestHeadersSpec);
    when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(root.toString()));

    when(discoveryServiceRepository.findById(Long.valueOf("1")))
        .thenReturn(Optional.of(new DiscoveryService()));
    when(accessFormRepository.findById(Long.valueOf("1")))
        .thenReturn(Optional.of(new AccessForm("name")));

    Organization org1 = Organization.builder().externalId("test_bb1").name("test_bb1").build();
    Organization org2 = Organization.builder().externalId("test_bb2").name("test_bb2").build();
    Organization org3 = Organization.builder().externalId("test_bb3").name("test_bb3").build();
    Organization org4 = Organization.builder().externalId("test_bb4").name("test_bb4").build();

    when(organizationRepository.findByExternalId("test_bb1")).thenReturn(Optional.of(org1));
    when(organizationRepository.findByExternalId("test_bb2")).thenReturn(Optional.of(org2));
    when(organizationRepository.findByExternalId("test_bb3")).thenReturn(Optional.of(org3));
    when(organizationRepository.findByExternalId("test_bb4")).thenReturn(Optional.of(org4));

    discoveryService.syncAllResources();

    verify(resourceRepository, times(1))
        .save(
            Resource.builder()
                .sourceId("test_coll1")
                .name("test_coll1")
                .description("test_coll1")
                .organization(org1)
                .build());
    verify(resourceRepository, times(1))
        .save(
            Resource.builder()
                .sourceId("test_coll2")
                .name("test_coll2")
                .description("test_coll2")
                .organization(org2)
                .build());
    verify(resourceRepository, times(1))
        .save(
            Resource.builder()
                .sourceId("test_coll3")
                .name("test_coll3")
                .description("test_coll3")
                .organization(org3)
                .build());
    verify(resourceRepository, times(1))
        .save(
            Resource.builder()
                .sourceId("test_coll4")
                .name("test_coll4")
                .description("test_coll4")
                .organization(org4)
                .build());
  }

  @Test
  void testSyncAllResourcesWhenAllMissingStorageError() {
    String baseUrl = "http://localhost:8080/directory";
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode root = mapper.createObjectNode();
    ArrayNode collections = getTestCollections();
    root.set("items", collections);
    String uriString =
        "/api/v2/eu_bbmri_eric_collections?num=10000&attrs=id,name,description,biobank";

    when(webClient.get()).thenReturn(requestHeadersUriSpec);
    when(requestHeadersUriSpec.uri(uriString)).thenReturn(requestHeadersSpec);
    when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(root.toString()));

    when(discoveryServiceRepository.findById(Long.valueOf("1")))
        .thenReturn(Optional.of(new DiscoveryService()));
    when(accessFormRepository.findById(Long.valueOf("1")))
        .thenReturn(Optional.of(new AccessForm("name")));

    Organization org1 = Organization.builder().externalId("test_bb1").name("test_bb1").build();
    Organization org2 = Organization.builder().externalId("test_bb2").name("test_bb2").build();
    Organization org3 = Organization.builder().externalId("test_bb3").name("test_bb3").build();
    Organization org4 = Organization.builder().externalId("test_bb4").name("test_bb4").build();

    lenient()
        .when(organizationRepository.findByExternalId("test_bb1"))
        .thenReturn(Optional.of(org1));
    lenient()
        .when(organizationRepository.findByExternalId("test_bb2"))
        .thenReturn(Optional.of(org2));
    lenient()
        .when(organizationRepository.findByExternalId("test_bb3"))
        .thenReturn(Optional.of(org3));
    lenient()
        .when(organizationRepository.findByExternalId("test_bb4"))
        .thenReturn(Optional.of(org4));

    Resource res1 =
        Resource.builder()
            .sourceId("test_coll1")
            .name("test_coll_1")
            .description("test_coll_1")
            .organization(org1)
            .build();

    when(resourceRepository.save(res1))
        .thenThrow(new DataIntegrityViolationException("Constraint violation"));

    assertThrows(
        EntityNotStorableException.class,
        () -> {
          discoveryService.syncAllResources();
        });
  }

  @Test
  void testSyncAllOrganizationsUpdateOrgAlreadyPresent() {
    String baseUrl = "http://localhost:8080/directory";
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode root = mapper.createObjectNode();
    ArrayNode biobanks = getTestBiobanks();

    String uriString = "/api/v2/eu_bbmri_eric_biobanks?num=10000&q=withdrawn==false&attrs=id,name";

    biobanks.remove(0);
    ObjectNode updatedBiobank1 = mapper.createObjectNode();
    updatedBiobank1.put("_href", "/assembler/v2/test_bb1");
    updatedBiobank1.put("id", "test_bb1");
    updatedBiobank1.put("name", "test_newname_bb1");

    biobanks.add(updatedBiobank1);
    root.set("items", biobanks);

    lenient()
        .when(organizationRepository.findByExternalId("test_bb1"))
        .thenReturn(
            Optional.of(Organization.builder().externalId("test_bb1").name("test_bb1").build()));

    lenient().when(webClient.get()).thenReturn(requestHeadersUriSpec);
    lenient().when(requestHeadersUriSpec.uri(uriString)).thenReturn(requestHeadersSpec);
    lenient().when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    lenient().when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(root.toString()));

    discoveryService.syncAllOrganizations();

    verify(organizationRepository, times(1))
        .save(Organization.builder().externalId("test_bb1").name("test_newname_bb1").build());
  }

  @Test
  void testSyncAllOrganizationsUpdateOrgAlreadyPresentStorageError() {
    String baseUrl = "http://localhost:8080/directory";
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode root = mapper.createObjectNode();
    ArrayNode biobanks = getTestBiobanks();

    String uriString = "/api/v2/eu_bbmri_eric_biobanks?num=10000&q=withdrawn==false&attrs=id,name";

    biobanks.removeAll();
    ObjectNode updatedBiobank1 = mapper.createObjectNode();
    updatedBiobank1.put("_href", "/api/v2/test_bb1");
    updatedBiobank1.put("id", "test_bb1");
    updatedBiobank1.put("name", "test_newname_bb1");

    biobanks.add(updatedBiobank1);
    root.set("items", biobanks);

    lenient()
        .when(organizationRepository.findByExternalId("test_bb1"))
        .thenReturn(
            Optional.of(Organization.builder().externalId("test_bb1").name("test_bb1").build()));

    lenient().when(webClient.get()).thenReturn(requestHeadersUriSpec);
    lenient().when(requestHeadersUriSpec.uri(uriString)).thenReturn(requestHeadersSpec);
    lenient().when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    lenient().when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(root.toString()));

    Organization org1 =
        Organization.builder().externalId("test_bb1").name("test_newname_bb1").build();
    when(organizationRepository.save(org1))
        .thenThrow(new DataIntegrityViolationException("Constraint violation"));

    assertThrows(
        EntityNotStorableException.class,
        () -> {
          discoveryService.syncAllOrganizations();
        });
  }

  @Test
  void testSyncAllResourcesUpdateResAlreadyPresentNameChange() {
    String baseUrl = "http://localhost:8080/directory";
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode root = mapper.createObjectNode();
    ArrayNode collections = getTestCollections();

    String uriString =
        "/api/v2/eu_bbmri_eric_collections?num=10000&attrs=id,name,description,biobank";

    collections.remove(0);

    ObjectNode updatedCollection1 = mapper.createObjectNode();
    updatedCollection1.put("id", "test_coll1");
    updatedCollection1.put("name", "test_coll1_newname");
    updatedCollection1.put("description", "test_coll1");
    updatedCollection1.put("biobank", getTestBiobanks().get(0));

    collections.add(updatedCollection1);

    root.set("items", collections);

    Organization org1 = Organization.builder().externalId("test_bb1").name("test_bb1").build();
    lenient()
        .when(resourceRepository.findBySourceId("test_coll1"))
        .thenReturn(
            Optional.of(
                Resource.builder()
                    .sourceId("test_coll1")
                    .name("test_coll1")
                    .description("test_coll1")
                    .organization(org1)
                    .build()));

    lenient().when(webClient.get()).thenReturn(requestHeadersUriSpec);
    lenient().when(requestHeadersUriSpec.uri(uriString)).thenReturn(requestHeadersSpec);
    lenient().when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    lenient().when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(root.toString()));

    lenient()
        .when(discoveryServiceRepository.findById(Long.valueOf("1")))
        .thenReturn(Optional.of(new DiscoveryService()));
    lenient()
        .when(accessFormRepository.findById(Long.valueOf("1")))
        .thenReturn(Optional.of(new AccessForm("name")));

    discoveryService.syncAllResources();

    verify(resourceRepository, times(1))
        .save(
            Resource.builder()
                .sourceId("test_coll1")
                .name("test_coll_1_newname")
                .description("test_coll_1")
                .organization(org1)
                .build());
  }

  @Test
  void testSyncAllResourcesUpdateResAlreadyPresentStorageError() {
    String baseUrl = "http://localhost:8080/directory";
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode root = mapper.createObjectNode();
    ArrayNode collections = getTestCollections();

    String uriString =
        "/api/v2/eu_bbmri_eric_collections?num=10000&attrs=id,name,description,biobank";

    collections.removeAll();

    ObjectNode updatedCollection1 = mapper.createObjectNode();
    updatedCollection1.put("id", "test_coll1");
    updatedCollection1.put("name", "test_coll1_newname");
    updatedCollection1.put("description", "test_coll1");
    updatedCollection1.put("biobank", getTestBiobanks().get(0));

    collections.add(updatedCollection1);

    root.set("items", collections);

    Organization org1 = Organization.builder().externalId("test_bb1").name("test_bb1").build();
    lenient()
        .when(resourceRepository.findBySourceId("test_coll1"))
        .thenReturn(
            Optional.of(
                Resource.builder()
                    .sourceId("test_coll1")
                    .name("test_coll_1")
                    .description("test_coll_1")
                    .organization(org1)
                    .build()));

    lenient().when(webClient.get()).thenReturn(requestHeadersUriSpec);
    lenient().when(requestHeadersUriSpec.uri(uriString)).thenReturn(requestHeadersSpec);
    lenient().when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    lenient().when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(root.toString()));

    lenient()
        .when(discoveryServiceRepository.findById(Long.valueOf("1")))
        .thenReturn(Optional.of(new DiscoveryService()));
    lenient()
        .when(accessFormRepository.findById(Long.valueOf("1")))
        .thenReturn(Optional.of(new AccessForm("name")));

    Resource res1 =
        Resource.builder()
            .sourceId("test_coll1")
            .name("test_coll_1_newname")
            .description("test_coll_1")
            .organization(org1)
            .build();

    when(resourceRepository.save(res1))
        .thenThrow(new DataIntegrityViolationException("Constraint violation"));

    assertThrows(
        EntityNotStorableException.class,
        () -> {
          discoveryService.syncAllResources();
        });
  }

  @Test
  void testSyncAllResourcesUpdateResAlreadyPresentDescriptionChange() {
    String baseUrl = "http://localhost:8080/directory";
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode root = mapper.createObjectNode();
    ArrayNode collections = getTestCollections();

    String uriString =
        "/api/v2/eu_bbmri_eric_collections?num=10000&attrs=id,name,description,biobank";

    collections.remove(0);

    ObjectNode updatedCollection1 = mapper.createObjectNode();
    updatedCollection1.put("id", "test_coll1");
    updatedCollection1.put("name", "test_coll1");
    updatedCollection1.put("description", "test_coll1_newdesc");
    updatedCollection1.put("biobank", getTestBiobanks().get(0));

    collections.add(updatedCollection1);

    root.set("items", collections);

    Organization org1 = Organization.builder().externalId("test_bb1").name("test_bb1").build();
    lenient()
        .when(resourceRepository.findBySourceId("test_coll1"))
        .thenReturn(
            Optional.of(
                Resource.builder()
                    .sourceId("test_coll1")
                    .name("test_coll1")
                    .description("test_coll1")
                    .organization(org1)
                    .build()));

    lenient().when(webClient.get()).thenReturn(requestHeadersUriSpec);
    lenient().when(requestHeadersUriSpec.uri(uriString)).thenReturn(requestHeadersSpec);
    lenient().when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    lenient().when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(root.toString()));

    lenient()
        .when(discoveryServiceRepository.findById(Long.valueOf("1")))
        .thenReturn(Optional.of(new DiscoveryService()));
    lenient()
        .when(accessFormRepository.findById(Long.valueOf("1")))
        .thenReturn(Optional.of(new AccessForm("name")));

    discoveryService.syncAllResources();

    verify(resourceRepository, times(1))
        .save(
            Resource.builder()
                .sourceId("test_coll1")
                .name("test_coll_1")
                .description("test_coll_1_newdesc")
                .organization(org1)
                .build());
  }

  @Test
  void testSyncAllNetworksWhenAllMissing() {
    String baseUrl = "http://localhost:8080/directory";
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode root = mapper.createObjectNode();
    ArrayNode networks = getTestNetworks();
    root.set("items", networks);

    String uriString = "api/v2/eu_bbmri_eric_networks?num=10000&attrs=id,name,url,contact";

    when(webClient.get()).thenReturn(requestHeadersUriSpec);
    when(requestHeadersUriSpec.uri(uriString)).thenReturn(requestHeadersSpec);
    when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(root.toString()));

    discoveryService.syncAllNetworks();
    verify(networkRepository, times(1))
        .save(
            Network.builder()
                .externalId("test_ntw1")
                .name("test_ntw1")
                .contactEmail("test_ntw1@test.it")
                .uri("https://test_ntw1.it")
                .build());
    verify(networkRepository, times(1))
        .save(
            Network.builder()
                .externalId("test_ntw2")
                .name("test_ntw2")
                .contactEmail("test_ntw2@test.it")
                .uri("https://test_ntw2.it")
                .build());
    verify(networkRepository, times(1))
        .save(
            Network.builder()
                .externalId("test_ntw3")
                .name("test_ntw3")
                .contactEmail("test_ntw3@test.it")
                .uri("https://test_ntw3.it")
                .build());
    verify(networkRepository, times(1))
        .save(
            Network.builder()
                .externalId("test_ntw4")
                .name("test_ntw4")
                .contactEmail("test_ntw4@test.it")
                .uri("https://test_ntw4.it")
                .build());
  }

  @Test
  void testSyncAllNetworksWhenAllMissingStorageError() {
    String baseUrl = "http://localhost:8080/directory";
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode root = mapper.createObjectNode();
    ArrayNode networks = getTestNetworks();
    root.set("items", networks);

    String uriString = "api/v2/eu_bbmri_eric_networks?num=10000&attrs=id,name,url,contact";

    when(webClient.get()).thenReturn(requestHeadersUriSpec);
    when(requestHeadersUriSpec.uri(uriString)).thenReturn(requestHeadersSpec);
    when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(root.toString()));

    Network net1 =
        Network.builder()
            .externalId("test_ntw1")
            .name("test_ntw1")
            .contactEmail("test_ntw1@test.it")
            .uri("https://test_ntw1.it")
            .build();

    when(networkRepository.save(net1))
        .thenThrow(new DataIntegrityViolationException("Constraint violation"));

    assertThrows(
        EntityNotStorableException.class,
        () -> {
          discoveryService.syncAllNetworks();
        });
  }

  @Test
  void testSyncAllNetworksWhenAllMissingEmptyUrl() {
    String baseUrl = "http://localhost:8080/directory";
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode root = mapper.createObjectNode();
    ArrayNode networks = getTestNetworks();
    ObjectNode net1 = (ObjectNode) networks.get(0);
    net1.remove("url");
    networks.remove(0);
    networks.add(net1);

    root.set("items", networks);

    String uriString = "api/v2/eu_bbmri_eric_networks?num=10000&attrs=id,name,url,contact";

    when(webClient.get()).thenReturn(requestHeadersUriSpec);
    when(requestHeadersUriSpec.uri(uriString)).thenReturn(requestHeadersSpec);
    when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(root.toString()));

    discoveryService.syncAllNetworks();
    verify(networkRepository, times(1))
        .save(
            Network.builder()
                .externalId("test_ntw1")
                .name("test_ntw1")
                .contactEmail("test_ntw1@test.it")
                .uri("")
                .build());
    verify(networkRepository, times(1))
        .save(
            Network.builder()
                .externalId("test_ntw2")
                .name("test_ntw2")
                .contactEmail("test_ntw2@test.it")
                .uri("https://test_ntw2.it")
                .build());
    verify(networkRepository, times(1))
        .save(
            Network.builder()
                .externalId("test_ntw3")
                .name("test_ntw3")
                .contactEmail("test_ntw3@test.it")
                .uri("https://test_ntw3.it")
                .build());
    verify(networkRepository, times(1))
        .save(
            Network.builder()
                .externalId("test_ntw4")
                .name("test_ntw4")
                .contactEmail("test_ntw4@test.it")
                .uri("https://test_ntw4.it")
                .build());
  }

  @Test
  void testSyncAllNetworksUpdateNetworkAlreadyPresentNameChange() {
    String baseUrl = "http://localhost:8080/directory";
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode root = mapper.createObjectNode();
    ArrayNode networks = getTestNetworks();
    String uriString = "api/v2/eu_bbmri_eric_networks?num=10000&attrs=id,name,url,contact";

    networks.remove(0);

    ObjectNode updatedNetwork1 = mapper.createObjectNode();
    updatedNetwork1.put("id", "test_ntw1");
    updatedNetwork1.put("name", "test_updt_ntw1");
    updatedNetwork1.put("url", "https://test_ntw1.it");
    ObjectNode network1contact = mapper.createObjectNode();
    network1contact.put("_href", "https://network1contact.it");
    network1contact.put("name", "network1contact");
    network1contact.put("email", "test_ntw1@test.it");
    updatedNetwork1.put("contact", network1contact);

    networks.add(updatedNetwork1);

    root.set("items", networks);

    lenient()
        .when(networkRepository.findByExternalId("test_ntw1"))
        .thenReturn(
            Optional.of(
                Network.builder()
                    .externalId("test_ntw1")
                    .name("test_ntw1")
                    .uri("https://test_ntw1.it")
                    .contactEmail("test_ntw1@test.it")
                    .build()));

    lenient().when(webClient.get()).thenReturn(requestHeadersUriSpec);
    lenient().when(requestHeadersUriSpec.uri(uriString)).thenReturn(requestHeadersSpec);
    lenient().when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    lenient().when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(root.toString()));

    discoveryService.syncAllNetworks();

    verify(networkRepository, times(1))
        .save(
            Network.builder()
                .externalId("test_ntw1")
                .name("test_updt_ntw1")
                .uri("https://test_ntw1.it")
                .contactEmail("test_ntw1@test.it")
                .build());
  }

  @Test
  void testSyncAllNetworksUpdateNetworkAlreadyPresentStorageError() {
    String baseUrl = "http://localhost:8080/directory";
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode root = mapper.createObjectNode();
    ArrayNode networks = getTestNetworks();
    String uriString = "api/v2/eu_bbmri_eric_networks?num=10000&attrs=id,name,url,contact";

    networks.removeAll();

    ObjectNode updatedNetwork1 = mapper.createObjectNode();
    updatedNetwork1.put("id", "test_ntw1");
    updatedNetwork1.put("name", "test_updt_ntw1");
    updatedNetwork1.put("url", "https://test_ntw1.it");
    ObjectNode network1contact = mapper.createObjectNode();
    network1contact.put("_href", "https://network1contact.it");
    network1contact.put("name", "network1contact");
    network1contact.put("email", "test_ntw1@test.it");
    updatedNetwork1.put("contact", network1contact);

    networks.add(updatedNetwork1);

    root.set("items", networks);

    lenient()
        .when(networkRepository.findByExternalId("test_ntw1"))
        .thenReturn(
            Optional.of(
                Network.builder()
                    .externalId("test_ntw1")
                    .name("test_ntw1")
                    .uri("https://test_ntw1.it")
                    .contactEmail("test_ntw1@test.it")
                    .build()));

    lenient().when(webClient.get()).thenReturn(requestHeadersUriSpec);
    lenient().when(requestHeadersUriSpec.uri(uriString)).thenReturn(requestHeadersSpec);
    lenient().when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    lenient().when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(root.toString()));

    Network net1 =
        Network.builder()
            .externalId("test_ntw1")
            .name("test_updt_ntw1")
            .uri("https://test_ntw1.it")
            .contactEmail("test_ntw1@test.it")
            .build();

    when(networkRepository.save(net1))
        .thenThrow(new DataIntegrityViolationException("Constraint violation"));

    assertThrows(
        EntityNotStorableException.class,
        () -> {
          discoveryService.syncAllNetworks();
        });
  }

  @Test
  void testSyncAllNetworksUpdateNetworkAlreadyPresentUrlChange() {
    String baseUrl = "http://localhost:8080/directory";
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode root = mapper.createObjectNode();
    ArrayNode networks = getTestNetworks();
    String uriString = "api/v2/eu_bbmri_eric_networks?num=10000&attrs=id,name,url,contact";

    networks.remove(0);

    ObjectNode updatedNetwork1 = mapper.createObjectNode();
    updatedNetwork1.put("id", "test_ntw1");
    updatedNetwork1.put("name", "test_ntw1");
    updatedNetwork1.put("url", "https://test_updt_ntw1.it");
    ObjectNode network1contact = mapper.createObjectNode();
    network1contact.put("_href", "https://network1contact.it");
    network1contact.put("name", "network1contact");
    network1contact.put("email", "test_ntw1@test.it");
    updatedNetwork1.put("contact", network1contact);

    networks.add(updatedNetwork1);

    root.set("items", networks);

    lenient()
        .when(networkRepository.findByExternalId("test_ntw1"))
        .thenReturn(
            Optional.of(
                Network.builder()
                    .externalId("test_ntw1")
                    .name("test_ntw1")
                    .uri("https://test_ntw1.it")
                    .contactEmail("test_ntw1@test.it")
                    .build()));

    lenient().when(webClient.get()).thenReturn(requestHeadersUriSpec);
    lenient().when(requestHeadersUriSpec.uri(uriString)).thenReturn(requestHeadersSpec);
    lenient().when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    lenient().when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(root.toString()));

    discoveryService.syncAllNetworks();

    verify(networkRepository, times(1))
        .save(
            Network.builder()
                .externalId("test_ntw1")
                .name("test_ntw1")
                .uri("https://test_updt_ntw1.it")
                .contactEmail("test_ntw1@test.it")
                .build());
  }

  @Test
  void testSyncAllNetworksUpdateNetworkAlreadyPresentEmailChange() {
    String baseUrl = "http://localhost:8080/directory";
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode root = mapper.createObjectNode();
    ArrayNode networks = getTestNetworks();
    String uriString = "api/v2/eu_bbmri_eric_networks?num=10000&attrs=id,name,url,contact";

    networks.remove(0);

    ObjectNode updatedNetwork1 = mapper.createObjectNode();
    updatedNetwork1.put("id", "test_ntw1");
    updatedNetwork1.put("name", "test_ntw1");
    updatedNetwork1.put("url", "https://test_ntw1.it");
    ObjectNode network1contact = mapper.createObjectNode();
    network1contact.put("_href", "https://network1contact.it");
    network1contact.put("name", "network1contact");
    network1contact.put("email", "test_updt_ntw1@test.it");
    updatedNetwork1.put("contact", network1contact);

    networks.add(updatedNetwork1);

    root.set("items", networks);

    lenient()
        .when(networkRepository.findByExternalId("test_ntw1"))
        .thenReturn(
            Optional.of(
                Network.builder()
                    .externalId("test_ntw1")
                    .name("test_ntw1")
                    .uri("https://test_ntw1.it")
                    .contactEmail("test_ntw1@test.it")
                    .build()));

    lenient().when(webClient.get()).thenReturn(requestHeadersUriSpec);
    lenient().when(requestHeadersUriSpec.uri(uriString)).thenReturn(requestHeadersSpec);
    lenient().when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    lenient().when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(root.toString()));

    discoveryService.syncAllNetworks();

    verify(networkRepository, times(1))
        .save(
            Network.builder()
                .externalId("test_ntw1")
                .name("test_ntw1")
                .uri("https://test_ntw1.it")
                .contactEmail("test_updt_ntw1@test.it")
                .build());
  }

  @Test
  void testSyncAllOrganizationWithMolgenisNotRecheable() {
    String uriString = "/api/v2/eu_bbmri_eric_biobanks?num=10000&q=withdrawn==false&attrs=id,name";

    when(webClient.get()).thenReturn(requestHeadersUriSpec);
    when(requestHeadersUriSpec.uri(uriString)).thenReturn(requestHeadersSpec);
    when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.bodyToMono(String.class))
        .thenReturn(
            Mono.error(new WebClientResponseException(500, "Not Recheable", null, null, null)));

    assertThrows(
        WebClientResponseException.class,
        () -> {
          discoveryService.syncAllOrganizations();
        });
    ;
  }

  @Test
  void testSyncAllResourcesWithMolgenisNotRecheable() {
    String uriString =
        "/api/v2/eu_bbmri_eric_collections?num=10000&attrs=id,name,description,biobank";

    when(webClient.get()).thenReturn(requestHeadersUriSpec);
    when(requestHeadersUriSpec.uri(uriString)).thenReturn(requestHeadersSpec);
    when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.bodyToMono(String.class))
        .thenReturn(
            Mono.error(new WebClientResponseException(500, "Not Recheable", null, null, null)));

    assertThrows(
        WebClientResponseException.class,
        () -> {
          discoveryService.syncAllResources();
        });
    ;
  }

  @Test
  void testSyncAllOrganizationsWithMolgenisNotRecheable() {
    String uriString = "/api/v2/eu_bbmri_eric_biobanks?num=10000&q=withdrawn==false&attrs=id,name";
    when(webClient.get()).thenReturn(requestHeadersUriSpec);
    when(requestHeadersUriSpec.uri(uriString)).thenReturn(requestHeadersSpec);
    when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.bodyToMono(String.class))
        .thenReturn(
            Mono.error(new WebClientResponseException(500, "Not Recheable", null, null, null)));

    assertThrows(
        WebClientResponseException.class,
        () -> {
          discoveryService.syncAllOrganizations();
        });
    ;
  }

  @Test
  void testSyncAllNetworksWithMolgenisNotRecheable() {
    String uriString = "api/v2/eu_bbmri_eric_networks?num=10000&attrs=id,name,url,contact";
    when(webClient.get()).thenReturn(requestHeadersUriSpec);
    when(requestHeadersUriSpec.uri(uriString)).thenReturn(requestHeadersSpec);
    when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.bodyToMono(String.class))
        .thenReturn(
            Mono.error(new WebClientResponseException(500, "Not Recheable", null, null, null)));

    assertThrows(
        WebClientResponseException.class,
        () -> {
          discoveryService.syncAllNetworks();
        });
    ;
  }
}
