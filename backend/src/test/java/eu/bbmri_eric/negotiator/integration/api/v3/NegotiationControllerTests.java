package eu.bbmri_eric.negotiator.integration.api.v3;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import eu.bbmri_eric.negotiator.common.AuthenticatedUserContext;
import eu.bbmri_eric.negotiator.discovery.DiscoveryService;
import eu.bbmri_eric.negotiator.discovery.DiscoveryServiceRepository;
import eu.bbmri_eric.negotiator.governance.organization.Organization;
import eu.bbmri_eric.negotiator.governance.organization.OrganizationRepository;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.governance.resource.ResourceRepository;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.dto.NegotiationCreateDTO;
import eu.bbmri_eric.negotiator.negotiation.dto.NegotiationUpdateDTO;
import eu.bbmri_eric.negotiator.negotiation.dto.UpdateResourcesDTO;
import eu.bbmri_eric.negotiator.negotiation.request.RequestRepository;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceState;
import eu.bbmri_eric.negotiator.post.Post;
import eu.bbmri_eric.negotiator.post.PostRepository;
import eu.bbmri_eric.negotiator.user.Person;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import eu.bbmri_eric.negotiator.util.WithMockNegotiatorUser;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opentest4j.TestAbortedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
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
public class NegotiationControllerTests {

  // Request alrady present in data-h2. It is already assigned to a request
  private static final String REQUEST_1_ID = "request-1";
  private static final String REQUEST_2_ID = "request-2";
  private static final String REQUEST_UNASSIGNED = "request-unassigned";
  private static final String NEGOTIATION_1_ID = "negotiation-1";
  private static final String NEGOTIATION_2_ID = "negotiation-2";
  private static final String NEGOTIATION_V2_ID = "negotiation-v2";
  private static final String NEGOTIATION_3_ID = "negotiation-3";
  private static final String NEGOTIATION_4_ID = "negotiation-4";
  private static final String NEGOTIATION_5_ID = "negotiation-5";
  private static final String NEGOTIATION_1_CREATION_DATE = "2024-10-12T00:00:00";
  private static final String NEGOTIATIONS_URL = "/v3/negotiations";
  private static final String SELF_LINK_TPL = "http://localhost/v3/negotiations/%s";
  private static final String POSTS_LINK_TPL = "http://localhost/v3/negotiations/%s/posts";
  private static final String ATTACHMENTS_LINK_TPL =
      "http://localhost/v3/negotiations/%s/attachments";

  @Autowired private WebApplicationContext context;
  @Autowired private NegotiationRepository negotiationRepository;
  @Autowired PersonRepository personRepository;
  @Autowired EntityManager entityManager; // for flushing the entity manager

  @Autowired ResourceRepository resourceRepository;

  @Autowired RequestRepository requestRepository;

  @Autowired DiscoveryServiceRepository discoveryServiceRepository;

  @Autowired OrganizationRepository organizationRepository;

  @Autowired PostRepository postRepository;

  private MockMvc mockMvc;

  @BeforeEach
  public void before() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @Test
  public void testGetAll_Unauthorized_whenNoAuth() throws Exception {
    TestUtils.checkErrorResponse(
        mockMvc, HttpMethod.GET, "", status().isUnauthorized(), anonymous(), NEGOTIATIONS_URL);
  }

  @Test
  public void testGetAll_Unauthorized_whenBasicAuth() throws Exception {
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.GET,
        "",
        status().isUnauthorized(),
        httpBasic("researcher", "wrong_pass"),
        NEGOTIATIONS_URL);
  }

  @Test
  public void testGetAll_Unauthorized_whenInvalidToken() throws Exception {
    TestUtils.checkErrorResponse(
        mockMvc, HttpMethod.GET, "", status().isUnauthorized(), "", NEGOTIATIONS_URL);
  }

  /**
   * It tests that, getting all negotiations without filters for a user that doesn't represent any
   * resource, it returns all the negotiations create by the user.
   */
  @Test
  @WithUserDetails("admin")
  @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
  public void testGetAllForAdministrator_whenNoFilters() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("/v3/negotiations"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/hal+json"))
        .andExpect(jsonPath("$.page.totalElements", is(6)))
        .andExpect(jsonPath("$._embedded.negotiations.length()", is(6)))
        .andExpect(jsonPath("$._embedded.negotiations.[0].id", is(NEGOTIATION_1_ID)))
        .andExpect(jsonPath("$._embedded.negotiations.[1].id", is(NEGOTIATION_2_ID)))
        .andExpect(jsonPath("$._embedded.negotiations.[2].id", is(NEGOTIATION_5_ID)))
        .andExpect(jsonPath("$._embedded.negotiations.[3].id", is(NEGOTIATION_3_ID)))
        .andExpect(jsonPath("$._embedded.negotiations.[4].id", is(NEGOTIATION_4_ID)))
        .andExpect(jsonPath("$._embedded.negotiations.[5].id", is(NEGOTIATION_V2_ID)));
  }

  /** It tests that using an unsupported sort column it returns 400 Bad Request */
  @Test
  @WithUserDetails("admin")
  public void testGetAllForAdministrator_whenUnknownSortBy() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("/v3/negotiations?sortBy=UNK"))
        .andExpect(status().isBadRequest());
  }

  /** It tests that using an unsupported sort column it returns 400 Bad Request */
  @Test
  @WithUserDetails("admin")
  public void testGetAllForAdministrator_whenUnknownSortOrder() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("/v3/negotiations?sortOrder=UNK"))
        .andExpect(status().isBadRequest());
  }

  /** It tests that using an unknown param it returns 400 Bad Request */
  @Test
  @WithUserDetails("admin")
  @Disabled
  public void testGetAllForAdministrator_whenUnknownParameter() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("/v3/negotiations?unkParam=something"))
        .andExpect(status().isBadRequest());
  }

  /**
   * It tests that, getting all negotiations without filters for a user that doesn't represent any
   * resource, it returns all the negotiations create by the user.
   */
  @Test
  @WithUserDetails("admin")
  public void testGetAllForAdministrator_whenNoFilters_withCustomPagination_firstPage()
      throws Exception {
    int pageSize = 2;
    String endpoint = "/v3/negotiations";
    String firstLink =
        "http://localhost%s?sortBy=creationDate&sortOrder=DESC&page=0&size=%s"
            .formatted(endpoint, pageSize);
    String lastLink =
        "http://localhost%s?sortBy=creationDate&sortOrder=DESC&page=2&size=%s"
            .formatted(endpoint, pageSize);

    mockMvc
        .perform(MockMvcRequestBuilders.get("%s?size=%s".formatted(endpoint, pageSize)))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/hal+json"))
        .andExpect(jsonPath("$.page.totalElements", is(6)))
        .andExpect(jsonPath("$.page.totalPages", is(3)))
        .andExpect(jsonPath("$._embedded.negotiations.length()", is(2)))
        .andExpect(jsonPath("$._embedded.negotiations.[0].id", is(NEGOTIATION_1_ID)))
        .andExpect(jsonPath("$._embedded.negotiations.[1].id", is(NEGOTIATION_2_ID)))
        .andExpect(jsonPath("$._links.current.href", is(firstLink)))
        .andExpect(jsonPath("$._links.last.href", is(lastLink)));
  }

  /**
   * It tests that, getting all negotiations without filters for a user that doesn't represent any
   * resource, it returns all the negotiations create by the user.
   */
  @Test
  @WithUserDetails("admin")
  public void testGetAllForAdministrator_whenNoFilters_withCustomPagination_secondPage()
      throws Exception {
    int pageSize = 2;
    String endpoint = "/v3/negotiations";
    String firstLink =
        "http://localhost%s?sortBy=creationDate&sortOrder=DESC&page=0&size=%s"
            .formatted(endpoint, pageSize);
    String currentLink =
        "http://localhost%s?sortBy=creationDate&sortOrder=DESC&page=1&size=%s"
            .formatted(endpoint, pageSize);
    String lastLink =
        "http://localhost%s?sortBy=creationDate&sortOrder=DESC&page=2&size=%s"
            .formatted(endpoint, pageSize);

    mockMvc
        .perform(MockMvcRequestBuilders.get("%s?page=1&size=%s".formatted(endpoint, pageSize)))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/hal+json"))
        .andExpect(jsonPath("$.page.totalElements", is(6)))
        .andExpect(jsonPath("$.page.totalPages", is(3)))
        .andExpect(jsonPath("$._embedded.negotiations.length()", is(2)))
        .andExpect(jsonPath("$._embedded.negotiations.[0].id", is(NEGOTIATION_5_ID)))
        .andExpect(jsonPath("$._embedded.negotiations.[1].id", is(NEGOTIATION_3_ID)))
        .andExpect(jsonPath("$._links.first.href", is(firstLink)))
        .andExpect(jsonPath("$._links.current.href", is(currentLink)))
        .andExpect(jsonPath("$._links.last.href", is(lastLink)));
  }

  /**
   * It tests that, getting all negotiations without filters for a user that doesn't represent any
   * resource, it returns all the negotiations create by the user.
   */
  @Test
  @WithUserDetails("admin")
  public void testGetAllForAdministrator_whenFilteredByState_andSortedASC() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("/v3/negotiations?status=ABANDONED&sortOrder=ASC"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/hal+json"))
        .andExpect(jsonPath("$.page.totalElements", is(2)))
        .andExpect(jsonPath("$._embedded.negotiations.length()", is(2)))
        .andExpect(jsonPath("$._embedded.negotiations.[0].id", is(NEGOTIATION_V2_ID)))
        .andExpect(jsonPath("$._embedded.negotiations.[1].id", is(NEGOTIATION_4_ID)));
  }

  /**
   * It tests that, getting all negotiations without filters for a user that doesn't represent any
   * resource, it returns all the negotiations create by the user.
   */
  @Test
  @WithUserDetails("admin")
  public void testGetAllForAdministrator_whenFilteredByState() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("/v3/negotiations?status=ABANDONED"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/hal+json"))
        .andExpect(jsonPath("$.page.totalElements", is(2)))
        .andExpect(jsonPath("$._embedded.negotiations.length()", is(2)))
        .andExpect(jsonPath("$._embedded.negotiations.[0].id", is(NEGOTIATION_4_ID)))
        .andExpect(jsonPath("$._embedded.negotiations.[1].id", is(NEGOTIATION_V2_ID)));
  }

  /**
   * It tests that, getting all negotiations without filters for a user that doesn't represent any
   * resource, it returns all the negotiations create by the user.
   */
  @Test
  @WithUserDetails("admin")
  public void testGetAllForAdministrator_whenFilteredByStateAndCreationDate() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(
                "/v3/negotiations?status=ABANDONED,IN_PROGRESS&createdAfter=2024-01-09&createdBefore=2024-09-01"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/hal+json"))
        .andExpect(jsonPath("$.page.totalElements", is(2)))
        .andExpect(jsonPath("$._embedded.negotiations.length()", is(2)))
        .andExpect(jsonPath("$._embedded.negotiations.[0].id", is(NEGOTIATION_3_ID)))
        .andExpect(jsonPath("$._embedded.negotiations.[1].id", is(NEGOTIATION_4_ID)));
  }

  /**
   * It tests that, getting all negotiations without filters for a user that doesn't represent any
   * resource, it returns all the negotiations create by the user.
   */
  @Test
  @WithUserDetails("admin")
  public void testGetAllForAdministrator_SortedByState() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("/v3/negotiations?sortBy=currentState"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/hal+json"))
        .andExpect(jsonPath("$.page.totalElements", is(6)))
        .andExpect(jsonPath("$._embedded.negotiations.length()", is(6)))
        .andExpect(jsonPath("$._embedded.negotiations.[0].id", is(NEGOTIATION_2_ID)))
        .andExpect(jsonPath("$._embedded.negotiations.[1].id", is(NEGOTIATION_5_ID)))
        .andExpect(jsonPath("$._embedded.negotiations.[2].id", is(NEGOTIATION_1_ID)))
        .andExpect(jsonPath("$._embedded.negotiations.[3].id", is(NEGOTIATION_3_ID)))
        .andExpect(jsonPath("$._embedded.negotiations.[4].id", is(NEGOTIATION_V2_ID)))
        .andExpect(jsonPath("$._embedded.negotiations.[5].id", is(NEGOTIATION_4_ID)));
  }

  /**
   * It tests that, getting all negotiations without filters for a user that doesn't represent any
   * resource, it returns all the negotiations create by the user.
   */
  @Test
  @WithUserDetails("TheResearcher")
  public void testGetAllForResearcher_whenNoFilters() throws Exception {
    String endpoint =
        "/v3/users/%s/negotiations"
            .formatted(AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId());

    mockMvc
        .perform(MockMvcRequestBuilders.get(endpoint))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/hal+json"))
        .andExpect(jsonPath("$.page.totalElements", is(4)))
        .andExpect(jsonPath("$._embedded.negotiations.length()", is(4)))
        .andExpect(jsonPath("$._embedded.negotiations.[0].id", is(NEGOTIATION_1_ID)))
        .andExpect(jsonPath("$._embedded.negotiations.[1].id", is(NEGOTIATION_2_ID)))
        .andExpect(jsonPath("$._embedded.negotiations.[2].id", is(NEGOTIATION_5_ID)))
        .andExpect(jsonPath("$._embedded.negotiations.[3].id", is(NEGOTIATION_V2_ID)));
  }

  /** It tests that using an unsupported sort column it returns 400 Bad Request */
  @Test
  @WithUserDetails("TheResearcher")
  public void testGetAllForResearcher_whenUnknownSortBy() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(
                "/v3/users/%s/negotiations?sortBy=UNK"
                    .formatted(AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId())))
        .andExpect(status().isBadRequest());
  }

  /** It tests that using an unknown param it returns 400 Bad Request */
  @Test
  @WithUserDetails("TheResearcher")
  @Disabled
  public void testGetAllForResearcher_whenUnknownParameter() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(
                "/v3/users/%s/negotiations?unkParam=something"
                    .formatted(AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId())))
        .andExpect(status().isBadRequest());
  }

  /** It tests that using an unsupported sort column it returns 400 Bad Request */
  @Test
  @WithUserDetails("TheResearcher")
  public void testGetAllForResearcher_whenUnknownSortOrder() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("/v3/users/1/negotiations?sortOrder=UNK"))
        .andExpect(status().isBadRequest());
  }

  /** It tests getting negotiations for researtcher with custom pagination data. */
  @Test
  @WithUserDetails("TheResearcher")
  public void testGetAllForResearcher_whenNoFilters_customPagination() throws Exception {
    int pageSize = 2;
    String endpoint =
        "/v3/users/%s/negotiations"
            .formatted(AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId());
    String firstLink =
        "http://localhost%s?sortBy=creationDate&sortOrder=DESC&page=0&size=%s"
            .formatted(endpoint, pageSize);
    String lastLink =
        "http://localhost%s?sortBy=creationDate&sortOrder=DESC&page=1&size=%s"
            .formatted(endpoint, pageSize);

    mockMvc
        .perform(MockMvcRequestBuilders.get("%s?size=%s".formatted(endpoint, pageSize)))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/hal+json"))
        .andExpect(jsonPath("$.page.totalElements", is(4)))
        .andExpect(jsonPath("$.page.totalPages", is(2)))
        .andExpect(jsonPath("$._embedded.negotiations.length()", is(2)))
        .andExpect(jsonPath("$._embedded.negotiations.[0].id", is(NEGOTIATION_1_ID)))
        .andExpect(jsonPath("$._embedded.negotiations.[1].id", is(NEGOTIATION_2_ID)))
        .andExpect(jsonPath("$._links.current.href", is(firstLink))) // they are the same
        .andExpect(jsonPath("$._links.last.href", is(lastLink)));
  }

  /** It tests sorting by status */
  @Test
  @WithUserDetails("TheResearcher")
  public void testGetAllForResearcher_whenNoFilters_sortedByCurrentState_DefaultOrder()
      throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(
                "/v3/users/%s/negotiations?sortBy=currentState"
                    .formatted(AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId())))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/hal+json"))
        .andExpect(jsonPath("$.page.totalElements", is(4)))
        .andExpect(jsonPath("$._embedded.negotiations.length()", is(4)))
        .andExpect(jsonPath("$._embedded.negotiations.[0].id", is(NEGOTIATION_2_ID)))
        .andExpect(jsonPath("$._embedded.negotiations.[1].id", is(NEGOTIATION_5_ID)))
        .andExpect(jsonPath("$._embedded.negotiations.[2].id", is(NEGOTIATION_1_ID)))
        .andExpect(jsonPath("$._embedded.negotiations.[3].id", is(NEGOTIATION_V2_ID)));
  }

  /** It tests sorting by status */
  @Test
  @WithUserDetails("TheResearcher")
  public void testGetAllForResearcher_whenNoFilters_sortedByCurrentStateAscending()
      throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(
                "/v3/users/%s/negotiations?sortBy=currentState&sortOrder=ASC"
                    .formatted(AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId())))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/hal+json"))
        .andExpect(jsonPath("$.page.totalElements", is(4)))
        .andExpect(jsonPath("$._embedded.negotiations.length()", is(4)))
        .andExpect(jsonPath("$._embedded.negotiations.[0].id", is(NEGOTIATION_V2_ID)))
        .andExpect(jsonPath("$._embedded.negotiations.[1].id", is(NEGOTIATION_1_ID)))
        .andExpect(jsonPath("$._embedded.negotiations.[2].id", is(NEGOTIATION_2_ID)));
  }

  /** It tests sorting by status */
  @Test
  @WithUserDetails("TheResearcher")
  public void testGetAllForResearcher_whenNoFilters_sortedByCurrentStateDescending()
      throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(
                "/v3/users/%s/negotiations?sortBy=currentState&sortOrder=DESC"
                    .formatted(AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId())))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/hal+json"))
        .andExpect(jsonPath("$.page.totalElements", is(4)))
        .andExpect(jsonPath("$._embedded.negotiations.length()", is(4)))
        .andExpect(jsonPath("$._embedded.negotiations.[0].id", is(NEGOTIATION_2_ID)))
        .andExpect(jsonPath("$._embedded.negotiations.[1].id", is(NEGOTIATION_5_ID)))
        .andExpect(jsonPath("$._embedded.negotiations.[2].id", is(NEGOTIATION_1_ID)))
        .andExpect(jsonPath("$._embedded.negotiations.[3].id", is(NEGOTIATION_V2_ID)));
  }

  /** It tests sorting by title default order (descending) */
  @Test
  @WithUserDetails("TheResearcher")
  public void testGetAllForResearcher_whenNoFilters_sortedByCurrentTitleDefault() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(
                "/v3/users/%s/negotiations?sortBy=title"
                    .formatted(AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId())))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/hal+json"))
        .andExpect(jsonPath("$.page.totalElements", is(4)))
        .andExpect(jsonPath("$._embedded.negotiations.length()", is(4)))
        .andExpect(jsonPath("$._embedded.negotiations.[0].id", is(NEGOTIATION_5_ID)))
        .andExpect(jsonPath("$._embedded.negotiations.[1].id", is(NEGOTIATION_2_ID)))
        .andExpect(jsonPath("$._embedded.negotiations.[2].id", is(NEGOTIATION_1_ID)))
        .andExpect(jsonPath("$._embedded.negotiations.[3].id", is(NEGOTIATION_V2_ID)));
  }

  /** It tests sorting by title ascending */
  @Test
  @WithUserDetails("TheResearcher")
  public void testGetAllForResearcher_whenNoFilters_sortedByCurrentTitleAscending()
      throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(
                "/v3/users/%s/negotiations?sortBy=title&sortOrder=ASC"
                    .formatted(AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId())))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/hal+json"))
        .andExpect(jsonPath("$.page.totalElements", is(4)))
        .andExpect(jsonPath("$._embedded.negotiations.length()", is(4)))
        .andExpect(jsonPath("$._embedded.negotiations.[0].id", is(NEGOTIATION_V2_ID)))
        .andExpect(jsonPath("$._embedded.negotiations.[1].id", is(NEGOTIATION_1_ID)))
        .andExpect(jsonPath("$._embedded.negotiations.[2].id", is(NEGOTIATION_2_ID)));
  }

  /** It tests sorting by title descending */
  @Test
  @WithUserDetails("TheResearcher")
  public void testGetAllForResearcher_whenNoFilters_sortedByCurrentTitleDescending()
      throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(
                "/v3/users/%s/negotiations?sortBy=title&sortOrder=DESC"
                    .formatted(AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId())))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/hal+json"))
        .andExpect(jsonPath("$.page.totalElements", is(4)))
        .andExpect(jsonPath("$._embedded.negotiations.length()", is(4)))
        .andExpect(jsonPath("$._embedded.negotiations.[0].id", is(NEGOTIATION_5_ID)))
        .andExpect(jsonPath("$._embedded.negotiations.[1].id", is(NEGOTIATION_2_ID)))
        .andExpect(jsonPath("$._embedded.negotiations.[2].id", is(NEGOTIATION_1_ID)))
        .andExpect(jsonPath("$._embedded.negotiations.[3].id", is(NEGOTIATION_V2_ID)));
  }

  /** It tests sorting by status */
  @Test
  @WithUserDetails("TheResearcher")
  public void testGetAllForResearcher_WrongSortingValue_BadRequest() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(
                "/v3/users/%s/negotiations?sortOrder=UNKNOWN"
                    .formatted(AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId())))
        .andExpect(status().isBadRequest());
  }

  /** It tests sorting by status */
  @Test
  @WithUserDetails("TheResearcher")
  public void testGetAllForResearcher_whenNoFilters_sortedByCurrentStateDefault() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(
                "/v3/users/%s/negotiations?sortBy=currentState"
                    .formatted(AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId())))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/hal+json"))
        .andExpect(jsonPath("$.page.totalElements", is(4)))
        .andExpect(jsonPath("$._embedded.negotiations.length()", is(4)))
        .andExpect(jsonPath("$._embedded.negotiations.[0].id", is(NEGOTIATION_2_ID)))
        .andExpect(jsonPath("$._embedded.negotiations.[1].id", is(NEGOTIATION_5_ID)))
        .andExpect(jsonPath("$._embedded.negotiations.[2].id", is(NEGOTIATION_1_ID)))
        .andExpect(jsonPath("$._embedded.negotiations.[3].id", is(NEGOTIATION_V2_ID)));
  }

  /**
   * It tests that, getting all negotiations filtered by role author for a user that doesn't
   * represent any resource, it returns all the negotiations create by the user.
   */
  @Test
  @WithUserDetails("TheResearcher")
  public void testGetAllForResearcher_whenFiltersByAuthor() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(
                "/v3/users/%s/negotiations?role=AUTHOR"
                    .formatted(AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId())))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/hal+json"))
        .andExpect(jsonPath("$.page.totalElements", is(4)))
        .andExpect(jsonPath("$._embedded.negotiations.length()", is(4)))
        .andExpect(jsonPath("$._embedded.negotiations.[0].id", is(NEGOTIATION_1_ID)))
        .andExpect(jsonPath("$._embedded.negotiations.[1].id", is(NEGOTIATION_2_ID)))
        .andExpect(jsonPath("$._embedded.negotiations.[2].id", is(NEGOTIATION_5_ID)))
        .andExpect(jsonPath("$._embedded.negotiations.[3].id", is(NEGOTIATION_V2_ID)));
  }

  /**
   * It tests that, getting all negotiations filtered by role REPRESENTATIVE for a user that doesn't
   * represent any resource, it returns empty result
   */
  @Test
  @WithUserDetails("TheResearcher")
  public void testGetAllForResearcher_whenFiltersByRepresentative() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(
                "/v3/users/%s/negotiations?role=REPRESENTATIVE"
                    .formatted(AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId())))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/hal+json"))
        .andExpect(jsonPath("$.page.totalElements", is(0)));
  }

  /**
   * It tests that, getting all negotiations filtered by state for a user who has just AUTHOR role,
   * it returns one negotiation
   */
  @Test
  @WithUserDetails("TheResearcher")
  public void testGetAllForResearcher_whenFiltersByState() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(
                "/v3/users/%s/negotiations?status=IN_PROGRESS"
                    .formatted(AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId())))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/hal+json"))
        .andExpect(jsonPath("$.page.totalElements", is(1)))
        .andExpect(jsonPath("$._embedded.negotiations.length()", is(1)))
        .andExpect(jsonPath("$._embedded.negotiations.[0].id", is(NEGOTIATION_1_ID)));
  }

  /**
   * It tests that, getting all negotiations filtered by createdAfter for a user who has just AUTHOR
   * role, it returns two negotiations
   */
  @Test
  @WithUserDetails("TheResearcher")
  public void testGetAllForResearcher_whenFiltersByCreatedAfter() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(
                "/v3/users/%s/negotiations?createdAfter=2023-04-13" // day after negotiation-1
                    .formatted(AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId())))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/hal+json"))
        .andExpect(jsonPath("$.page.totalElements", is(3)))
        .andExpect(jsonPath("$._embedded.negotiations.length()", is(3)))
        .andExpect(jsonPath("$._embedded.negotiations.[0].id", is(NEGOTIATION_1_ID)))
        .andExpect(jsonPath("$._embedded.negotiations.[1].id", is(NEGOTIATION_2_ID)));
  }

  /**
   * It tests that, getting all negotiations filtered by createdBefore for a user who has just
   * AUTHOR role, it returns one negotiation
   */
  @Test
  @WithUserDetails("TheResearcher")
  public void testGetAllForResearcher_whenFiltersByCreatedBefore() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(
                "/v3/users/%s/negotiations?createdBefore=2023-04-13" // day after negotiation-1
                    .formatted(AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId())))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/hal+json"))
        .andExpect(jsonPath("$.page.totalElements", is(1)))
        .andExpect(jsonPath("$._embedded.negotiations.length()", is(1)))
        .andExpect(jsonPath("$._embedded.negotiations.[0].id", is(NEGOTIATION_V2_ID)));
  }

  /**
   * It tests that, getting all negotiations filtered by createdBefore for a user who has just
   * AUTHOR role, it returns one negotiation
   */
  @Test
  @WithUserDetails("TheResearcher")
  public void testGetAllForResearcher_whenFiltersByStartAndCreatedBefore() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(
                "/v3/users/%s/negotiations?createdAfter=2023-04-13&createdBefore=2024-04-13"
                    .formatted(AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId())))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/hal+json"))
        .andExpect(jsonPath("$.page.totalElements", is(2)))
        .andExpect(jsonPath("$._embedded.negotiations.length()", is(2)))
        .andExpect(jsonPath("$._embedded.negotiations.[0].id", is(NEGOTIATION_2_ID)));
  }

  /**
   * It tests that, getting all negotiations without filters for a user that is just a
   * REPRESENTATIVE it returns all the negotiations involving resources represented by the user
   */
  @Test
  @WithUserDetails("TheBiobanker")
  public void testGetAllForBiobanker_whenNoFilters() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(
                "/v3/users/%s/negotiations"
                    .formatted(AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId())))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/hal+json"))
        .andExpect(jsonPath("$.page.totalElements", is(4)))
        .andExpect(jsonPath("$._embedded.negotiations.length()", is(4)))
        .andExpect(jsonPath("$._embedded.negotiations.[0].id", is(NEGOTIATION_1_ID)))
        .andExpect(jsonPath("$._embedded.negotiations.[1].id", is(NEGOTIATION_5_ID)))
        .andExpect(jsonPath("$._embedded.negotiations.[2].id", is(NEGOTIATION_3_ID)))
        .andExpect(jsonPath("$._embedded.negotiations.[3].id", is(NEGOTIATION_4_ID)));
  }

  /**
   * It tests that, getting all negotiations filtered by role AUTHOR for a user that is just a
   * REPRESENTATIVE it returns emptyt result
   */
  @Test
  @WithUserDetails("TheBiobanker")
  public void testGetAllForBiobanker_whenFiltersByAuthor() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(
                "/v3/users/%s/negotiations?role=AUTHOR"
                    .formatted(AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId())))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/hal+json"))
        .andExpect(jsonPath("$.page.totalElements", is(0)));
  }

  /**
   * It tests that, getting all negotiations filtered by role REPRESENTATIVE for a user that is just
   * a REPRESENTATIVE it returns all the negotiations involving resources represented by the user
   */
  @Test
  @WithUserDetails("TheBiobanker")
  public void testGetAllForBiobanker_whenFiltersByRepesentative() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(
                "/v3/users/%s/negotiations"
                    .formatted(AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId())))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/hal+json"))
        .andExpect(jsonPath("$.page.totalElements", is(4)))
        .andExpect(jsonPath("$._embedded.negotiations.length()", is(4)))
        .andExpect(jsonPath("$._embedded.negotiations.[0].id", is(NEGOTIATION_1_ID)))
        .andExpect(jsonPath("$._embedded.negotiations.[1].id", is(NEGOTIATION_5_ID)))
        .andExpect(jsonPath("$._embedded.negotiations.[2].id", is(NEGOTIATION_3_ID)))
        .andExpect(jsonPath("$._embedded.negotiations.[3].id", is(NEGOTIATION_4_ID)));
  }

  /**
   * It tests that, getting all negotiations without filters for a user that is AUTHOR and
   * REPRESENTATIVE, it returns both the negotiations where the user is author and the ones
   * involving resources for which the user is a representative
   */
  @Test
  @WithUserDetails("SarahRepr")
  public void testGetAllForUserBothAuthorAndBiobanker_whenNoFilters() throws Exception {

    mockMvc
        .perform(
            MockMvcRequestBuilders.get(
                "/v3/users/%s/negotiations"
                    .formatted(AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId())))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/hal+json"))
        .andExpect(jsonPath("$.page.totalElements", is(4)))
        .andExpect(jsonPath("$._embedded.negotiations.length()", is(4)))
        .andExpect(jsonPath("$._embedded.negotiations.[0].id", is(NEGOTIATION_5_ID)))
        .andExpect(
            jsonPath(
                "$._embedded.negotiations.[0]._links.self.href",
                is(SELF_LINK_TPL.formatted(NEGOTIATION_5_ID))))
        .andExpect(
            jsonPath(
                "$._embedded.negotiations.[0]._links.posts.href",
                is(POSTS_LINK_TPL.formatted(NEGOTIATION_5_ID))))
        .andExpect(
            jsonPath(
                "$._embedded.negotiations.[0]._links.attachments.href",
                is(ATTACHMENTS_LINK_TPL.formatted(NEGOTIATION_5_ID))))
        .andExpect(jsonPath("$._embedded.negotiations.[1].id", is(NEGOTIATION_3_ID)))
        .andExpect(jsonPath("$._embedded.negotiations.[2].id", is(NEGOTIATION_4_ID)))
        .andExpect(jsonPath("$._embedded.negotiations.[3].id", is(NEGOTIATION_V2_ID)))
        .andDo(print());
  }

  /**
   * It tests that, getting all negotiations filtered by AUTHOR for a user that is AUTHOR and
   * REPRESENTATIVE, it returns only the negotiations where the user is the author
   */
  @Test
  @WithUserDetails("SarahRepr")
  public void testGetAllForUserBothAuthorAndBiobanker_whenFiltersByAuthor() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(
                "/v3/users/%s/negotiations?role=AUTHOR"
                    .formatted(AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId())))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/hal+json"))
        .andExpect(jsonPath("$.page.totalElements", is(2)))
        .andExpect(jsonPath("$._embedded.negotiations.length()", is(2)))
        .andExpect(jsonPath("$._embedded.negotiations.[0].id", is(NEGOTIATION_3_ID)))
        .andExpect(
            jsonPath(
                "$._embedded.negotiations.[0]._links.self.href",
                is(SELF_LINK_TPL.formatted(NEGOTIATION_3_ID))))
        .andExpect(
            jsonPath(
                "$._embedded.negotiations.[0]._links.posts.href",
                is(POSTS_LINK_TPL.formatted(NEGOTIATION_3_ID))))
        .andExpect(
            jsonPath(
                "$._embedded.negotiations.[0]._links.attachments.href",
                is(ATTACHMENTS_LINK_TPL.formatted(NEGOTIATION_3_ID))))
        .andExpect(jsonPath("$._embedded.negotiations.[1].id", is(NEGOTIATION_4_ID)));
  }

  /**
   * It tests that, getting all negotiations filtered by REPRESENTATIVE for a user that is AUTHOR
   * and REPRESENTATIVE, it returns only the negotiations involving resources for which the user is
   * a representative
   */
  @Test
  @WithUserDetails("SarahRepr")
  public void testGetAllForUserBothAuthorAndBiobanker_whenFiltersByRepresentative()
      throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(
                "/v3/users/%s/negotiations?role=REPRESENTATIVE"
                    .formatted(AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId())))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/hal+json"))
        .andExpect(jsonPath("$.page.totalElements", is(3)))
        .andExpect(jsonPath("$._embedded.negotiations.length()", is(3)))
        .andExpect(jsonPath("$._embedded.negotiations.[0].id", is(NEGOTIATION_5_ID)))
        .andExpect(
            jsonPath(
                "$._embedded.negotiations.[0]._links.self.href",
                is(SELF_LINK_TPL.formatted(NEGOTIATION_5_ID))))
        .andExpect(
            jsonPath(
                "$._embedded.negotiations.[0]._links.posts.href",
                is(POSTS_LINK_TPL.formatted(NEGOTIATION_5_ID))))
        .andExpect(
            jsonPath(
                "$._embedded.negotiations.[0]._links.attachments.href",
                is(ATTACHMENTS_LINK_TPL.formatted(NEGOTIATION_5_ID))))
        .andExpect(jsonPath("$._embedded.negotiations.[1].id", is(NEGOTIATION_4_ID)))
        .andExpect(jsonPath("$._embedded.negotiations.[2].id", is(NEGOTIATION_V2_ID)));
  }

  /** Tests applying all filters */
  @Test
  @WithUserDetails("SarahRepr")
  public void testGetAllForUserBothAuthorAndBiobanker_whenFiltersByAllAvailableFilters()
      throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(
                "/v3/users/%s/negotiations?role=AUTHOR&status=ABANDONED&createdAfter=2024-01-09&createdBefore=2024-01-11"
                    .formatted(AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId())))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/hal+json"))
        .andExpect(jsonPath("$.page.totalElements", is(1)))
        .andExpect(jsonPath("$._embedded.negotiations.length()", is(1)))
        .andExpect(jsonPath("$._embedded.negotiations.[0].id", is(NEGOTIATION_4_ID)))
        .andExpect(
            jsonPath(
                "$._embedded.negotiations.[0]._links.self.href",
                is(SELF_LINK_TPL.formatted(NEGOTIATION_4_ID))))
        .andExpect(
            jsonPath(
                "$._embedded.negotiations.[0]._links.posts.href",
                is(POSTS_LINK_TPL.formatted(NEGOTIATION_4_ID))))
        .andExpect(
            jsonPath(
                "$._embedded.negotiations.[0]._links.attachments.href",
                is(ATTACHMENTS_LINK_TPL.formatted(NEGOTIATION_4_ID))));
  }

  @Test
  public void testGetById_Unauthorized_whenNoAuth() throws Exception {
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.GET,
        "",
        status().isUnauthorized(),
        anonymous(),
        "%s/%s".formatted(NEGOTIATIONS_URL, NEGOTIATION_1_ID));
  }

  @Test
  public void testGetById_Unauthorized_whenBasicAuth() throws Exception {
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.GET,
        "",
        status().isUnauthorized(),
        httpBasic("researcher", "wrong_pass"),
        "%s/%s".formatted(NEGOTIATIONS_URL, NEGOTIATION_1_ID));
  }

  @Test
  @WithUserDetails("TheResearcher")
  public void testGetById_NotFound_whenWrongId() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("%s/-1".formatted(NEGOTIATIONS_URL)))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithUserDetails("TheResearcher")
  public void testGetById_Ok_whenCorrectId() throws Exception {
    String selfLink = "http://localhost/v3/negotiations/%s".formatted(NEGOTIATION_1_ID);
    String postsLink = "http://localhost/v3/negotiations/%s/posts".formatted(NEGOTIATION_1_ID);
    String attachmentsLink =
        "http://localhost/v3/negotiations/%s/attachments".formatted(NEGOTIATION_1_ID);

    mockMvc
        .perform(MockMvcRequestBuilders.get("%s/%s".formatted(NEGOTIATIONS_URL, NEGOTIATION_1_ID)))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/hal+json"))
        .andExpect(jsonPath("$.id", is(NEGOTIATION_1_ID)))
        .andExpect(jsonPath("$.creationDate", is(NEGOTIATION_1_CREATION_DATE)))
        .andExpect(jsonPath("$._links.self.href", is(selfLink)))
        .andExpect(jsonPath("$._links.posts.href", is(postsLink)))
        .andExpect(jsonPath("$._links.attachments.href", is(attachmentsLink)));
  }

  @Test
  public void testCreate_Unauthorized_whenNoAuth() throws Exception {
    TestUtils.checkErrorResponse(
        mockMvc, HttpMethod.POST, "", status().isUnauthorized(), anonymous(), NEGOTIATIONS_URL);
  }

  @Test
  public void testCreate_Unauthorized_whenWrongAuth() throws Exception {
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        "",
        status().isUnauthorized(),
        httpBasic("researcher", "wrong_pass"),
        NEGOTIATIONS_URL);
  }

  @Test
  public void testCreate_Unauthorized() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.post(NEGOTIATIONS_URL))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser
  public void testCreate_BadRequest_whenRequests_IsMissing() throws Exception {
    NegotiationCreateDTO request = TestUtils.createNegotiation(REQUEST_UNASSIGNED, false);
    request.setRequest(null);
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(NEGOTIATIONS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.jsonFromRequest(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser
  public void testCreate_BadRequest_whenRequests_IsEmpty() throws Exception {
    NegotiationCreateDTO request = TestUtils.createNegotiation(null, false);
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(NEGOTIATIONS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.jsonFromRequest(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithUserDetails("researcher")
  public void testCreate_BadRequest_whenSomeRequests_IsNotFound() throws Exception {
    NegotiationCreateDTO request = TestUtils.createNegotiation("unknown", false);
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(NEGOTIATIONS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.jsonFromRequest(request)))
        .andExpect(status().isBadRequest());
  }

  @Disabled
  @Test
  @WithUserDetails("researcher")
  public void testCreate_BadRequest_whenRequest_IsAlreadyAssignedToAnotherRequest()
      throws Exception {
    // It tries to create a request by assigning the already assigned REQUEST_1
    NegotiationCreateDTO negotiationBody = TestUtils.createNegotiation(REQUEST_1_ID, false);
    String requestBody = TestUtils.jsonFromRequest(negotiationBody);
    long previousRequestCount = negotiationRepository.count();
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(NEGOTIATIONS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    assertEquals(negotiationRepository.count(), previousRequestCount);
  }

  @Test
  @WithUserDetails("researcher") // researcher not
  @Transactional
  @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
  public void testCreate_Ok() throws Exception {
    NegotiationCreateDTO request = TestUtils.createNegotiation(REQUEST_UNASSIGNED, false);
    String requestBody = TestUtils.jsonFromRequest(request);
    long previousRequestCount = negotiationRepository.count();
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post(URI.create(NEGOTIATIONS_URL))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").isString())
            .andExpect(jsonPath("$.publicPostsEnabled", is(true)))
            .andExpect(jsonPath("$.privatePostsEnabled", is(false)))
            .andExpect(jsonPath("$.payload.project.title", is("Title")))
            .andExpect(jsonPath("$.payload.samples.num-of-subjects", is(10)))
            .andExpect(jsonPath("$.payload.ethics-vote.ethics-vote", is("My ethic vote")))
            .andReturn();
    assertEquals(negotiationRepository.count(), previousRequestCount + 1);
    String negotiationId = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    Optional<Negotiation> negotiation = negotiationRepository.findById(negotiationId);
    assert negotiation.isPresent();
    assertEquals(negotiation.get().getCreatedBy().getName(), "researcher");
    assertFalse(requestRepository.existsById(REQUEST_UNASSIGNED));
  }

  @Test
  @WithUserDetails("researcher") // researcher not
  public void testCreate_Ok_orderIsPreserved() throws Exception {
    NegotiationCreateDTO request = TestUtils.createNegotiation(REQUEST_UNASSIGNED, false);
    JsonNode jsonPayload = getJsonNode();
    request.setPayload(jsonPayload);
    String requestBody = TestUtils.jsonFromRequest(request);
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post(URI.create(NEGOTIATIONS_URL))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andExpect(status().isCreated())
            .andReturn();
    String negotiationId = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    ObjectMapper mapper = new ObjectMapper();
    JsonPath.read(result.getResponse().getContentAsString(), "$.payload");
    Object actualPayloadObject;
    String expectedPayloadJson = mapper.writeValueAsString(request.getPayload());
    MvcResult result2 =
        mockMvc
            .perform(MockMvcRequestBuilders.get(NEGOTIATIONS_URL + "/" + negotiationId))
            .andExpect(status().isOk())
            .andReturn();
    actualPayloadObject = JsonPath.read(result2.getResponse().getContentAsString(), "$.payload");
    String actualPayloadJson = mapper.writeValueAsString(actualPayloadObject);
    assertEquals(expectedPayloadJson, actualPayloadJson);
  }

  private static JsonNode getJsonNode() throws JsonProcessingException {
    String payload =
        """
                {"project":{"title":"sdfsdfsd","diseaese-code":null,"objective":null,"organization":"sdfsdf","profit":"Yes","acknowledgment":null,"Multichoice test":[],"Bool test":null,"Single test":"first_choice"},"request":{"description":null,"collection":null,"donors":null,"samples":null,"specifics":null},"ethics-vote":{"ethics-vote":null,"ethics-vote-attachment":null}}""";
    ;
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonPayload = mapper.readTree(payload);
    return jsonPayload;
  }

  @Test
  @WithMockNegotiatorUser(id = 108L)
  @Transactional
  public void testUpdate_Ok() throws Exception {
    NegotiationUpdateDTO updateDTO = new NegotiationUpdateDTO();
    updateDTO.setPayload(
        new ObjectMapper()
            .readTree(
                """
                        {
                    "project": {
                    "title": "Updated",
                    "description": "Description"
                    },
                     "samples": {
                       "sample-type": "DNA",
                       "num-of-subjects": 20,
                       "num-of-samples": 20,
                       "volume-per-sample": 5
                     },
                     "ethics-vote": {
                       "ethics-vote": "No"
                     }
                    }
                    """));
    mockMvc
        .perform(
            MockMvcRequestBuilders.patch("/v3/negotiations/negotiation-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.jsonFromRequest(updateDTO)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").isString())
        .andExpect(jsonPath("$.payload.project.title", is("Updated")))
        .andExpect(jsonPath("$.payload.samples.num-of-subjects", is(20)))
        .andExpect(jsonPath("$.payload.ethics-vote.ethics-vote", is("No")));
  }

  @Test
  public void testUpdate_Unauthorized_whenNoAuth() throws Exception {
    NegotiationCreateDTO negotiationBody = TestUtils.createNegotiation(REQUEST_2_ID, false);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.PATCH,
        negotiationBody,
        status().isUnauthorized(),
        anonymous(),
        "%s/1".formatted(NEGOTIATIONS_URL));
  }

  @Test
  public void testUpdate_Unauthorized_whenWrongAuth() throws Exception {
    NegotiationCreateDTO request = TestUtils.createNegotiation(REQUEST_UNASSIGNED, false);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.PATCH,
        request,
        status().isUnauthorized(),
        httpBasic("admin", "wrong_pass"),
        "%s/1".formatted(NEGOTIATIONS_URL));
  }

  @Test
  @WithUserDetails("TheResearcher")
  @Transactional
  public void testUpdate_Ok_whenChangePayload() throws Exception {
    NegotiationCreateDTO request = TestUtils.createNegotiation(REQUEST_1_ID, false);
    String requestBody = TestUtils.jsonFromRequest(request);
    requestBody = requestBody.replace("Title", "New Title");
    mockMvc
        .perform(
            MockMvcRequestBuilders.patch("%s/%s".formatted(NEGOTIATIONS_URL, NEGOTIATION_1_ID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andReturn();
    Optional<Negotiation> negotiation = negotiationRepository.findById(NEGOTIATION_1_ID);
    negotiation.ifPresent(value -> assertEquals(value.getModifiedBy().getName(), "TheResearcher"));
  }

  @Test
  @WithUserDetails("researcher")
  public void testNoNegotiationsAreReturned() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(
                "/v3/users/%s/negotiations"
                    .formatted(AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId())))
        .andExpect(status().isOk());
  }

  @Test
  @WithUserDetails("TheResearcher")
  void testGetNegotiationsUserCreated() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(
                "/v3/users/%s/negotiations?role=AUTHOR"
                    .formatted(AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.negotiations.length()", is(4)));
  }

  @Test
  @WithUserDetails("TheResearcher")
  @Transactional
  void getNegotiation_2000resources_ok() throws Exception {
    Set<Resource> resources = new HashSet<>();
    DiscoveryService discoveryService =
        discoveryServiceRepository.save(DiscoveryService.builder().url("").name("").build());
    for (int i = 0; i < 2000; i++) {
      Organization organization1 =
          organizationRepository.save(
              Organization.builder()
                  .name("test-%s".formatted(i))
                  .description("test-%s".formatted(i))
                  .externalId("biobank-%s".formatted(i))
                  .build());
      Resource resource =
          resourceRepository.save(
              Resource.builder()
                  .organization(organization1)
                  .discoveryService(discoveryService)
                  .sourceId("collection:%s".formatted(i))
                  .name("test")
                  .description("test")
                  .representatives(new HashSet<>())
                  .build());
      resources.add(resource);
    }
    Negotiation negotiation =
        negotiationRepository.findById("negotiation-1").orElseThrow(TestAbortedException::new);
    negotiation.setResources(resources);
    for (Resource resource : resources) {
      negotiation.setStateForResource(resource.getSourceId(), NegotiationResourceState.SUBMITTED);
    }
    negotiationRepository.save(negotiation);
    mockMvc
        .perform(MockMvcRequestBuilders.get(NEGOTIATIONS_URL + "/" + negotiation.getId()))
        .andExpect(status().isOk());
  }

  @Test
  @WithUserDetails("researcher")
  void testGetNegotiationUserShouldNotHaveAccessTo() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("%s/negotiation-1".formatted(NEGOTIATIONS_URL)))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithUserDetails("TheResearcher")
  void testGetNegotiationUserCreatedIsOk() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("%s/negotiation-1".formatted(NEGOTIATIONS_URL)))
        .andExpect(status().isOk());
  }

  @Test
  @WithUserDetails("TheResearcher")
  void testGetNonExistentNegotiationReturnsEmptyBody() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("%s/fake-123".formatted(NEGOTIATIONS_URL)))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithUserDetails("SarahRepr")
  void testGetNegotiationRepresentativeShouldNotHaveAccessTo() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("%s/negotiation-1".formatted(NEGOTIATIONS_URL)))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithUserDetails("TheBiobanker")
  void testGetNegotiationRepresentativeShouldHaveAccessTo() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("%s/%s".formatted(NEGOTIATIONS_URL, NEGOTIATION_5_ID)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(NEGOTIATION_5_ID)));
  }

  @Test
  @WithMockUser(authorities = "ROLE_ADMIN")
  void getNegotiationsForAdmin_hasUnknownRole_BadRequest() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("%s?role=UNKN".formatted(NEGOTIATIONS_URL)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(authorities = "biobank:1:collection:1")
  void getNegotiationsForAdmin_doesNotHaveRoleAdmin_Forbidden() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("%s?role=ROLE_ADMIN".formatted(NEGOTIATIONS_URL)))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockNegotiatorUser(id = 102L) // Assuming a non-admin user
  void sendEvent_NonAdmin_Forbidden() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.put(
                "%s/negotiation-1/lifecycle/APPROVE".formatted(NEGOTIATIONS_URL)))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(authorities = "ROLE_ADMIN")
  void sendEvent_InvalidEvent_BadRequest() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.put(
                "%s/negotiation-1/lifecycle/NONE_EXISTING_VALUE".formatted(NEGOTIATIONS_URL)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockNegotiatorUser(authorities = "ROLE_ADMIN", id = 101L)
  @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
  void sendEvent_ValidInput_ReturnNegotiationState() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.put(
                "%s/negotiation-5/lifecycle/APPROVE".formatted(NEGOTIATIONS_URL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status", is("IN_PROGRESS")));
  }

  @Test
  @WithMockNegotiatorUser(authorities = "ROLE_ADMIN", id = 101L)
  @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
  void sendEvent_ValidLowerCaseInput_ReturnNegotiationState() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.put(
                "%s/negotiation-1/lifecycle/Abandon".formatted(NEGOTIATIONS_URL)))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockNegotiatorUser(authorities = "ROLE_ADMIN", id = 101L)
  @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
  void sendEvent_WithMandatoryMessage_Ok() throws Exception {
    List<Post> posts = postRepository.findByNegotiationId("negotiation-5");
    int numberOfPost = posts.size();
    String message = "{\"message\": \"Request not acceptable\"}";
    mockMvc
        .perform(
            MockMvcRequestBuilders.put(
                    "%s/negotiation-5/lifecycle/DECLINE".formatted(NEGOTIATIONS_URL))
                .contentType(MediaType.APPLICATION_JSON)
                .content(message))
        .andDo(print())
        .andExpect(status().isOk());
    posts = postRepository.findByNegotiationId("negotiation-5");
    Assertions.assertEquals(numberOfPost + 1, posts.size());
  }

  @Test
  @WithUserDetails("TheBiobanker")
  void sendEvent_InvalidResourceEvent_BadRequest() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.put(
                "%s/negotiation-1/resources/biobank:1:collection:1/lifecycle/NONE_EXISTING_VALUE"
                    .formatted(NEGOTIATIONS_URL)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithUserDetails("TheBiobanker")
  void sendEvent_ValidResourceEvent_ReturnResourceLifecycleState() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.put(
                "%s/negotiation-1/resources/biobank:1:collection:1/lifecycle/CONTACT"
                    .formatted(NEGOTIATIONS_URL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is("negotiation-1")));
  }

  @Test
  @WithUserDetails("TheBiobanker")
  void sendEvent_ValidLowerCaseResourceEvent_ReturnResourceLifecycleState() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.put(
                "%s/negotiation-1/resources/biobank:1:collection:1/lifecycle/contact"
                    .formatted(NEGOTIATIONS_URL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is("negotiation-1")));
  }

  @Test
  @WithMockNegotiatorUser(id = 109L, authorities = "ROLE_ADMIN")
  void addResources_emptyList_400() throws Exception {
    Negotiation negotiation = negotiationRepository.findAll().get(0);
    mockMvc
        .perform(
            MockMvcRequestBuilders.patch(
                    "%s/%s/resources".formatted(NEGOTIATIONS_URL, negotiation.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(List.of())))
        .andExpect(status().isBadRequest())
        .andReturn();
  }

  @Test
  @WithMockNegotiatorUser(id = 109L, authorities = "ROLE_ADMIN")
  void addResources_nonExistingNegotiation_404() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.patch("%s/%s/resources".formatted(NEGOTIATIONS_URL, "UNKNOWN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    new ObjectMapper().writeValueAsString(new UpdateResourcesDTO(List.of(1L)))))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockNegotiatorUser(id = 109L, authorities = "ROLE_ADMIN")
  @Transactional
  void addResources_correctPayload_resourcesAdded() throws Exception {
    Negotiation negotiation = negotiationRepository.findAll().get(0);
    int count = negotiation.getResources().size();
    List<Resource> resources =
        resourceRepository.findAll().stream()
            .filter(resource -> !negotiation.getResources().contains(resource))
            .toList();
    mockMvc
        .perform(
            MockMvcRequestBuilders.patch(
                    "%s/%s/resources".formatted(NEGOTIATIONS_URL, negotiation.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    new ObjectMapper()
                        .writeValueAsString(
                            new UpdateResourcesDTO(
                                resources.stream().map(Resource::getId).toList()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.resources.length()", is(resources.size() + count)));
  }

  @Test
  @WithMockNegotiatorUser(id = 109L, authorities = "ROLE_ADMIN")
  @Transactional
  void addResources_correctPayload_resourcesAddedAndWithStatus() throws Exception {
    Negotiation negotiation = negotiationRepository.findById("negotiation-1").get();
    List<Resource> resources = resourceRepository.findAll();
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.patch(
                        "%s/%s/resources".formatted(NEGOTIATIONS_URL, negotiation.getId()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        new ObjectMapper()
                            .writeValueAsString(
                                new UpdateResourcesDTO(
                                    resources.stream().map(Resource::getId).toList()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.resources.length()", is(resources.size())))
            .andReturn();
    JsonNode response = new ObjectMapper().readTree(result.getResponse().getContentAsString());
    JsonNode resourcesAsJson = response.get("_embedded").get("resources");
    for (JsonNode resourceAsJson : resourcesAsJson) {
      assertNotNull(resourceAsJson.get("currentState"));
    }
  }

  @Test
  @WithMockNegotiatorUser(id = 109L, authorities = "ROLE_ADMIN")
  @Transactional
  void addResources_resourcesAlreadyPresent_noChange() throws Exception {
    Negotiation negotiation = negotiationRepository.findAll().get(0);
    negotiation.setStateForResource(
        negotiation.getResources().iterator().next().getSourceId(),
        NegotiationResourceState.REPRESENTATIVE_UNREACHABLE);
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.patch(
                        "%s/%s/resources".formatted(NEGOTIATIONS_URL, negotiation.getId()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        new ObjectMapper()
                            .writeValueAsString(
                                new UpdateResourcesDTO(
                                    negotiation.getResources().stream()
                                        .map(Resource::getId)
                                        .toList()))))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.resources.length()", is(negotiation.getResources().size())))
            .andReturn();
    JsonNode response = new ObjectMapper().readTree(result.getResponse().getContentAsString());
    JsonNode resourcesAsJson = response.get("_embedded").get("resources");
    for (JsonNode resourceAsJson : resourcesAsJson) {
      assertEquals(
          negotiation.getCurrentStateForResource(resourceAsJson.get("sourceId").asText()),
          NegotiationResourceState.valueOf(resourceAsJson.get("currentState").asText()));
    }
  }

  @Test
  @WithMockNegotiatorUser(id = 109L, authorities = "ROLE_ADMIN")
  @Transactional
  void addResources_presentResourcesWithStatusUpdate_statusChanged() throws Exception {
    Negotiation negotiation = negotiationRepository.findAll().get(0);
    NegotiationResourceState expectedState = NegotiationResourceState.RESOURCE_MADE_AVAILABLE;
    List<Long> resourceIds = negotiation.getResources().stream().map(Resource::getId).toList();
    UpdateResourcesDTO updateResourcesDTO = new UpdateResourcesDTO(resourceIds, expectedState);
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.patch(
                        "%s/%s/resources".formatted(NEGOTIATIONS_URL, negotiation.getId()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(new ObjectMapper().writeValueAsString(updateResourcesDTO)))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.resources.length()", is(negotiation.getResources().size())))
            .andReturn();
    JsonNode response = new ObjectMapper().readTree(result.getResponse().getContentAsString());
    JsonNode resourcesAsJson = response.get("_embedded").get("resources");
    for (JsonNode resourceAsJson : resourcesAsJson) {
      assertEquals(
          expectedState,
          NegotiationResourceState.valueOf(resourceAsJson.get("currentState").asText()));
    }
  }

  @Test
  @WithMockNegotiatorUser(id = 109L)
  @Transactional
  void updateResources_asRepresentative_cannotAddNew() throws Exception {
    Negotiation negotiation = negotiationRepository.findAll().get(0);
    List<Resource> resources = resourceRepository.findAll();
    List<Long> resourceIds = resources.stream().map(Resource::getId).toList();
    UpdateResourcesDTO updateResourcesDTO = new UpdateResourcesDTO(resourceIds);
    mockMvc
        .perform(
            MockMvcRequestBuilders.patch(
                    "%s/%s/resources".formatted(NEGOTIATIONS_URL, negotiation.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(updateResourcesDTO)))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockNegotiatorUser(id = 109L)
  @Transactional
  void updateResources_asRepresentative_cannotUpdateOtherResources() throws Exception {
    Negotiation negotiation = negotiationRepository.findAll().get(0);
    List<Resource> resources = resourceRepository.findAll();
    resources.remove(negotiation.getResources().iterator().next());
    resources.forEach(negotiation::addResource);
    Person person = personRepository.findById(109L).get();
    assertFalse(person.getResources().containsAll(negotiation.getResources()));
    UpdateResourcesDTO updateResourcesDTO =
        new UpdateResourcesDTO(
            resources.stream().map(Resource::getId).collect(Collectors.toList()),
            NegotiationResourceState.RESOURCE_MADE_AVAILABLE);
    mockMvc
        .perform(
            MockMvcRequestBuilders.patch(
                    "%s/%s/resources".formatted(NEGOTIATIONS_URL, negotiation.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(updateResourcesDTO)))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithUserDetails("admin")
  void getLifecycleEvents() throws Exception {
    // negotiation-1 status is IN_PROGRESS
    mockMvc
        .perform(
            MockMvcRequestBuilders.get("%s/negotiation-1/lifecycle".formatted(NEGOTIATIONS_URL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(3)))
        .andDo(print())
        .andExpect(jsonPath("$[0].value", is(NegotiationEvent.ABANDON.getValue())))
        .andExpect(jsonPath("$[0].label", is(NegotiationEvent.ABANDON.getLabel())))
        .andExpect(jsonPath("$[0].description", is(NegotiationEvent.ABANDON.getDescription())))
        .andExpect(jsonPath("$[1].value", is(NegotiationEvent.CONCLUDE.getValue())))
        .andExpect(jsonPath("$[1].label", is(NegotiationEvent.CONCLUDE.getLabel())))
        .andExpect(jsonPath("$[1].description", is(NegotiationEvent.CONCLUDE.getDescription())))
        .andExpect(jsonPath("$[2].value", is(NegotiationEvent.PAUSE.getValue())))
        .andExpect(jsonPath("$[2].label", is(NegotiationEvent.PAUSE.getLabel())))
        .andExpect(jsonPath("$[2].description", is(NegotiationEvent.PAUSE.getDescription())));
  }

  @WithMockNegotiatorUser(id = 109L)
  void findAllForNetwork_notAuthorized_throws403() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("/v3/networks/1/negotiations"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockNegotiatorUser(id = 102L)
  void findAllForNetwork_isAuthorized_ok() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("/v3/networks/1/negotiations"))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockNegotiatorUser(id = 102L)
  void accessNegotiation_asNetworkManager_ok() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("/v3/negotiations/negotiation-1"))
        .andExpect(status().isOk());
    mockMvc
        .perform(MockMvcRequestBuilders.get("/v3/negotiations/negotiation-1/resources"))
        .andExpect(status().isOk());
    mockMvc
        .perform(MockMvcRequestBuilders.get("/v3/negotiations/negotiation-1/posts"))
        .andExpect(status().isOk());
  }

  @Test
  public void testDelete_Unauthorized() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.delete("%s/negotiation-1".formatted(NEGOTIATIONS_URL)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithUserDetails("SarahRepr")
  public void testDelete_Forbidden_whenUserNotCreatorOrAdmin() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.delete("%s/negotiation-1".formatted(NEGOTIATIONS_URL)))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithUserDetails("TheResearcher")
  public void testDelete_NotFound() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.delete("%s/unknown".formatted(NEGOTIATIONS_URL)))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithUserDetails("TheResearcher")
  @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
  public void testDelete_conflict_whenWrongStatus() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.delete("%s/negotiation-1".formatted(NEGOTIATIONS_URL)))
        .andExpect(status().isConflict());
  }

  @Test
  @WithUserDetails("TheResearcher")
  @Transactional
  @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
  public void testDelete_ok_whenNegotiatorAuthor() throws Exception {
    Negotiation negotiation = negotiationRepository.findById("negotiation-1").get();
    negotiation.setCurrentState(NegotiationState.DRAFT);
    negotiationRepository.save(negotiation);

    mockMvc
        .perform(MockMvcRequestBuilders.delete("%s/negotiation-1".formatted(NEGOTIATIONS_URL)))
        .andExpect(status().isNoContent());

    assertFalse(negotiationRepository.findById("negotiation-1").isPresent());
  }

  @Test
  @WithUserDetails("admin")
  @Transactional
  @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
  public void testDelete_ok_whenAdmin() throws Exception {
    Negotiation negotiation = negotiationRepository.findById("negotiation-1").get();
    negotiation.setCurrentState(NegotiationState.DRAFT);
    negotiationRepository.save(negotiation);

    mockMvc
        .perform(MockMvcRequestBuilders.delete("%s/negotiation-1".formatted(NEGOTIATIONS_URL)))
        .andExpect(status().isNoContent());

    assertFalse(negotiationRepository.findById("negotiation-1").isPresent());
  }

  @Test
  @WithUserDetails("TheBiobanker")
  void updateNegotiation_notAuthor_throws403() throws Exception {
    NegotiationUpdateDTO updateDTO = new NegotiationUpdateDTO();
    updateDTO.setAuthorSubjectId("idk");
    mockMvc
        .perform(
            MockMvcRequestBuilders.patch("/v3/negotiations/negotiation-1")
                .content(TestUtils.jsonFromRequest(updateDTO))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithUserDetails("TheResearcher")
  void updateNegotiation_transferToNonExistingUser_throws400() throws Exception {
    NegotiationUpdateDTO updateDTO = new NegotiationUpdateDTO();
    updateDTO.setAuthorSubjectId("idk");
    mockMvc
        .perform(
            MockMvcRequestBuilders.patch("/v3/negotiations/negotiation-1")
                .content(TestUtils.jsonFromRequest(updateDTO))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithUserDetails("TheResearcher")
  void updateNegotiation_transferToExistingUser_ok() throws Exception {
    NegotiationUpdateDTO updateDTO = new NegotiationUpdateDTO();
    updateDTO.setAuthorSubjectId("2");
    mockMvc
        .perform(
            MockMvcRequestBuilders.patch("/v3/negotiations/negotiation-1")
                .content(TestUtils.jsonFromRequest(updateDTO))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.author.id", is("102")))
        .andExpect(jsonPath("$.author.name", is("directory")));
    assertEquals(
        102L, negotiationRepository.findById("negotiation-1").get().getCreatedBy().getId());
    mockMvc
        .perform(MockMvcRequestBuilders.get("/v3/negotiations/negotiation-1"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithUserDetails("admin")
  public void testGetPdf_Ok_WhenUserIsCreatorOrAdmin() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("/v3/negotiations/negotiation-3/pdf"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/pdf"))
        .andExpect(
            header().string("Content-Disposition", org.hamcrest.Matchers.containsString(".pdf")));
  }

  @Test
  @WithUserDetails("SarahRepr")
  public void testGetPdf_Forbidden_WhenUserNotCreatorOrAdmin() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("/v3/negotiations/negotiation-1/pdf"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithUserDetails("admin")
  public void testGetPdf_NotFound_WhenNegotiationDoesNotExist() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("/v3/negotiations/non-existent-id/pdf"))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithUserDetails("admin")
  public void testGetPdf_Ok_WithTemplateNameProvided() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("/v3/negotiations/negotiation-3/pdf"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/pdf"))
        .andExpect(
            header().string("Content-Disposition", org.hamcrest.Matchers.containsString(".pdf")));
  }

  @Test
  @WithUserDetails("admin")
  public void testGetPdf_Ok_PayloadWithSpecialCharacters() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("/v3/negotiations/negotiation-5/pdf"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/pdf"))
        .andExpect(
            header().string("Content-Disposition", org.hamcrest.Matchers.containsString(".pdf")));
  }

  @Test
  @WithUserDetails("admin")
  public void getFullPdf_Ok_WhenUserIsAdmin() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("/v3/negotiations/negotiation-3/fullpdf"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/pdf"))
        .andExpect(
            header().string("Content-Disposition", org.hamcrest.Matchers.containsString(".pdf")));
  }

  @Test
  @WithUserDetails("TheResearcher")
  public void getFullPdf_Ok_WhenUserIsCreator() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("/v3/negotiations/negotiation-1/fullpdf"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/pdf"))
        .andExpect(
            header().string("Content-Disposition", org.hamcrest.Matchers.containsString(".pdf")));
  }

  @Test
  @WithUserDetails("SarahRepr")
  public void getFullPdf_Forbidden_WhenUserNotCreatorOrAdmin() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("/v3/negotiations/negotiation-1/fullpdf"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithUserDetails("admin")
  public void getFullPdf_NotFound_WhenNegotiationDoesNotExist() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("/v3/negotiations/non-existent-id/fullpdf"))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithUserDetails("admin")
  public void getFullPdf_Ok_WithLargePayload() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("/v3/negotiations/negotiation-5/fullpdf"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/pdf"))
        .andExpect(
            header().string("Content-Disposition", org.hamcrest.Matchers.containsString(".pdf")));
  }
}
