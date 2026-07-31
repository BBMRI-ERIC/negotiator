package eu.bbmri_eric.negotiator.integration.api.v3;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import eu.bbmri_eric.negotiator.post.Post;
import eu.bbmri_eric.negotiator.post.PostCreateDTO;
import eu.bbmri_eric.negotiator.post.PostRepository;
import eu.bbmri_eric.negotiator.post.PostType;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import eu.bbmri_eric.negotiator.util.WithMockNegotiatorUser;
import jakarta.transaction.Transactional;
import java.net.URI;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@IntegrationTest(loadTestData = true)
@AutoConfigureMockMvc
public class PostControllerTests {

  private static final String NEGOTIATION_1_ID = "negotiation-1";
  private static final String NEGOTIATION_1_ORGANIZATION_ID = "biobank:1";
  private static final String NEGOTIATIONS_URI = "/v3/negotiations";
  private static final String POSTS_URI = "posts";
  private static final String NEGOTIATION_HELPDESK_ID = "negotiation-helpdesk";
  private static final String NEGOTIATION_HELPDESK_ORGANIZATION_ID = "biobank:1";
  public static final String NEGOTIATION_POSTS_URL = "/v3/negotiations/%s/posts";
  private static final String POSTS_ENDPOINT_URI = "/v3/posts";
  private static final String POST_1_RESEARCHER_ID = "post-1-researcher";
  @Autowired private PostRepository postRepository;
  @Autowired private MockMvc mockMvc;

  @Test
  void getNegotiationPosts_notAuthenticated_401() throws Exception {
    mockMvc.perform(get("/v3/negotiations/idk/posts")).andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockNegotiatorUser(id = 104L)
  void getNegotiationPosts_notAuthorized_getAll() throws Exception {
    mockMvc
        .perform(get(NEGOTIATION_POSTS_URL.formatted(NEGOTIATION_1_ID)))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockNegotiatorUser(id = 108L)
  void getNegotiationPosts_author_getAll() throws Exception {
    int count = postRepository.findByNegotiationId(NEGOTIATION_1_ID).size();
    mockMvc
        .perform(get(NEGOTIATION_POSTS_URL.formatted(NEGOTIATION_1_ID)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaTypes.HAL_JSON))
        .andExpect(jsonPath("$._embedded.posts.length()", is(count)));
  }

  @Test
  @WithMockNegotiatorUser(id = 104L, authorities = "ROLE_ADMIN")
  void getNegotiationPosts_admin_getAll() throws Exception {
    int count = postRepository.findByNegotiationId(NEGOTIATION_1_ID).size();
    mockMvc
        .perform(get(NEGOTIATION_POSTS_URL.formatted(NEGOTIATION_1_ID)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaTypes.HAL_JSON))
        .andExpect(jsonPath("$._embedded.posts.length()", is(count)));
  }

  @Test
  @WithMockNegotiatorUser(id = 103L)
  void getNegotiationPosts_representative_getSubset() throws Exception {
    int count = postRepository.findByNegotiationId(NEGOTIATION_1_ID).size();
    mockMvc
        .perform(get(NEGOTIATION_POSTS_URL.formatted(NEGOTIATION_1_ID)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaTypes.HAL_JSON))
        .andExpect(jsonPath("$._embedded.posts.length()", lessThan(count)));
  }

  @Test
  @WithUserDetails("TheResearcher")
  @Transactional
  public void testCreatePublicPostOK() throws Exception {
    PostCreateDTO request = TestUtils.createPostDTO(null, "message", PostType.PUBLIC);
    String requestBody = TestUtils.jsonFromRequest(request);
    String uri = String.format("%s/%s/%s", NEGOTIATIONS_URI, NEGOTIATION_1_ID, POSTS_URI);

    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post(URI.create(uri))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(MediaTypes.HAL_JSON_VALUE))
            .andExpect(jsonPath("$.text", is("message")))
            .andReturn();

    String postId = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    Optional<Post> post = postRepository.findById(postId);
    assert post.isPresent();
    assertEquals("TheResearcher", post.get().getCreatedBy().getName());
  }

  @Test
  public void testCreatePublicPostUnauthorized() throws Exception {
    PostCreateDTO request = TestUtils.createPostDTO(null, "message", PostType.PUBLIC);
    String requestBody = TestUtils.jsonFromRequest(request);
    String uri = String.format("%s/%s/%s", NEGOTIATIONS_URI, NEGOTIATION_1_ID, POSTS_URI);

    mockMvc
        .perform(
            MockMvcRequestBuilders.post(URI.create(uri))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithUserDetails("TheResearcher")
  public void testCreatePrivatePostWithUnknownResource() throws Exception {
    PostCreateDTO request = TestUtils.createPostDTO("Unknown", "message", PostType.PRIVATE);
    String requestBody = TestUtils.jsonFromRequest(request);
    String uri = String.format("%s/%s/%s", NEGOTIATIONS_URI, NEGOTIATION_1_ID, POSTS_URI);

    mockMvc
        .perform(
            MockMvcRequestBuilders.post(URI.create(uri))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isBadRequest())
        .andReturn();
  }

  @Test
  @WithUserDetails("TheResearcher")
  @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
  public void testGetAll_authOk_correctOrderByDate() throws Exception {
    int numberOfPosts = postRepository.findByNegotiationId(NEGOTIATION_1_ID).size();
    String uri = String.format("%s/%s/%s", NEGOTIATIONS_URI, NEGOTIATION_1_ID, POSTS_URI);

    mockMvc
        .perform(get(uri))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.posts.length()", is(numberOfPosts)))
        .andExpect(
            jsonPath(
                "$._embedded.posts[*].id",
                contains(
                    "post-1-representative",
                    "post-1-researcher",
                    "post-2-researcher",
                    "post-3-researcher",
                    "post-2-representative",
                    "post-3-representative",
                    "post-4-representative")));
  }

  @Test
  @WithUserDetails("TheResearcher")
  @Transactional
  public void testCreatePrivatePostOK() throws Exception {
    PostCreateDTO request =
        TestUtils.createPostDTO(NEGOTIATION_1_ORGANIZATION_ID, "message", PostType.PRIVATE);
    String requestBody = TestUtils.jsonFromRequest(request);
    String uri = String.format("%s/%s/%s", NEGOTIATIONS_URI, NEGOTIATION_1_ID, POSTS_URI);

    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post(URI.create(uri))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(MediaTypes.HAL_JSON_VALUE))
            .andExpect(jsonPath("$.text", is("message")))
            .andReturn();

    String postId = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    Optional<Post> post = postRepository.findById(postId);
    assert post.isPresent();
    assertEquals(post.get().getCreatedBy().getName(), "TheResearcher");
  }

  @Test
  public void testCreatePrivatePostUnauthorized() throws Exception {
    PostCreateDTO request =
        TestUtils.createPostDTO(NEGOTIATION_1_ORGANIZATION_ID, "message", PostType.PRIVATE);
    String requestBody = TestUtils.jsonFromRequest(request);
    String uri = String.format("%s/%s/%s", NEGOTIATIONS_URI, NEGOTIATION_1_ID, POSTS_URI);

    mockMvc
        .perform(
            MockMvcRequestBuilders.post(URI.create(uri))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockNegotiatorUser(authorities = "ROLE_HELPDESK_INTEGRATION", id = 110L)
  @Transactional
  void createPrivatePost_asHelpdeskIntegration_ok() throws Exception {
    String postText = "helpdesk message";
    PostCreateDTO request =
        TestUtils.createPostDTO(
            NEGOTIATION_HELPDESK_ORGANIZATION_ID,
            postText,
            PostType.PRIVATE,
            "john.smith@helpdesk.org");
    String requestBody = TestUtils.jsonFromRequest(request);
    String uri = String.format("%s/%s/%s", NEGOTIATIONS_URI, NEGOTIATION_HELPDESK_ID, POSTS_URI);

    mockMvc
        .perform(
            MockMvcRequestBuilders.post(URI.create(uri))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaTypes.HAL_JSON_VALUE))
        .andExpect(jsonPath("$.text", is(postText)))
        .andExpect(jsonPath("$.type", is(PostType.PRIVATE.toString())));
  }

  @Test
  @WithMockNegotiatorUser(authorities = "ROLE_HELPDESK_INTEGRATION", id = 110L)
  void getNegotiationPosts_asHelpdeskIntegration_ok() throws Exception {
    mockMvc
        .perform(get(NEGOTIATION_POSTS_URL.formatted(NEGOTIATION_HELPDESK_ID)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaTypes.HAL_JSON))
        .andExpect(jsonPath("$._embedded.posts.length()", is(2)))
        .andExpect(jsonPath("$._embedded.posts[?(@.type == 'PUBLIC')]", hasSize(1)))
        .andExpect(jsonPath("$._embedded.posts[?(@.type == 'PRIVATE')]", hasSize(1)));
  }

  @Test
  @WithMockNegotiatorUser(id = 110L, authorities = "ROLE_HELPDESK_INTEGRATION")
  @Transactional
  public void testCreatePost_asHelpdeskIntegration_withHelpdeskActor_persistsAndReturnsActor()
      throws Exception {
    PostCreateDTO request =
        TestUtils.createPostDTO(
            NEGOTIATION_HELPDESK_ORGANIZATION_ID,
            "message from helpdesk",
            PostType.PRIVATE,
            "john.smith@helpdesk.org");
    String requestBody = TestUtils.jsonFromRequest(request);
    String uri = String.format("%s/%s/%s", NEGOTIATIONS_URI, "negotiation-helpdesk", POSTS_URI);

    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post(URI.create(uri))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.helpdeskActor", is("john.smith@helpdesk.org")))
            .andReturn();

    String postId = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    Optional<Post> post = postRepository.findById(postId);
    assertTrue(post.isPresent());
    assertEquals("john.smith@helpdesk.org", post.get().getHelpdeskActor());
  }

  @Test
  @WithUserDetails("TheResearcher")
  @Transactional
  public void testCreatePost_asRegularUser_withHelpdeskActorInBody_doesNotPersistActor()
      throws Exception {
    PostCreateDTO request =
        TestUtils.createPostDTO(
            NEGOTIATION_1_ORGANIZATION_ID, "message", PostType.PUBLIC, "injected-actor");
    String requestBody = TestUtils.jsonFromRequest(request);
    String uri = String.format("%s/%s/%s", NEGOTIATIONS_URI, NEGOTIATION_1_ID, POSTS_URI);

    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post(URI.create(uri))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andExpect(status().isCreated())
            .andReturn();

    String postId = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    Optional<Post> post = postRepository.findById(postId);
    assertTrue(post.isPresent());
    assertNull(post.get().getHelpdeskActor());
  }

  @Test
  @WithMockNegotiatorUser(authorities = "ROLE_HELPDESK_INTEGRATION", id = 110L)
  void getPostById_asHelpdeskIntegration_ok() throws Exception {
    mockMvc
        .perform(get(String.format("%s/%s", POSTS_ENDPOINT_URI, POST_1_RESEARCHER_ID)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaTypes.HAL_JSON))
        .andExpect(jsonPath("$.id", is(POST_1_RESEARCHER_ID)));
  }

  @Test
  @WithMockNegotiatorUser(id = 104L)
  void getPostById_notParticipant_forbidden() throws Exception {
    mockMvc
        .perform(get(String.format("%s/%s", POSTS_ENDPOINT_URI, POST_1_RESEARCHER_ID)))
        .andExpect(status().isForbidden());
  }
}
