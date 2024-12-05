package eu.bbmri_eric.negotiator.integration.api.v3;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
  public static final String NEGOTIATION_POSTS_URL = "/v3/negotiations/%s/posts";
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
    PostCreateDTO request = TestUtils.createPostDTO(null, "message", null, PostType.PUBLIC);
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
  public void testCreatePublicPostUnauthorized() throws Exception {
    PostCreateDTO request = TestUtils.createPostDTO(null, "message", null, PostType.PUBLIC);
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
    PostCreateDTO request = TestUtils.createPostDTO("Unknown", "message", null, PostType.PRIVATE);
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
    int numberOfPosts = (int) postRepository.count();
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
        TestUtils.createPostDTO(NEGOTIATION_1_ORGANIZATION_ID, "message", null, PostType.PRIVATE);
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
        TestUtils.createPostDTO(NEGOTIATION_1_ORGANIZATION_ID, "message", null, PostType.PRIVATE);
    String requestBody = TestUtils.jsonFromRequest(request);
    String uri = String.format("%s/%s/%s", NEGOTIATIONS_URI, NEGOTIATION_1_ID, POSTS_URI);

    mockMvc
        .perform(
            MockMvcRequestBuilders.post(URI.create(uri))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isUnauthorized());
  }
}
