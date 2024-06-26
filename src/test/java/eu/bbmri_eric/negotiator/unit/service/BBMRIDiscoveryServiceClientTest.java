package eu.bbmri_eric.negotiator.unit.service;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import eu.bbmri_eric.negotiator.database.model.AccessForm;
import eu.bbmri_eric.negotiator.database.model.DiscoveryService;
import eu.bbmri_eric.negotiator.database.model.Organization;
import eu.bbmri_eric.negotiator.database.model.Resource;
import eu.bbmri_eric.negotiator.database.repository.AccessFormRepository;
import eu.bbmri_eric.negotiator.database.repository.DiscoveryServiceRepository;
import eu.bbmri_eric.negotiator.database.repository.OrganizationRepository;
import eu.bbmri_eric.negotiator.database.repository.ResourceRepository;
import eu.bbmri_eric.negotiator.dto.MolgenisBiobank;
import eu.bbmri_eric.negotiator.dto.MolgenisCollection;
import eu.bbmri_eric.negotiator.service.BBMRIDiscoveryServiceClientImpl;
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

@WireMockTest(httpPort = 8080)
@ExtendWith(MockitoExtension.class)
public class BBMRIDiscoveryServiceClientTest {

  @Mock DiscoveryServiceRepository discoveryServiceRepository;

  @Mock OrganizationRepository organizationRepository;

  @Mock AccessFormRepository accessFormRepository;

  @Mock ResourceRepository resourceRepository;

  @InjectMocks private BBMRIDiscoveryServiceClientImpl discoveryService;

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

  @BeforeEach
  public void createBiobanksAndCollections() {
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

    this.testBiobanks = new ArrayList<>(Arrays.asList(bb1, bb2, bb3, bb4));
    this.testCollections = new ArrayList<>(Arrays.asList(coll1, coll2, coll3, coll4));
  }

  @Test
  void testFindAllBiobanks() {
    String baseUrl = "http://localhost:8080/directory";
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode root = mapper.createObjectNode();
    ObjectNode biobank1 = mapper.createObjectNode();
    ObjectNode biobank2 = mapper.createObjectNode();
    ObjectNode biobank3 = mapper.createObjectNode();
    ObjectNode biobank4 = mapper.createObjectNode();

    ArrayNode biobanks = mapper.createArrayNode();
    biobank1.put("_href", "/api/v2/test_bb1");
    biobank1.put("id", "test_bb1");
    biobank1.put("name", "test_bb1");
    biobank2.put("_href", "/api/v2/test_bb2");
    biobank2.put("id", "test_bb2");
    biobank2.put("name", "test_bb2");
    biobank3.put("_href", "/api/v2/test_bb3");
    biobank3.put("id", "test_bb3");
    biobank3.put("name", "test_bb3");
    biobank4.put("_href", "/api/v2/test_bb4");
    biobank4.put("id", "test_bb4");
    biobank4.put("name", "test_bb4");

    biobanks.add(biobank1);
    biobanks.add(biobank2);
    biobanks.add(biobank3);
    biobanks.add(biobank4);

    root.set("items", biobanks);

    stubFor(
        get(urlEqualTo(
                "/directory/api/v2/eu_bbmri_eric_biobanks?num=10000&q=withdrawn%3D%3Dfalse&attrs=id,name"))
            .willReturn(
                aResponse().withHeader("Content-Type", "application/json").withJsonBody(root)));

    List retrievedBiobanks = new BBMRIDiscoveryServiceClientImpl(baseUrl).findAllBiobanks();
    assertTrue(this.testBiobanks.size() == retrievedBiobanks.size());
  }

  @Test
  void testFindAllCollections() {
    String baseUrl = "http://localhost:8080/directory";
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode root = mapper.createObjectNode();

    ObjectNode collection1 = mapper.createObjectNode();
    ObjectNode collection2 = mapper.createObjectNode();
    ObjectNode collection3 = mapper.createObjectNode();
    ObjectNode collection4 = mapper.createObjectNode();

    ObjectNode biobank1 = mapper.createObjectNode();
    ObjectNode biobank2 = mapper.createObjectNode();
    ObjectNode biobank3 = mapper.createObjectNode();
    ObjectNode biobank4 = mapper.createObjectNode();

    biobank1.put("_href", "/api/v2/test_bb1");
    biobank1.put("id", "test_bb1");
    biobank1.put("name", "test_bb1");
    biobank2.put("_href", "/api/v2/test_bb2");
    biobank2.put("id", "test_bb2");
    biobank2.put("name", "test_bb2");
    biobank3.put("_href", "/api/v2/test_bb3");
    biobank3.put("id", "test_bb3");
    biobank3.put("name", "test_bb3");
    biobank4.put("_href", "/api/v2/test_bb4");
    biobank4.put("id", "test_bb4");
    biobank4.put("name", "test_bb4");

    ArrayNode collections = mapper.createArrayNode();
    collection1.put("id", "test_coll1");
    collection1.put("name", "test_coll1");
    collection1.put("description", "test_coll1");
    collection1.put("biobank", biobank1);
    collection2.put("id", "test_coll2");
    collection2.put("name", "test_coll2");
    collection2.put("description", "test_coll2");
    collection2.put("biobank", biobank2);
    collection3.put("id", "test_coll3");
    collection3.put("name", "test_coll3");
    collection3.put("description", "test_coll3");
    collection3.put("biobank", biobank3);
    collection4.put("id", "test_coll4");
    collection4.put("name", "test_coll4");
    collection4.put("description", "test_coll4");
    collection4.put("biobank", biobank4);

    collections.add(collection1);
    collections.add(collection2);
    collections.add(collection3);
    collections.add(collection4);

    root.set("items", collections);

    stubFor(
        get(urlEqualTo(
                "/directory/api/v2/eu_bbmri_eric_collections?num=10000&attrs=id,name,description,biobank"))
            .willReturn(
                aResponse().withHeader("Content-Type", "application/json").withJsonBody(root)));

    List retrievedCollections = new BBMRIDiscoveryServiceClientImpl(baseUrl).findAllCollections();
    assertTrue(this.testBiobanks.size() == retrievedCollections.size());
  }

  @Test
  void testAddMissingOrganizationsWithOrgNotPresent() {
    when(organizationRepository.findByExternalId("test_bb1")).thenReturn(Optional.empty());
    when(organizationRepository.findByExternalId("test_bb2")).thenReturn(Optional.empty());
    when(organizationRepository.findByExternalId("test_bb3")).thenReturn(Optional.empty());
    when(organizationRepository.findByExternalId("test_bb4")).thenReturn(Optional.empty());
    discoveryService.addMissingOrganizations(testBiobanks);

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
  void testAddMissingOrganizationsWithOrgAlreadyPresent() {
    List<MolgenisBiobank> testModifyBiobanks =
        Arrays.asList(new MolgenisBiobank("test_bb1", "test_bb1_newname", "/api/v2/test_bb1"));
    when(organizationRepository.findByExternalId("test_bb1"))
        .thenReturn(
            Optional.of(Organization.builder().externalId("test_bb1").name("test_bb1").build()));
    discoveryService.addMissingOrganizations(testModifyBiobanks);
    verify(organizationRepository, times(1))
        .save(Organization.builder().externalId("test_bb1").name("test_bb1_newname").build());
  }

  @Test
  void testAddMissingResourceWithResourceNotPresent() {
    when(resourceRepository.findBySourceId("test_coll1")).thenReturn(Optional.empty());
    when(resourceRepository.findBySourceId("test_coll2")).thenReturn(Optional.empty());
    when(resourceRepository.findBySourceId("test_coll3")).thenReturn(Optional.empty());
    when(resourceRepository.findBySourceId("test_coll4")).thenReturn(Optional.empty());

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

    discoveryService.addMissingResources(testCollections);

    verify(resourceRepository, times(1))
        .save(
            Resource.builder()
                .sourceId("test_coll1")
                .name("test_coll_1")
                .description("test_coll_1")
                .organization(org1)
                .build());
    verify(resourceRepository, times(1))
        .save(
            Resource.builder()
                .sourceId("test_coll2")
                .name("test_coll_2")
                .description("test_coll_2")
                .organization(org1)
                .build());
    verify(resourceRepository, times(1))
        .save(
            Resource.builder()
                .sourceId("test_coll3")
                .name("test_coll_3")
                .description("test_coll_3")
                .organization(org1)
                .build());
    verify(resourceRepository, times(1))
        .save(
            Resource.builder()
                .sourceId("test_coll4")
                .name("test_coll_4")
                .description("test_coll_4")
                .organization(org1)
                .build());
  }

  @Test
  void testAddMissingResourcesWithResAlreadyPresent() {
    List<MolgenisCollection> testModifyResources =
        Arrays.asList(
            new MolgenisCollection(
                "test_coll1", "test_coll1_newname", "test_coll1", testBiobanks.get(0)));
    Organization org1 = Organization.builder().externalId("test_bb1").name("test_bb1").build();
    when(resourceRepository.findBySourceId("test_coll1"))
        .thenReturn(
            Optional.of(
                Resource.builder()
                    .sourceId("test_coll1")
                    .name("test_coll_1")
                    .description("test_coll_1")
                    .organization(org1)
                    .build()));
    when(discoveryServiceRepository.findById(Long.valueOf("1")))
        .thenReturn(Optional.of(new DiscoveryService()));
    when(accessFormRepository.findById(Long.valueOf("1")))
        .thenReturn(Optional.of(new AccessForm("name")));
    discoveryService.addMissingResources(testModifyResources);
    verify(resourceRepository, times(1))
        .save(
            Resource.builder()
                .sourceId("test_coll1")
                .name("test_coll_1_newname")
                .description("test_coll_1")
                .organization(org1)
                .build());
  }
}
