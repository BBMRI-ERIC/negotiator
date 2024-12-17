package eu.bbmri_eric.negotiator.integration.api.v3;

import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import eu.bbmri_eric.negotiator.governance.organization.Organization;
import eu.bbmri_eric.negotiator.governance.organization.OrganizationCreateDTO;
import eu.bbmri_eric.negotiator.governance.organization.OrganizationRepository;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@IntegrationTest(loadTestData = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class OrganizationControllerTest {

  private static final String ORGANIZATIONS_ENDPOINT = "/v3/organizations";

  @Autowired private OrganizationRepository organizationRepository;

  private MockMvc mockMvc;
  @Autowired private WebApplicationContext context;

  @BeforeEach
  public void before() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @Test
  @WithUserDetails("admin")
  void postOrganizations() throws Exception {
    OrganizationCreateDTO organizationDTO1 =
        OrganizationCreateDTO.builder()
            .externalId("test_organization_1")
            .name("Test Organization 1")
            .description("Test Organization 1")
            .contactEmail("testorg1@test.org")
            .uri("http://test1.org")
            .build();
    OrganizationCreateDTO organizationDTO2 =
        OrganizationCreateDTO.builder()
            .externalId("test_organization_2")
            .name("Test Organization 2")
            .description("Test Organization 2")
            .contactEmail("testorg2@test.org")
            .uri("http://test2.org")
            .build();
    String requestBody =
        TestUtils.jsonFromRequest(Arrays.asList(organizationDTO1, organizationDTO2));
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post(ORGANIZATIONS_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(content().contentType("application/hal+json"))
            .andExpect(
                jsonPath("$._embedded.organizations[0].name", is(organizationDTO1.getName())))
            .andExpect(
                jsonPath(
                    "$._embedded.organizations[0].externalId",
                    is(organizationDTO1.getExternalId())))
            .andExpect(
                jsonPath(
                    "$._embedded.organizations[0].description",
                    is(organizationDTO1.getDescription())))
            .andExpect(
                jsonPath(
                    "$._embedded.organizations[0].contactEmail",
                    is(organizationDTO1.getContactEmail())))
            .andExpect(jsonPath("$._embedded.organizations[0].uri", is(organizationDTO1.getUri())))
            .andExpect(content().contentType("application/hal+json"))
            .andExpect(
                jsonPath("$._embedded.organizations[1].name", is(organizationDTO2.getName())))
            .andExpect(
                jsonPath(
                    "$._embedded.organizations[1].externalId",
                    is(organizationDTO2.getExternalId())))
            .andExpect(
                jsonPath(
                    "$._embedded.organizations[1].description",
                    is(organizationDTO2.getDescription())))
            .andExpect(
                jsonPath(
                    "$._embedded.organizations[1].contactEmail",
                    is(organizationDTO2.getContactEmail())))
            .andExpect(jsonPath("$._embedded.organizations[1].uri", is(organizationDTO2.getUri())))
            .andReturn();
    long id1 =
        JsonPath.parse(result.getResponse().getContentAsString())
            .read("$._embedded.organizations[0].id", Long.class);
    Optional<Organization> organization1 = organizationRepository.findById(id1);
    assert organization1.isPresent();
    assertEquals(organizationDTO1.getName(), organization1.get().getName());
    long id2 =
        JsonPath.parse(result.getResponse().getContentAsString())
            .read("$._embedded.organizations[1].id", Long.class);
    Optional<Organization> organization2 = organizationRepository.findById(id2);
    assert organization2.isPresent();
    assertEquals(organizationDTO2.getName(), organization2.get().getName());
  }

  @Test
  @WithUserDetails("admin")
  void updateOrganization() throws Exception {
    OrganizationCreateDTO organizationDTO3 =
        OrganizationCreateDTO.builder()
            .externalId("test_organization_3")
            .name("Test Organization 3")
            .description("Test Organization 3")
            .contactEmail("testorg3@test.org")
            .uri("http://testorg3.org")
            .build();
    OrganizationCreateDTO organizationDTO4 =
        OrganizationCreateDTO.builder()
            .externalId("test_organization_4")
            .name("Test Organization 4")
            .description("Test Organization 4")
            .contactEmail("testorg4@test.org")
            .uri("http://testorg4.org")
            .build();
    String requestBody =
        TestUtils.jsonFromRequest(Arrays.asList(organizationDTO3, organizationDTO4));
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post(ORGANIZATIONS_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(content().contentType("application/hal+json"))
            .andExpect(
                jsonPath("$._embedded.organizations[0].name", is(organizationDTO3.getName())))
            .andExpect(
                jsonPath(
                    "$._embedded.organizations[0].externalId",
                    is(organizationDTO3.getExternalId())))
            .andExpect(
                jsonPath(
                    "$._embedded.organizations[0].description",
                    is(organizationDTO3.getDescription())))
            .andExpect(
                jsonPath(
                    "$._embedded.organizations[0].contactEmail",
                    is(organizationDTO3.getContactEmail())))
            .andExpect(jsonPath("$._embedded.organizations[0].uri", is(organizationDTO3.getUri())))
            .andExpect(content().contentType("application/hal+json"))
            .andExpect(
                jsonPath("$._embedded.organizations[1].name", is(organizationDTO4.getName())))
            .andExpect(
                jsonPath(
                    "$._embedded.organizations[1].externalId",
                    is(organizationDTO4.getExternalId())))
            .andExpect(
                jsonPath(
                    "$._embedded.organizations[1].description",
                    is(organizationDTO4.getDescription())))
            .andExpect(
                jsonPath(
                    "$._embedded.organizations[1].contactEmail",
                    is(organizationDTO4.getContactEmail())))
            .andExpect(jsonPath("$._embedded.organizations[1].uri", is(organizationDTO4.getUri())))
            .andReturn();
    long id1 =
        JsonPath.parse(result.getResponse().getContentAsString())
            .read("$._embedded.organizations[0].id", Long.class);
    Optional<Organization> organization3 = organizationRepository.findById(id1);
    assert organization3.isPresent();
    assertEquals(organizationDTO3.getName(), organization3.get().getName());
    long id2 =
        JsonPath.parse(result.getResponse().getContentAsString())
            .read("$._embedded.organizations[1].id", Long.class);
    Optional<Organization> organization4 = organizationRepository.findById(id2);
    assert organization4.isPresent();
    assertEquals(organizationDTO4.getName(), organization4.get().getName());

    OrganizationCreateDTO updatedOrganizationDTO3 =
        OrganizationCreateDTO.builder()
            .externalId("test_organization_3")
            .name("Updated Test Organization 3")
            .description("Updated Test Organization 3")
            .contactEmail("updtestorg3@test.org")
            .uri("http://updtestorg3.org")
            .build();
    String updatedRequestBody = TestUtils.jsonFromRequest(updatedOrganizationDTO3);
    MvcResult updatedResult =
        mockMvc
            .perform(
                MockMvcRequestBuilders.put(
                        ORGANIZATIONS_ENDPOINT + "/" + organization3.get().getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(updatedRequestBody))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/hal+json"))
            .andExpect(jsonPath("$.name", is(updatedOrganizationDTO3.getName())))
            .andExpect(jsonPath("$.externalId", is(updatedOrganizationDTO3.getExternalId())))
            .andExpect(jsonPath("$.description", is(updatedOrganizationDTO3.getDescription())))
            .andExpect(jsonPath("$.contactEmail", is(updatedOrganizationDTO3.getContactEmail())))
            .andExpect(jsonPath("$.uri", is(updatedOrganizationDTO3.getUri())))
            .andReturn();
    Optional<Organization> updatedOrganization3 = organizationRepository.findById(id1);
    assertEquals(updatedOrganizationDTO3.getName(), updatedOrganization3.get().getName());
  }

  @Test
  @WithUserDetails("admin")
  void addOrganization_isWithdrawn() throws Exception {
    OrganizationCreateDTO organizationDTO =
        OrganizationCreateDTO.builder()
            .externalId("test_organization_1")
            .name("Test Organization 1")
            .description("Test Organization 1")
            .contactEmail("testorg1@test.org")
            .uri("http://testorg1.org")
            .withdrawn(true)
            .build();

    String requestBody = TestUtils.jsonFromRequest(Arrays.asList(organizationDTO));
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post(ORGANIZATIONS_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(content().contentType("application/hal+json"))
            .andExpect(jsonPath("$._embedded.organizations[0].name", is(organizationDTO.getName())))
            .andExpect(
                jsonPath(
                    "$._embedded.organizations[0].externalId", is(organizationDTO.getExternalId())))
            .andExpect(
                jsonPath(
                    "$._embedded.organizations[0].description",
                    is(organizationDTO.getDescription())))
            .andExpect(
                jsonPath(
                    "$._embedded.organizations[0].contactEmail",
                    is(organizationDTO.getContactEmail())))
            .andExpect(jsonPath("$._embedded.organizations[0].uri", is(organizationDTO.getUri())))
            .andExpect(content().contentType("application/hal+json"))
            .andReturn();
    long id1 =
        JsonPath.parse(result.getResponse().getContentAsString())
            .read("$._embedded.organizations[0].id", Long.class);
    Optional<Organization> organization = organizationRepository.findById(id1);
    assert organization.isPresent();
    assertEquals(organizationDTO.getName(), organization.get().getName());
    assertEquals(organizationDTO.getWithdrawn(), organization.get().getWithdrawn());
  }
}
