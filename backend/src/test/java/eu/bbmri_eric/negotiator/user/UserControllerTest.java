package eu.bbmri_eric.negotiator.user;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.bbmri_eric.negotiator.util.IntegrationTest;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@IntegrationTest(loadTestData = true)
public class UserControllerTest {
  @Autowired private PersonRepository personRepository;
  private static final String ROLES_ENDPOINT = "/v3/users/roles";
  private static final String LIST_USERS_ENDPOINT = "/v3/users";
  private static final String RESOURCES_FOR_USER_ENDPOINT = "/v3/users/%s/resources";
  private static final String NETWORKS_FOR_USER_ENDPOINT = "/v3/users/%s/networks";
  private static final String REPRESENTED_ORGANIZATIONS_FOR_USER_ENDPOINT =
      "/v3/users/%s/organizations";
  @Autowired private WebApplicationContext context;
  private MockMvc mockMvc;

  @BeforeEach
  public void before() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @Test
  @WithMockUser
  void getInfo_mockUserNoAuthorities_Ok() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.get(ROLES_ENDPOINT)).andExpect(status().isOk());
  }

  @Test
  @WithMockUser(authorities = "biobank:1:collection:1")
  void getInfo_mockUserOneAuthority_responseIsOk() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get(ROLES_ENDPOINT))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.[0]", is("biobank:1:collection:1")));
  }

  @Test
  @WithMockUser(authorities = {"biobank:1:collection:1", "ROLE_RESEARCHER"})
  void getInfo_mockUserMultipleAuthorities_responseIsOk() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get(ROLES_ENDPOINT))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$")
                .value(Matchers.containsInAnyOrder("biobank:1:collection:1", "ROLE_RESEARCHER")));
  }

  @Test
  @WithMockUser(roles = "AUTHORIZATION_MANAGER")
  void getRepresentedResources_oneResource_ok() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get(RESOURCES_FOR_USER_ENDPOINT.formatted(103)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.resources").isNotEmpty());
  }

  @Test
  @WithUserDetails("admin")
  void getRepresentedNetworks_oneNetwork_ok() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get(NETWORKS_FOR_USER_ENDPOINT.formatted(102)))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json"))
        .andExpect(jsonPath("$.page.totalElements", is(1)));
  }

  @Test
  @WithUserDetails("researcher")
  void getRepresentedNetworks_validRequest_Forbidden() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get(NETWORKS_FOR_USER_ENDPOINT.formatted(102)))
        .andExpect(status().isForbidden());
  }

  @Test
  void getUsers_notAuthorized_401() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get(LIST_USERS_ENDPOINT))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "AUTHORIZATION_MANAGER")
  void getUsers_validRequest_allAreReturned() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get(LIST_USERS_ENDPOINT))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.users").isArray())
        .andExpect(jsonPath("$._embedded.users").isNotEmpty())
        .andExpect(jsonPath("$._embedded.users.length()", is(personRepository.findAll().size())));
  }

  @Test
  @WithMockUser(roles = "AUTHORIZATION_MANAGER")
  void getUsers_validRequest_customPagination() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(LIST_USERS_ENDPOINT).param("page", "1").param("size", "3"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.users").isArray())
        .andExpect(jsonPath("$._embedded.users").isNotEmpty())
        .andExpect(jsonPath("$.page.totalElements", is(personRepository.findAll().size())))
        .andExpect(jsonPath("$.page.number", is(1)))
        .andExpect(jsonPath("$._embedded.users.length()", is(3)));
  }

  @Test
  @WithMockUser(roles = "AUTHORIZATION_MANAGER")
  void getUsers_validRequest_customSorting() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(LIST_USERS_ENDPOINT)
                .param("sortBy", "name")
                .param("sortOrder", "ASC"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.users").isArray())
        .andExpect(jsonPath("$._embedded.users").isNotEmpty())
        .andExpect(jsonPath("$._embedded.users.length()", is(personRepository.findAll().size())))
        .andExpect(jsonPath("$._embedded.users[0].name", is("admin")));

    mockMvc
        .perform(
            MockMvcRequestBuilders.get(LIST_USERS_ENDPOINT)
                .param("sortBy", "name")
                .param("sortOrder", "DESC"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.users").isArray())
        .andExpect(jsonPath("$._embedded.users").isNotEmpty())
        .andExpect(jsonPath("$._embedded.users.length()", is(personRepository.findAll().size())))
        .andExpect(jsonPath("$._embedded.users[0].name", is("TheResearcher")));
  }

  @Test
  @WithMockUser("TheResearcher")
  void getUsers_validRequest_Forbidden() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get(LIST_USERS_ENDPOINT))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "AUTHORIZATION_MANAGER")
  void getUsers_noFilters_authorized_ok() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.get(LIST_USERS_ENDPOINT)).andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "AUTHORIZATION_MANAGER")
  void findUsers_byName_authorized_ok() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get(LIST_USERS_ENDPOINT).param("name", "TheResearcher"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.users.length()", is(1)))
        .andExpect(jsonPath("$._embedded.users[0].id", is("108")))
        .andExpect(jsonPath("$._embedded.users[0].name", is("TheResearcher")))
        .andExpect(jsonPath("$._embedded.users[0].subjectId", is("1000@bbmri.eu")))
        .andExpect(jsonPath("$._embedded.users[0].email", is("adam.researcher@gmail.com")));
    mockMvc
        .perform(MockMvcRequestBuilders.get(LIST_USERS_ENDPOINT).param("name", "The"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.users.length()", is(2)))
        .andExpect(jsonPath("$._embedded.users[0].id", is("108")))
        .andExpect(jsonPath("$._embedded.users[0].name", is("TheResearcher")))
        .andExpect(jsonPath("$._embedded.users[0].subjectId", is("1000@bbmri.eu")))
        .andExpect(jsonPath("$._embedded.users[0].email", is("adam.researcher@gmail.com")))
        .andExpect(jsonPath("$._embedded.users[1].id", is("109")))
        .andExpect(jsonPath("$._embedded.users[1].name", is("TheBiobanker")))
        .andExpect(jsonPath("$._embedded.users[1].subjectId", is("1001@bbmri.eu")))
        .andExpect(jsonPath("$._embedded.users[1].email", is("taylor.biobanker@gmail.com")));
  }

  @Test
  @WithMockUser(roles = "AUTHORIZATION_MANAGER")
  void findUsers_byAccentName_authorized_ok() throws Exception {
    Person accentedPerson = new Person();
    accentedPerson.setName("José García");
    accentedPerson.setEmail("jose.garcia@test.com");
    accentedPerson.setSubjectId("jose@test");
    accentedPerson.setOrganization("Test Org");
    accentedPerson = personRepository.save(accentedPerson);
    try {
      mockMvc
          .perform(MockMvcRequestBuilders.get(LIST_USERS_ENDPOINT).param("name", "José"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$._embedded.users[?(@.name == 'José García')]").exists());
      mockMvc
          .perform(MockMvcRequestBuilders.get(LIST_USERS_ENDPOINT).param("name", "Jose"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$._embedded.users[?(@.name == 'José García')]").exists());
      mockMvc
          .perform(MockMvcRequestBuilders.get(LIST_USERS_ENDPOINT).param("name", "García"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$._embedded.users[?(@.name == 'José García')]").exists());
      mockMvc
          .perform(MockMvcRequestBuilders.get(LIST_USERS_ENDPOINT).param("name", "Garcia"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$._embedded.users[?(@.name == 'José García')]").exists());
    } finally {
      personRepository.delete(accentedPerson);
    }
  }

  @Test
  @WithMockUser(roles = "AUTHORIZATION_MANAGER")
  void findUsers_byAccentEmail_authorized_ok() throws Exception {
    Person accentedPerson = new Person();
    accentedPerson.setName("François Müller");
    accentedPerson.setEmail("françois.müller@test.com");
    accentedPerson.setSubjectId("francois@test");
    accentedPerson.setOrganization("Test Org");
    accentedPerson = personRepository.save(accentedPerson);
    try {
      mockMvc
          .perform(MockMvcRequestBuilders.get(LIST_USERS_ENDPOINT).param("email", "françois"))
          .andExpect(status().isOk())
          .andExpect(
              jsonPath("$._embedded.users[?(@.email == 'françois.müller@test.com')]").exists());
      mockMvc
          .perform(MockMvcRequestBuilders.get(LIST_USERS_ENDPOINT).param("email", "francois"))
          .andExpect(status().isOk())
          .andExpect(
              jsonPath("$._embedded.users[?(@.email == 'françois.müller@test.com')]").exists());
      mockMvc
          .perform(MockMvcRequestBuilders.get(LIST_USERS_ENDPOINT).param("email", "müller"))
          .andExpect(status().isOk())
          .andExpect(
              jsonPath("$._embedded.users[?(@.email == 'françois.müller@test.com')]").exists());
      mockMvc
          .perform(MockMvcRequestBuilders.get(LIST_USERS_ENDPOINT).param("email", "muller"))
          .andExpect(status().isOk())
          .andExpect(
              jsonPath("$._embedded.users[?(@.email == 'françois.müller@test.com')]").exists());
    } finally {
      personRepository.delete(accentedPerson);
    }
  }

  @Test
  @WithMockUser(roles = "AUTHORIZATION_MANAGER")
  void getRepresentedOrganizations_oneOrganization_ok() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(REPRESENTED_ORGANIZATIONS_FOR_USER_ENDPOINT.formatted(103)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.organizations").isArray())
        .andExpect(jsonPath("$._embedded.organizations.length()", is(1)));
  }

  @Test
  @WithMockUser(roles = "AUTHORIZATION_MANAGER")
  void getRepresentedOrganizations_filterByName_okEmptyWhenNoMatch() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(REPRESENTED_ORGANIZATIONS_FOR_USER_ENDPOINT.formatted(103))
                .param("name", "NoSuchOrgNameShouldNotMatch"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.organizations").doesNotExist());
  }

  @Test
  @WithUserDetails("TheBiobanker")
  void getRepresentedOrganizations_withExpandResources_ok() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(REPRESENTED_ORGANIZATIONS_FOR_USER_ENDPOINT.formatted(109))
                .param("expand", "resources"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json"))
        .andExpect(jsonPath("$._embedded.organizations").isArray())
        .andExpect(jsonPath("$._embedded.organizations.length()", not(0)))
        .andExpect(jsonPath("$._embedded.organizations[0].resources").isArray());
  }

  @Test
  @WithUserDetails("TheResearcher")
  void getRepresentedOrganizations_researcherAccessingBiobankers_forbidden() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(REPRESENTED_ORGANIZATIONS_FOR_USER_ENDPOINT.formatted(109)))
        .andExpect(status().isForbidden());
  }

  @Test
  void getRepresentedOrganizations_accessingBiobankers_unauthorized() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(REPRESENTED_ORGANIZATIONS_FOR_USER_ENDPOINT.formatted(109)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser
  void loginEvent_triggersLastLoginUpdate() {
    Person person =
        personRepository.findAll().stream()
            .filter(p -> p.getSubjectId() != null)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No person with subjectId found"));

    String subjectId = person.getSubjectId();
    ApplicationEventPublisher publisher = context;

    Map<String, Object> claims = new HashMap<>();
    claims.put(StandardClaimNames.SUB, subjectId);
    claims.put(StandardClaimNames.EMAIL, person.getEmail());
    claims.put(StandardClaimNames.NAME, person.getName());

    OidcIdToken idToken =
        new OidcIdToken("tokenValue", Instant.now(), Instant.now().plusSeconds(3600), claims);
    OidcUserAuthority authority = new OidcUserAuthority(idToken);

    JwtAuthenticationToken authentication =
        new JwtAuthenticationToken(
            Jwt.withTokenValue("tokenValue")
                .header("alg", "none")
                .claim(StandardClaimNames.SUB, subjectId)
                .claim(StandardClaimNames.EMAIL, person.getEmail())
                .claim(StandardClaimNames.NAME, person.getName())
                .build(),
            List.of(authority),
            subjectId);

    var event =
        new org.springframework.security.authentication.event.AuthenticationSuccessEvent(
            authentication);

    publisher.publishEvent(event);

    await()
        .atMost(Duration.ofSeconds(2))
        .untilAsserted(
            () -> {
              Person updated = personRepository.findById(person.getId()).orElseThrow();
              assertNotNull(updated.getLastLogin(), "lastLogin should have been updated");
              assertTrue(
                  updated.getLastLogin().isAfter(LocalDateTime.now().minusMinutes(2)),
                  "lastLogin should be recent");
            });
  }
}
