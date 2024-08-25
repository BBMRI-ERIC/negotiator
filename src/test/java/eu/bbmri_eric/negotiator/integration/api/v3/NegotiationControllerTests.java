package eu.bbmri_eric.negotiator.integration.api.v3;

import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import eu.bbmri_eric.negotiator.discovery.DiscoveryService;
import eu.bbmri_eric.negotiator.discovery.DiscoveryServiceRepository;
import eu.bbmri_eric.negotiator.governance.organization.Organization;
import eu.bbmri_eric.negotiator.governance.organization.OrganizationRepository;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.governance.resource.ResourceRepository;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.dto.NegotiationCreateDTO;
import eu.bbmri_eric.negotiator.negotiation.request.Request;
import eu.bbmri_eric.negotiator.negotiation.request.RequestRepository;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceState;
import eu.bbmri_eric.negotiator.user.NegotiatorUserDetailsService;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import eu.bbmri_eric.negotiator.util.WithMockNegotiatorUser;
import jakarta.transaction.Transactional;
import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
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

  @Autowired ResourceRepository resourceRepository;

  @Autowired RequestRepository requestRepository;

  @Autowired DiscoveryServiceRepository discoveryServiceRepository;

  @Autowired OrganizationRepository organizationRepository;

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
        .andExpect(jsonPath("$._links.first.href", is(firstLink)))
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
            .formatted(NegotiatorUserDetailsService.getCurrentlyAuthenticatedUserInternalId());

    String link =
        "http://localhost%s?sortBy=creationDate&sortOrder=DESC&page=0&size=50".formatted(endpoint);

    mockMvc
        .perform(MockMvcRequestBuilders.get(endpoint))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/hal+json"))
        .andExpect(jsonPath("$.page.totalElements", is(4)))
        .andExpect(jsonPath("$._embedded.negotiations.length()", is(4)))
        .andExpect(jsonPath("$._embedded.negotiations.[0].id", is(NEGOTIATION_1_ID)))
        .andExpect(jsonPath("$._embedded.negotiations.[1].id", is(NEGOTIATION_2_ID)))
        .andExpect(jsonPath("$._embedded.negotiations.[2].id", is(NEGOTIATION_5_ID)))
        .andExpect(jsonPath("$._embedded.negotiations.[3].id", is(NEGOTIATION_V2_ID)))
        .andExpect(jsonPath("$._links.first.href", is(link)))
        .andExpect(jsonPath("$._links.current.href", is(link)))
        .andExpect(jsonPath("$._links.last.href", is(link)));
  }

  /** It tests that using an unsupported sort column it returns 400 Bad Request */
  @Test
  @WithUserDetails("TheResearcher")
  public void testGetAllForResearcher_whenUnknownSortBy() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(
                "/v3/users/%s/negotiations?sortBy=UNK"
                    .formatted(
                        NegotiatorUserDetailsService.getCurrentlyAuthenticatedUserInternalId())))
        .andExpect(status().isBadRequest());
  }

  /** It tests that using an unknown param it returns 400 Bad Request */
  @Test
  @WithUserDetails("TheResearcher")
  public void testGetAllForResearcher_whenUnknownParameter() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(
                "/v3/users/%s/negotiations?unkParam=something"
                    .formatted(
                        NegotiatorUserDetailsService.getCurrentlyAuthenticatedUserInternalId())))
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
            .formatted(NegotiatorUserDetailsService.getCurrentlyAuthenticatedUserInternalId());
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
        .andExpect(jsonPath("$._links.first.href", is(firstLink)))
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
                    .formatted(
                        NegotiatorUserDetailsService.getCurrentlyAuthenticatedUserInternalId())))
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
                    .formatted(
                        NegotiatorUserDetailsService.getCurrentlyAuthenticatedUserInternalId())))
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
                    .formatted(
                        NegotiatorUserDetailsService.getCurrentlyAuthenticatedUserInternalId())))
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
                    .formatted(
                        NegotiatorUserDetailsService.getCurrentlyAuthenticatedUserInternalId())))
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
                    .formatted(
                        NegotiatorUserDetailsService.getCurrentlyAuthenticatedUserInternalId())))
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
                    .formatted(
                        NegotiatorUserDetailsService.getCurrentlyAuthenticatedUserInternalId())))
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
                    .formatted(
                        NegotiatorUserDetailsService.getCurrentlyAuthenticatedUserInternalId())))
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
                    .formatted(
                        NegotiatorUserDetailsService.getCurrentlyAuthenticatedUserInternalId())))
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
                    .formatted(
                        NegotiatorUserDetailsService.getCurrentlyAuthenticatedUserInternalId())))
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
                    .formatted(
                        NegotiatorUserDetailsService.getCurrentlyAuthenticatedUserInternalId())))
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
                    .formatted(
                        NegotiatorUserDetailsService.getCurrentlyAuthenticatedUserInternalId())))
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
                    .formatted(
                        NegotiatorUserDetailsService.getCurrentlyAuthenticatedUserInternalId())))
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
                    .formatted(
                        NegotiatorUserDetailsService.getCurrentlyAuthenticatedUserInternalId())))
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
                    .formatted(
                        NegotiatorUserDetailsService.getCurrentlyAuthenticatedUserInternalId())))
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
                    .formatted(
                        NegotiatorUserDetailsService.getCurrentlyAuthenticatedUserInternalId())))
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
                    .formatted(
                        NegotiatorUserDetailsService.getCurrentlyAuthenticatedUserInternalId())))
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
                    .formatted(
                        NegotiatorUserDetailsService.getCurrentlyAuthenticatedUserInternalId())))
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
                    .formatted(
                        NegotiatorUserDetailsService.getCurrentlyAuthenticatedUserInternalId())))
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
                    .formatted(
                        NegotiatorUserDetailsService.getCurrentlyAuthenticatedUserInternalId())))
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
                    .formatted(
                        NegotiatorUserDetailsService.getCurrentlyAuthenticatedUserInternalId())))
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
                    .formatted(
                        NegotiatorUserDetailsService.getCurrentlyAuthenticatedUserInternalId())))
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
    NegotiationCreateDTO request = TestUtils.createNegotiation(Set.of(REQUEST_UNASSIGNED));
    request.setRequests(null);
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
    NegotiationCreateDTO request = TestUtils.createNegotiation(Collections.emptySet());
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
    NegotiationCreateDTO request = TestUtils.createNegotiation(Set.of("unknown"));
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(NEGOTIATIONS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.jsonFromRequest(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithUserDetails("researcher")
  public void testCreate_BadRequest_whenRequest_IsAlreadyAssignedToAnotherRequest()
      throws Exception {
    // It tries to create a request by assigning the already assigned REQUEST_1
    NegotiationCreateDTO negotiationBody = TestUtils.createNegotiation(Set.of(REQUEST_1_ID));
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
  public void testCreate_Ok() throws Exception {
    NegotiationCreateDTO request = TestUtils.createNegotiation(Set.of(REQUEST_UNASSIGNED));
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
  }

  @Test
  public void testUpdate_Unauthorized_whenNoAuth() throws Exception {
    NegotiationCreateDTO negotiationBody = TestUtils.createNegotiation(Set.of(REQUEST_2_ID));
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.PUT,
        negotiationBody,
        status().isUnauthorized(),
        anonymous(),
        "%s/1".formatted(NEGOTIATIONS_URL));
  }

  @Test
  public void testUpdate_Unauthorized_whenWrongAuth() throws Exception {
    NegotiationCreateDTO request = TestUtils.createNegotiation(Set.of(REQUEST_UNASSIGNED));
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.PUT,
        request,
        status().isUnauthorized(),
        httpBasic("admin", "wrong_pass"),
        "%s/1".formatted(NEGOTIATIONS_URL));
  }

  @Test
  @WithUserDetails("TheResearcher")
  public void testUpdate_BadRequest_whenRequestIsAlreadyAssignedToAnotherNegotiation()
      throws Exception {
    // It tries to update the known NEGOTIATION_2 by assigning the request of NEGOTIATION_1
    NegotiationCreateDTO updateRequest =
        TestUtils.createNegotiation(Set.of("request-1", "request-2"));
    String requestBody = TestUtils.jsonFromRequest(updateRequest);
    mockMvc
        .perform(
            MockMvcRequestBuilders.put("%s/%s".formatted(NEGOTIATIONS_URL, NEGOTIATION_2_ID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON));
  }

  @Test
  @WithUserDetails("TheResearcher")
  @Transactional
  public void testUpdate_Ok_whenChangePayload() throws Exception {
    // Tries to updated negotiation
    // Negotiation body with updated values
    NegotiationCreateDTO request = TestUtils.createNegotiation(Set.of(REQUEST_1_ID));
    String requestBody = TestUtils.jsonFromRequest(request);
    requestBody = requestBody.replace("Title", "New Title");

    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.put("%s/%s".formatted(NEGOTIATIONS_URL, NEGOTIATION_1_ID))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andExpect(status().isNoContent())
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
                    .formatted(
                        NegotiatorUserDetailsService.getCurrentlyAuthenticatedUserInternalId())))
        .andExpect(status().isOk());
  }

  @Test
  @WithUserDetails("TheResearcher")
  void testGetNegotiationsUserCreated() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(
                "/v3/users/%s/negotiations?role=AUTHOR"
                    .formatted(
                        NegotiatorUserDetailsService.getCurrentlyAuthenticatedUserInternalId())))
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
                  .externalId("biobank-%s".formatted(i))
                  .build());
      Resource resource =
          resourceRepository.save(
              Resource.builder()
                  .organization(organization1)
                  .discoveryService(discoveryService)
                  .sourceId("collection:%s".formatted(i))
                  .name("test")
                  .representatives(new HashSet<>())
                  .build());
      resources.add(resource);
    }
    Request request = requestRepository.findAll().get(0);
    request.setResources(resources);
    for (Resource resource : resources) {
      request
          .getNegotiation()
          .setStateForResource(resource.getSourceId(), NegotiationResourceState.SUBMITTED);
    }
    requestRepository.save(request);
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(NEGOTIATIONS_URL + "/" + request.getNegotiation().getId()))
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
  void getNegotiationsForAdmin_hasRoleAdmin_Ok() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("%s?role=ROLE_ADMIN".formatted(NEGOTIATIONS_URL)))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser(authorities = "biobank:1:collection:1")
  void getNegotiationsForAdmin_doesNotHaveRoleAdmin_Forbidden() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("%s?role=ROLE_ADMIN".formatted(NEGOTIATIONS_URL)))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(authorities = "biobank:1:collection:1")
  void updateLifecycle_doesNotHaveRoleAdmin_Forbidden() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.put(
                "%s/negotiation-1/lifecycle/APPROVE".formatted(NEGOTIATIONS_URL)))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(authorities = "ROLE_USER") // Assuming a non-admin user
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
  @WithMockUser(authorities = "ROLE_ADMIN")
  void sendEvent_ValidInput_ReturnNegotiationState() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.put(
                "%s/negotiation-1/lifecycle/APPROVE".formatted(NEGOTIATIONS_URL)))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser(authorities = "ROLE_ADMIN")
  void sendEvent_ValidLowerCaseInput_ReturnNegotiationState() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.put(
                "%s/negotiation-1/lifecycle/Approve".formatted(NEGOTIATIONS_URL)))
        .andExpect(status().isOk());
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
  void getPossibleLifecycleStages_noAuth_Ok() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("%s/lifecycle".formatted(NEGOTIATIONS_URL)))
        .andExpect(status().isOk());
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
                .content(new ObjectMapper().writeValueAsString(List.of(1L))))
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
                        .writeValueAsString(resources.stream().map(Resource::getId).toList())))
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
                            .writeValueAsString(resources.stream().map(Resource::getId).toList())))
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
                                negotiation.getResources().stream().map(Resource::getId).toList())))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.resources.length()", is(negotiation.getResources().size())))
            .andReturn();
    JsonNode response = new ObjectMapper().readTree(result.getResponse().getContentAsString());
    JsonNode resourcesAsJson = response.get("_embedded").get("resources");
    for (JsonNode resourceAsJson : resourcesAsJson) {
      assertEquals(
          negotiation.getCurrentStatePerResource().get(resourceAsJson.get("sourceId").asText()),
          NegotiationResourceState.valueOf(resourceAsJson.get("currentState").asText()));
    }
  }
}
