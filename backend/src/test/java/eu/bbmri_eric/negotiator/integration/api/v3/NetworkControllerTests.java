package eu.bbmri_eric.negotiator.integration.api.v3;

import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import eu.bbmri_eric.negotiator.governance.network.Network;
import eu.bbmri_eric.negotiator.governance.network.NetworkCreateDTO;
import eu.bbmri_eric.negotiator.governance.network.NetworkDTO;
import eu.bbmri_eric.negotiator.governance.network.NetworkRepository;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import eu.bbmri_eric.negotiator.util.WithMockNegotiatorUser;
import jakarta.transaction.Transactional;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@IntegrationTest(loadTestData = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class NetworkControllerTests {

  private static final String NETWORKS_URL = "/v3/networks";

  @Autowired private WebApplicationContext context;

  private MockMvc mockMvc;
  @Autowired private NetworkRepository networkRepository;

  @BeforeEach
  public void before() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @Test
  @WithMockUser("researcher")
  public void getNetworks_validURL_allNetworks() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get(NETWORKS_URL))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/hal+json"))
        .andExpect(jsonPath("$.page.totalElements", is(3)))
        .andExpect(jsonPath("$._embedded.networks.length()", is(3)))
        .andExpect(jsonPath("$._embedded.networks.[0].id", is(1)))
        .andExpect(jsonPath("$._embedded.networks.[1].id", is(2)))
        .andExpect(jsonPath("$._embedded.networks.[2].id", is(3)));
  }

  @Test
  @WithMockUser("researcher")
  public void getNetwork_validNetworkId_Network() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get(NETWORKS_URL + "/1"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/hal+json"))
        .andExpect(jsonPath("$.id", is(1)));
  }

  @Test
  @WithMockUser("researcher")
  public void getNetworkResources_validNetworkId_returnsResources() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get(NETWORKS_URL + "/1/resources"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/hal+json"))
        .andExpect(jsonPath("$.page.totalElements", is(3)))
        .andExpect(jsonPath("$._embedded.resources.length()", is(3)))
        .andExpect(jsonPath("$._embedded.resources.[0].id", is(4)))
        .andExpect(jsonPath("$._embedded.resources.[1].id", is(5)))
        .andExpect(jsonPath("$._embedded.resources.[2].id", is(6)));
  }

  @Test
  @WithMockUser("researcher")
  public void getNetworkManagers_validNetworkId_returnsManagers() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get(NETWORKS_URL + "/1/managers"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/hal+json"))
        .andExpect(jsonPath("$.page.totalElements", is(2)))
        .andExpect(jsonPath("$._embedded.users.length()", is(2)))
        .andExpect(jsonPath("$._embedded.users.[0].id", is("101")))
        .andExpect(jsonPath("$._embedded.users.[1].id", is("102")));
  }

  @Test
  @WithMockNegotiatorUser(id = 101L)
  public void getNetworkNegotiations_validNetworkId_returnsNegotiations() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get(NETWORKS_URL + "/1/negotiations"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/hal+json"))
        .andExpect(jsonPath("$.page.totalElements", is(5)))
        .andExpect(jsonPath("$._embedded.negotiations.length()", is(5)))
        .andExpect(jsonPath("$._embedded.negotiations.[0].id", is("negotiation-6")))
        .andExpect(jsonPath("$._embedded.negotiations.[1].id", is("negotiation-1")))
        .andExpect(jsonPath("$._embedded.negotiations.[2].id", is("negotiation-5")))
        .andExpect(jsonPath("$._embedded.negotiations.[3].id", is("negotiation-3")))
        .andExpect(jsonPath("$._embedded.negotiations.[4].id", is("negotiation-4")));
  }

  @Test
  @WithMockNegotiatorUser(id = 101L)
  public void getNetworkNegotiations_filterByOrgId_returnsNegotiations() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get(NETWORKS_URL + "/1/negotiations?organizationId=5"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/hal+json"))
        .andExpect(jsonPath("$.page.totalElements", is(0)));
    mockMvc
        .perform(MockMvcRequestBuilders.get(NETWORKS_URL + "/1/negotiations?organizationId=6"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/hal+json"))
        .andExpect(jsonPath("$.page.totalElements", is(2)))
        .andExpect(jsonPath("$._embedded.negotiations.length()", is(2)))
        .andExpect(jsonPath("$._embedded.negotiations.[0].id", is("negotiation-5")));
    mockMvc
        .perform(MockMvcRequestBuilders.get(NETWORKS_URL + "/1/negotiations?organizationId=4"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/hal+json"))
        .andExpect(jsonPath("$.page.totalElements", is(5)))
        .andExpect(jsonPath("$._embedded.negotiations.length()", is(5)))
        .andExpect(jsonPath("$._embedded.negotiations.[0].id", is("negotiation-6")));
  }

  @Test
  @WithMockNegotiatorUser(id = 101L)
  public void getNetworkNegotiations_filterBy2OrgId_returnsNegotiations() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(
                NETWORKS_URL + "/1/negotiations?organizationId=5&organizationId=6"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/hal+json"))
        .andExpect(jsonPath("$.page.totalElements", is(2)));
  }

  @Test
  @WithMockNegotiatorUser(id = 102L)
  public void getNetworkNegotiations_sortAsc_Ok() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get(NETWORKS_URL + "/1/negotiations?sortOrder=ASC"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/hal+json"))
        .andExpect(jsonPath("$.page.totalElements", is(5)))
        .andExpect(jsonPath("$._embedded.negotiations.length()", is(5)))
        .andExpect(jsonPath("$._embedded.negotiations.[0].id", is("negotiation-4")))
        .andExpect(jsonPath("$._embedded.negotiations.[1].id", is("negotiation-3")))
        .andExpect(jsonPath("$._embedded.negotiations.[2].id", is("negotiation-5")))
        .andExpect(jsonPath("$._embedded.negotiations.[3].id", is("negotiation-1")))
        .andExpect(jsonPath("$._embedded.negotiations.[4].id", is("negotiation-6")));
  }

  @Test
  @WithMockNegotiatorUser(id = 102L)
  public void getNetworkNegotiations_sortByTitle_Ok() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get(NETWORKS_URL + "/1/negotiations?sortBy=title"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/hal+json"))
        .andExpect(jsonPath("$.page.totalElements", is(5)))
        .andExpect(jsonPath("$._embedded.negotiations.length()", is(5)))
        .andExpect(jsonPath("$._embedded.negotiations.[0].id", is("negotiation-5")))
        .andExpect(jsonPath("$._embedded.negotiations.[1].id", is("negotiation-4")))
        .andExpect(jsonPath("$._embedded.negotiations.[2].id", is("negotiation-3")))
        .andExpect(jsonPath("$._embedded.negotiations.[3].id", is("negotiation-6")))
        .andExpect(jsonPath("$._embedded.negotiations.[4].id", is("negotiation-1")));
  }

  @Test
  @WithMockNegotiatorUser(id = 102L)
  public void getNetworkNegotiations_filterByStatus_ok() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get(NETWORKS_URL + "/1/negotiations?status=IN_PROGRESS"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/hal+json"))
        .andExpect(jsonPath("$.page.totalElements", is(2)))
        .andExpect(jsonPath("$._embedded.negotiations.length()", is(2)))
        .andExpect(jsonPath("$._embedded.negotiations.[0].id", is("negotiation-1")))
        .andExpect(jsonPath("$._embedded.negotiations.[1].id", is("negotiation-3")));
  }

  @Test
  @WithMockNegotiatorUser(id = 102L)
  public void testNetworkNegotiations_filterByCreationDate() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(
                NETWORKS_URL + "/1/negotiations?createdAfter=2024-01-09&createdBefore=2024-09-01"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/hal+json"))
        .andExpect(jsonPath("$.page.totalElements", is(3)))
        .andExpect(jsonPath("$._embedded.negotiations.length()", is(3)))
        .andExpect(jsonPath("$._embedded.negotiations.[0].id", is("negotiation-5")))
        .andExpect(jsonPath("$._embedded.negotiations.[1].id", is("negotiation-3")))
        .andExpect(jsonPath("$._embedded.negotiations.[2].id", is("negotiation-4")));
  }

  @Test
  @WithUserDetails("admin")
  public void deleteNetwork_NetworkExists_returns204() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.delete(NETWORKS_URL + "/1"))
        .andExpect(status().isNoContent());

    Optional<Network> network = networkRepository.findById(1L);
    assertFalse(network.isPresent());
  }

  @Test
  @WithUserDetails("admin")
  public void deleteNetwork_noNetworkExists_throws400() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.delete(NETWORKS_URL + "/314"))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithUserDetails("admin")
  public void postNetwork_validNetwork_returnsNewNetwork() throws Exception {
    NetworkCreateDTO networkDTO =
        NetworkCreateDTO.builder()
            .externalId("externalId")
            .contactEmail("new@negotiator.com")
            .name("newNetwork")
            .description("newNetwork")
            .uri("http://newuri.org")
            .build();
    String requestBody = TestUtils.jsonFromRequest(Arrays.asList(networkDTO));
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post(NETWORKS_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(content().contentType("application/hal+json"))
            .andExpect(jsonPath("$._embedded.networks[0].name", is(networkDTO.getName())))
            .andExpect(
                jsonPath("$._embedded.networks[0].externalId", is(networkDTO.getExternalId())))
            .andExpect(jsonPath("$._embedded.networks[0].uri", is(networkDTO.getUri())))
            .andExpect(
                jsonPath("$._embedded.networks[0].contactEmail", is(networkDTO.getContactEmail())))
            .andReturn();

    long id =
        JsonPath.parse(result.getResponse().getContentAsString())
            .read("$._embedded.networks[0].id", Long.class);
    Optional<Network> network = networkRepository.findById(id);
    assert network.isPresent();
    assertEquals(networkDTO.getName(), network.get().getName());
  }

  @Test
  @WithUserDetails("admin")
  public void postNetwork_batch() throws Exception {
    NetworkCreateDTO networkDTO1 =
        NetworkCreateDTO.builder()
            .externalId("network1")
            .contactEmail("network1@negotiator.com")
            .name("network1")
            .description("network1")
            .uri("http://network1.org")
            .build();
    NetworkCreateDTO networkDTO2 =
        NetworkCreateDTO.builder()
            .externalId("network2")
            .contactEmail("network2@negotiator.com")
            .name("network2")
            .description("network2")
            .uri("http://network2.org")
            .build();
    String requestBody = TestUtils.jsonFromRequest(Arrays.asList(networkDTO1, networkDTO2));
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post(NETWORKS_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(content().contentType("application/hal+json"))
            .andExpect(jsonPath("$._embedded.networks[0].name", is(networkDTO1.getName())))
            .andExpect(
                jsonPath("$._embedded.networks[0].externalId", is(networkDTO1.getExternalId())))
            .andExpect(jsonPath("$._embedded.networks[0].uri", is(networkDTO1.getUri())))
            .andExpect(
                jsonPath("$._embedded.networks[0].contactEmail", is(networkDTO1.getContactEmail())))
            .andExpect(jsonPath("$._embedded.networks[1].name", is(networkDTO2.getName())))
            .andExpect(
                jsonPath("$._embedded.networks[1].externalId", is(networkDTO2.getExternalId())))
            .andExpect(jsonPath("$._embedded.networks[1].uri", is(networkDTO2.getUri())))
            .andExpect(
                jsonPath("$._embedded.networks[1].contactEmail", is(networkDTO2.getContactEmail())))
            .andReturn();

    long id1 =
        JsonPath.parse(result.getResponse().getContentAsString())
            .read("$._embedded.networks[0].id", Long.class);
    Optional<Network> network1 = networkRepository.findById(id1);
    assert network1.isPresent();
    assertEquals(networkDTO1.getName(), network1.get().getName());
    long id2 =
        JsonPath.parse(result.getResponse().getContentAsString())
            .read("$._embedded.networks[1].id", Long.class);
    Optional<Network> network2 = networkRepository.findById(id2);
    assert network2.isPresent();
    assertEquals(networkDTO2.getName(), network2.get().getName());
  }

  @Test
  @WithUserDetails("admin")
  public void putNetwork() throws Exception {
    NetworkCreateDTO networkDTO1 =
        NetworkCreateDTO.builder()
            .externalId("network1")
            .contactEmail("network1@negotiator.com")
            .name("network1")
            .description("Network 1")
            .uri("http://network1.org")
            .build();
    NetworkCreateDTO networkDTO2 =
        NetworkCreateDTO.builder()
            .externalId("network2")
            .contactEmail("network2@negotiator.com")
            .name("network2")
            .description("Network 2")
            .uri("http://network2.org")
            .build();
    String requestBody = TestUtils.jsonFromRequest(Arrays.asList(networkDTO1, networkDTO2));
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post(NETWORKS_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(content().contentType("application/hal+json"))
            .andExpect(jsonPath("$._embedded.networks[0].name", is(networkDTO1.getName())))
            .andExpect(
                jsonPath("$._embedded.networks[0].description", is(networkDTO1.getDescription())))
            .andExpect(
                jsonPath("$._embedded.networks[0].externalId", is(networkDTO1.getExternalId())))
            .andExpect(jsonPath("$._embedded.networks[0].uri", is(networkDTO1.getUri())))
            .andExpect(
                jsonPath("$._embedded.networks[0].contactEmail", is(networkDTO1.getContactEmail())))
            .andExpect(jsonPath("$._embedded.networks[1].name", is(networkDTO2.getName())))
            .andExpect(
                jsonPath("$._embedded.networks[1].description", is(networkDTO2.getDescription())))
            .andExpect(
                jsonPath("$._embedded.networks[1].externalId", is(networkDTO2.getExternalId())))
            .andExpect(jsonPath("$._embedded.networks[1].uri", is(networkDTO2.getUri())))
            .andExpect(
                jsonPath("$._embedded.networks[1].contactEmail", is(networkDTO2.getContactEmail())))
            .andReturn();

    long id1 =
        JsonPath.parse(result.getResponse().getContentAsString())
            .read("$._embedded.networks[0].id", Long.class);
    Optional<Network> network1 = networkRepository.findById(id1);
    assert network1.isPresent();
    assertEquals(networkDTO1.getName(), network1.get().getName());
    long id2 =
        JsonPath.parse(result.getResponse().getContentAsString())
            .read("$._embedded.networks[1].id", Long.class);
    Optional<Network> network2 = networkRepository.findById(id2);
    assert network2.isPresent();
    assertEquals(networkDTO2.getName(), network2.get().getName());
    NetworkCreateDTO networkUpdateDTO1 =
        NetworkCreateDTO.builder()
            .externalId("network1")
            .contactEmail("newnetwork1@negotiator.com")
            .name("newnetwork1")
            .description("newdescnetwork1")
            .uri("http://network1.org")
            .build();
    String updateRequestBody = TestUtils.jsonFromRequest(networkUpdateDTO1);
    MvcResult updateResult =
        mockMvc
            .perform(
                MockMvcRequestBuilders.put(NETWORKS_URL + "/" + id1)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(updateRequestBody))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/hal+json"))
            .andExpect(jsonPath("$.name", is(networkUpdateDTO1.getName())))
            .andExpect(jsonPath("$.description", is(networkUpdateDTO1.getDescription())))
            .andExpect(jsonPath("$.externalId", is(networkUpdateDTO1.getExternalId())))
            .andExpect(jsonPath("$.uri", is(networkUpdateDTO1.getUri())))
            .andExpect(jsonPath("$.contactEmail", is(networkUpdateDTO1.getContactEmail())))
            .andReturn();
    Optional<Network> updatedNetwork1 = networkRepository.findById(id1);
    assertEquals(networkUpdateDTO1.getName(), updatedNetwork1.get().getName());
    assertEquals(networkUpdateDTO1.getContactEmail(), updatedNetwork1.get().getContactEmail());
  }

  @Test
  @WithUserDetails("admin")
  public void postNetwork_batch_same_network_name() throws Exception {
    NetworkCreateDTO networkDTO1 =
        NetworkCreateDTO.builder()
            .externalId("network3")
            .contactEmail("network3@negotiator.com")
            .name("network")
            .description("network")
            .uri("http://network3.org")
            .build();
    NetworkCreateDTO networkDTO2 =
        NetworkCreateDTO.builder()
            .externalId("network4")
            .contactEmail("network4@negotiator.com")
            .name("network")
            .description("network")
            .uri("http://network4.org")
            .build();
    String requestBody = TestUtils.jsonFromRequest(Arrays.asList(networkDTO1, networkDTO2));
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post(NETWORKS_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(content().contentType("application/hal+json"))
            .andExpect(jsonPath("$._embedded.networks[0].name", is(networkDTO1.getName())))
            .andExpect(
                jsonPath("$._embedded.networks[0].externalId", is(networkDTO1.getExternalId())))
            .andExpect(jsonPath("$._embedded.networks[0].uri", is(networkDTO1.getUri())))
            .andExpect(
                jsonPath("$._embedded.networks[0].contactEmail", is(networkDTO1.getContactEmail())))
            .andExpect(jsonPath("$._embedded.networks[1].name", is(networkDTO2.getName())))
            .andExpect(
                jsonPath("$._embedded.networks[1].externalId", is(networkDTO2.getExternalId())))
            .andExpect(jsonPath("$._embedded.networks[1].uri", is(networkDTO2.getUri())))
            .andExpect(
                jsonPath("$._embedded.networks[1].contactEmail", is(networkDTO2.getContactEmail())))
            .andReturn();

    long id1 =
        JsonPath.parse(result.getResponse().getContentAsString())
            .read("$._embedded.networks[0].id", Long.class);
    Optional<Network> network1 = networkRepository.findById(id1);
    assert network1.isPresent();
    assertEquals(networkDTO1.getName(), network1.get().getName());
    long id2 =
        JsonPath.parse(result.getResponse().getContentAsString())
            .read("$._embedded.networks[1].id", Long.class);
    Optional<Network> network2 = networkRepository.findById(id2);
    assert network2.isPresent();
    assertEquals(networkDTO2.getName(), network2.get().getName());
  }

  @Test
  @WithUserDetails("admin")
  public void postNetwork_alreadyExists_throws400() throws Exception {
    NetworkCreateDTO networkDTO =
        NetworkCreateDTO.builder()
            .externalId("bbmri-eric:ID:SE_890:network:bbmri-eric")
            .contactEmail("office@negotiator.org")
            .name("network-1")
            .description("network-1")
            .uri("https://network-1/")
            .build();
    String requestBody = TestUtils.jsonFromRequest(networkDTO);
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(NETWORKS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithUserDetails("admin")
  public void putNetwork_validNetwork_returnsUpdatedNetwork() throws Exception {
    NetworkDTO networkDTO =
        NetworkDTO.builder()
            .id(1L)
            .externalId("externalId")
            .contactEmail("new@negotiator.com")
            .name("newNetwork")
            .description("newNetwork")
            .uri("http://newuri.org")
            .build();
    String requestBody = TestUtils.jsonFromRequest(networkDTO);
    mockMvc
        .perform(
            MockMvcRequestBuilders.put(NETWORKS_URL + "/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/hal+json"))
        .andExpect(jsonPath("$.id", is(1)));

    Optional<Network> network = networkRepository.findById(1L);
    assert network.isPresent();
    assertEquals(networkDTO.getName(), network.get().getName());
  }

  @Test
  @Transactional
  @WithUserDetails("admin")
  public void postNetworkResources_validIds_returns204() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(NETWORKS_URL + "/1/resources")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[7,8]"))
        .andExpect(status().isNoContent());

    Optional<Network> network = networkRepository.findById(1L);
    assert network.isPresent();
    assertTrue(network.get().getResources().stream().anyMatch(resource -> resource.getId() == 7));
    assertTrue(network.get().getResources().stream().anyMatch(resource -> resource.getId() == 8));
  }

  @Test
  @Transactional
  @WithUserDetails("admin")
  public void postNetworkManagers_validIds_returns204() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(NETWORKS_URL + "/1/managers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[103,104]"))
        .andExpect(status().isNoContent());

    Optional<Network> network = networkRepository.findById(1L);
    assert network.isPresent();
    assertTrue(network.get().getManagers().stream().anyMatch(manager -> manager.getId() == 103));
    assertTrue(network.get().getManagers().stream().anyMatch(manager -> manager.getId() == 104));
  }

  @Test
  @Transactional
  @WithUserDetails("admin")
  public void deleteNetworkResource_validId_returns204() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.delete(NETWORKS_URL + "/1/resources/4"))
        .andExpect(status().isNoContent());

    Optional<Network> network = networkRepository.findById(1L);
    assert network.isPresent();
    assertTrue(network.get().getResources().stream().noneMatch(resource -> resource.getId() == 4));
  }

  @Test
  @Transactional
  @WithUserDetails("admin")
  public void deleteNetworkManager_validId_returns204() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.delete(NETWORKS_URL + "/1/managers/101"))
        .andExpect(status().isNoContent());

    Optional<Network> network = networkRepository.findById(1L);
    assert network.isPresent();
    assertTrue(network.get().getManagers().stream().noneMatch(manager -> manager.getId() == 101));
  }

  @Test
  @Transactional
  @WithUserDetails("admin")
  void getStatistics_validNetwork_ok() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(
                NETWORKS_URL + "/1/statistics?since=2024-01-01&until=2024-12-18"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.networkId", is(1)))
        .andExpect(jsonPath("$.totalNumberOfNegotiations", is(4)))
        .andExpect(jsonPath("$.numberOfIgnoredNegotiations", is(0)))
        .andExpect(jsonPath("$.statusDistribution.ABANDONED", is(1)));
  }

  @Test
  @WithUserDetails("researcher")
  void getOrganizations_notManager_403() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get(NETWORKS_URL + "/1/organizations"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithUserDetails("admin")
  void getOrganizations_validID_ok() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get(NETWORKS_URL + "/1/organizations"))
        .andExpect(status().isOk())
        .andDo(print())
        .andExpect(jsonPath("$._embedded.organizations.size()", is(2)));
  }
}
