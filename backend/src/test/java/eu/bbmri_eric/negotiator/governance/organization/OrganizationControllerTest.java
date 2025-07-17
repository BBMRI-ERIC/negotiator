package eu.bbmri_eric.negotiator.governance.organization;

import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import eu.bbmri_eric.negotiator.integration.api.v3.TestUtils;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@IntegrationTest(loadTestData = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@AutoConfigureMockMvc
public class OrganizationControllerTest {

  private static final String ORGANIZATIONS_ENDPOINT = "/v3/organizations";

  @Autowired private OrganizationRepository organizationRepository;
  @Autowired
  private MockMvc mockMvc;

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
  void updateOrganization_validInput_ok() throws Exception {
    Optional<Organization> firstOrg = organizationRepository.findAll().stream().findFirst();
    Long orgId = firstOrg.get().getId();

    OrganizationUpdateDTO updateDTO =
            OrganizationUpdateDTO.builder()
            .name("Updated Organization Name")
                    .externalId("idk")
            .description("Updated organization description")
            .contactEmail("updated@test.org")
            .uri("https://updated-organization.org")
            .withdrawn(true)
            .build();

    String requestBody = TestUtils.jsonFromRequest(updateDTO);

    mockMvc
        .perform(
            MockMvcRequestBuilders.patch(ORGANIZATIONS_ENDPOINT + "/" + orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/hal+json"))
        .andExpect(jsonPath("$.name", is(updateDTO.getName())))
        .andExpect(jsonPath("$.externalId", is(updateDTO.getExternalId())))
        .andExpect(jsonPath("$.description", is(updateDTO.getDescription())))
        .andExpect(jsonPath("$.contactEmail", is(updateDTO.getContactEmail())))
        .andExpect(jsonPath("$.uri", is(updateDTO.getUri())))
        .andExpect(jsonPath("$.withdrawn", is(updateDTO.getWithdrawn())));

    Optional<Organization> updatedOrg = organizationRepository.findById(orgId);
    assert updatedOrg.isPresent();
    assertEquals(updateDTO.getName(), updatedOrg.get().getName());
    assertEquals(updateDTO.getExternalId(), updatedOrg.get().getExternalId());
    assertEquals(updateDTO.getDescription(), updatedOrg.get().getDescription());
    assertEquals(updateDTO.getContactEmail(), updatedOrg.get().getContactEmail());
    assertEquals(updateDTO.getUri(), updatedOrg.get().getUri());
    assertEquals(updateDTO.getWithdrawn(), updatedOrg.get().isWithdrawn());
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
    assertEquals(organizationDTO.getWithdrawn(), organization.get().isWithdrawn());
  }

  @Test
  @WithUserDetails("admin")
  void updateOrganization_partialUpdate_ok() throws Exception {
    Optional<Organization> firstOrg = organizationRepository.findAll().stream().findFirst();
    Long orgId = firstOrg.get().getId();

    String originalExternalId = firstOrg.get().getExternalId();
    String originalDescription = firstOrg.get().getDescription();
    String originalContactEmail = firstOrg.get().getContactEmail();

    OrganizationUpdateDTO partialUpdateDTO =
            OrganizationUpdateDTO.builder()
            .name("Partially Updated Organization")
            .uri("https://partially-updated.org")
            .build();

    String requestBody = TestUtils.jsonFromRequest(partialUpdateDTO);

    mockMvc
        .perform(
            MockMvcRequestBuilders.patch(ORGANIZATIONS_ENDPOINT + "/" + orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/hal+json"))
        .andExpect(jsonPath("$.name", is(partialUpdateDTO.getName())))
        .andExpect(jsonPath("$.uri", is(partialUpdateDTO.getUri())));

    Optional<Organization> updatedOrg = organizationRepository.findById(orgId);
    assertEquals(partialUpdateDTO.getName(), updatedOrg.get().getName());
    assertEquals(partialUpdateDTO.getUri(), updatedOrg.get().getUri());
    assertEquals(originalExternalId, updatedOrg.get().getExternalId());
    assertEquals(originalDescription, updatedOrg.get().getDescription());
    assertEquals(originalContactEmail, updatedOrg.get().getContactEmail());
  }
}
