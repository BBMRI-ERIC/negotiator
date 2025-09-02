package eu.bbmri_eric.negotiator.integration.api.v3;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import eu.bbmri_eric.negotiator.user.AssignResourceDTO;
import eu.bbmri_eric.negotiator.user.Person;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import eu.bbmri_eric.negotiator.util.WithMockNegotiatorUser;
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
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.annotation.DirtiesContext;
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
  void findUsers_byEmail_authorized_ok() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(LIST_USERS_ENDPOINT)
                .param("email", "adam.researcher@gmail.com"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.users.length()", is(1)))
        .andExpect(jsonPath("$._embedded.users[0].id", is("108")))
        .andExpect(jsonPath("$._embedded.users[0].name", is("TheResearcher")))
        .andExpect(jsonPath("$._embedded.users[0].subjectId", is("1000@bbmri.eu")))
        .andExpect(jsonPath("$._embedded.users[0].email", is("adam.researcher@gmail.com")));
  }

  @Test
  @WithMockUser(roles = "AUTHORIZATION_MANAGER")
  void findUsers_bySubjectId_authorized_ok() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(LIST_USERS_ENDPOINT).param("subjectId", "1000@bbmri.eu"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.users.length()", is(1)))
        .andExpect(jsonPath("$._embedded.users[0].id", is("108")))
        .andExpect(jsonPath("$._embedded.users[0].name", is("TheResearcher")))
        .andExpect(jsonPath("$._embedded.users[0].subjectId", is("1000@bbmri.eu")))
        .andExpect(jsonPath("$._embedded.users[0].email", is("adam.researcher@gmail.com")));
  }

  @Test
  @WithMockUser(roles = "AUTHORIZATION_MANAGER")
  void findUsers_byIsAdmin_authorized_ok() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get(LIST_USERS_ENDPOINT).param("isAdmin", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.users.length()", is(1)))
        .andExpect(jsonPath("$._embedded.users[0].id", is("101")))
        .andExpect(jsonPath("$._embedded.users[0].name", is("admin")))
        .andExpect(jsonPath("$._embedded.users[0].subjectId", is("1")))
        .andExpect(jsonPath("$._embedded.users[0].email", is("admin@negotiator.dev")));
  }

  @Test
  @WithMockUser(roles = "AUTHORIZATION_MANAGER")
  void findUsers_byLastLogin_authorized_ok() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(LIST_USERS_ENDPOINT)
                .param("lastLoginAfter", "2025-07-29")
                .param("sortBy", "lastLogin")
                .param("sortOrder", "ASC"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath(
                "$._embedded.users.length()",
                not(0))) // at least the one in the initial data, plus the ones logged in during the
        // test
        .andExpect(jsonPath("$._embedded.users[0].id", is("101")))
        .andExpect(jsonPath("$._embedded.users[0].name", is("admin")))
        .andExpect(jsonPath("$._embedded.users[0].subjectId", is("1")))
        .andExpect(jsonPath("$._embedded.users[0].email", is("admin@negotiator.dev")));

    mockMvc
        .perform(
            MockMvcRequestBuilders.get(LIST_USERS_ENDPOINT).param("lastLoginBefore", "2025-01-01"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.users.length()", is(1)))
        .andExpect(jsonPath("$._embedded.users[0].id", is("104")))
        .andExpect(jsonPath("$._embedded.users[0].name", is("researcher")))
        .andExpect(jsonPath("$._embedded.users[0].subjectId", is("4")))
        .andExpect(jsonPath("$._embedded.users[0].email", is("researcher@negotiator.dev")));

    mockMvc
        .perform(
            MockMvcRequestBuilders.get(LIST_USERS_ENDPOINT)
                .param("lastLoginBefore", "2025-04-01")
                .param("lastLoginAfter", "2025-02-09")
                .param("sortBy", "lastLogin")
                .param("sortOrder", "ASC"))
        .andExpect(jsonPath("$._embedded.users.length()", is(2)))
        .andExpect(jsonPath("$._embedded.users[0].id", is("109")))
        .andExpect(jsonPath("$._embedded.users[0].name", is("TheBiobanker")))
        .andExpect(jsonPath("$._embedded.users[0].subjectId", is("1001@bbmri.eu")))
        .andExpect(jsonPath("$._embedded.users[0].email", is("taylor.biobanker@gmail.com")));
  }

  @Test
  @WithMockUser(roles = "AUTHORIZATION_MANAGER")
  void findUsers_authorized_empty() throws Exception {
    List<String> params = List.of("name", "email", "subjectId");
    for (String param : params) {
      mockMvc
          .perform(MockMvcRequestBuilders.get(LIST_USERS_ENDPOINT).param(param, "novalue"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.page.totalElements", is(0)));
    }
  }

  @Test
  @WithMockUser(roles = "AUTHORIZATION_MANAGER")
  void findUsers_isAdmin_BadRequest() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get(LIST_USERS_ENDPOINT).param("isAdmin", "noboolean"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "AUTHORIZATION_MANAGER")
  void findUsers_byMultipleFilter_authorized_ok() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(LIST_USERS_ENDPOINT)
                .param("name", "The")
                .param("email", "adam.researcher@gmail.com")
                .param("subjectId", "1000@bbmri.eu")
                .param("isAdmin", "false"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.users.length()", is(1)))
        .andExpect(jsonPath("$._embedded.users[0].id", is("108")))
        .andExpect(jsonPath("$._embedded.users[0].name", is("TheResearcher")))
        .andExpect(jsonPath("$._embedded.users[0].subjectId", is("1000@bbmri.eu")))
        .andExpect(jsonPath("$._embedded.users[0].email", is("adam.researcher@gmail.com")));
  }

  @Test
  @WithMockUser("TheResearcher")
  void findUsers_Forbidden() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get(LIST_USERS_ENDPOINT).param("name", "value"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockNegotiatorUser(id = 109, authorities = "ROLE_RESOURCE_MANAGER")
  void findUsers_filtersIgnored_ok() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get(LIST_USERS_ENDPOINT).param("name", "NotMatching"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.users[0].id", is("109")))
        .andExpect(jsonPath("$._embedded.users[0].name", is("TheBiobanker")))
        .andExpect(jsonPath("$._embedded.users[0].subjectId", is("1001@bbmri.eu")))
        .andExpect(jsonPath("$._embedded.users[0].email", is("taylor.biobanker@gmail.com")));
  }

  @Test
  @WithMockUser(roles = "AUTHORIZATION_MANAGER")
  void getUserById_idNotLong_400() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get(LIST_USERS_ENDPOINT + "/null"))
        .andExpect(status().isBadRequest());
    mockMvc
        .perform(MockMvcRequestBuilders.get(LIST_USERS_ENDPOINT + "/fake4rd"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "AUTHORIZATION_MANAGER")
  void getUserById_validId_200() throws Exception {
    Person person = personRepository.findAll().iterator().next();
    mockMvc
        .perform(MockMvcRequestBuilders.get(LIST_USERS_ENDPOINT + "/" + person.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(person.getId().toString())))
        .andExpect(jsonPath("$.name", is(person.getName())))
        .andExpect(jsonPath("$.email", is(person.getEmail())));
  }

  @Test
  @WithMockUser(roles = "AUTHORIZATION_MANAGER")
  void assignResource_validResource_ok() throws Exception {
    Person person = personRepository.findAll().iterator().next();
    ObjectMapper mapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
    mockMvc
        .perform(MockMvcRequestBuilders.get(RESOURCES_FOR_USER_ENDPOINT.formatted(person.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isEmpty());
    mockMvc
        .perform(
            MockMvcRequestBuilders.patch(RESOURCES_FOR_USER_ENDPOINT.formatted(person.getId()))
                .content(mapper.writeValueAsString(new AssignResourceDTO(4L)))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent());
    mockMvc
        .perform(MockMvcRequestBuilders.get(RESOURCES_FOR_USER_ENDPOINT.formatted(person.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.resources").isNotEmpty());
  }

  @Test
  @WithMockUser(roles = "AUTHORIZATION_MANAGER")
  @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
  void removeResourceFromRepresentative_validResource_ok() throws Exception {
    Person person = personRepository.findAll().iterator().next();
    ObjectMapper mapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
    mockMvc
        .perform(MockMvcRequestBuilders.get(RESOURCES_FOR_USER_ENDPOINT.formatted(person.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isEmpty());
    mockMvc
        .perform(
            MockMvcRequestBuilders.patch(RESOURCES_FOR_USER_ENDPOINT.formatted(person.getId()))
                .content(mapper.writeValueAsString(new AssignResourceDTO(4L)))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent());
    mockMvc
        .perform(MockMvcRequestBuilders.get(RESOURCES_FOR_USER_ENDPOINT.formatted(person.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.resources").isNotEmpty());
    mockMvc
        .perform(
            MockMvcRequestBuilders.delete(
                RESOURCES_FOR_USER_ENDPOINT.formatted(person.getId()) + "/4"))
        .andExpect(status().isNoContent());
  }

  @Test
  @WithUserDetails("admin")
  void userInfo_networkManager_linkForNetworks() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("/v3/userinfo"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._links.networks").isNotEmpty());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void assignNetwork_validResource_ok() throws Exception {
    Person person = personRepository.findAll().get(3);
    ObjectMapper mapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
    mockMvc
        .perform(MockMvcRequestBuilders.get(NETWORKS_FOR_USER_ENDPOINT.formatted(person.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page.totalElements").value(0));
    mockMvc
        .perform(
            MockMvcRequestBuilders.patch(NETWORKS_FOR_USER_ENDPOINT.formatted(person.getId()))
                .content(mapper.writeValueAsString(new AssignResourceDTO(1L)))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent());
    mockMvc
        .perform(MockMvcRequestBuilders.get(NETWORKS_FOR_USER_ENDPOINT.formatted(person.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.networks").isNotEmpty());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
  void removeNetworkFromManager_validResource_ok() throws Exception {
    Person person = personRepository.findAll().get(1);
    ObjectMapper mapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
    mockMvc
        .perform(MockMvcRequestBuilders.get(NETWORKS_FOR_USER_ENDPOINT.formatted(person.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.networks").isNotEmpty());
    mockMvc
        .perform(
            MockMvcRequestBuilders.delete(
                NETWORKS_FOR_USER_ENDPOINT.formatted(person.getId()) + "/1"))
        .andExpect(status().isNoContent());
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
