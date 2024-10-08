package eu.bbmri_eric.negotiator.integration.api.v3;

import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import eu.bbmri_eric.negotiator.post.Post;
import eu.bbmri_eric.negotiator.post.PostCreateDTO;
import eu.bbmri_eric.negotiator.post.PostRepository;
import eu.bbmri_eric.negotiator.post.PostType;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import jakarta.transaction.Transactional;
import java.net.URI;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@IntegrationTest(loadTestData = true)
public class PostControllerTests {

  private static final String NEGOTIATION_1_ID = "negotiation-1";
  private static final String NEGOTIATION_1_ORGANIZATION_ID = "biobank:1";
  private static final String NEGOTIATIONS_URI = "/v3/negotiations";
  private static final String POSTS_URI = "posts";
  private static final String RESEARCHER_ROLE = "ROLE_RESEARCHER";
  private static final String REPRESENTATIVE_ROLE = "REPRESENTATIVE";
  private static final String POST_ID = "post-1-representative";
  @Autowired private WebApplicationContext context;
  @Autowired private PostRepository postRepository;
  private MockMvc mockMvc;

  @BeforeEach
  public void beforeEach() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
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
  public void testGetAll() throws Exception {
    int numberOfPosts = (int) postRepository.count();
    String uri = String.format("%s/%s/%s", NEGOTIATIONS_URI, NEGOTIATION_1_ID, POSTS_URI);
    mockMvc
        .perform(MockMvcRequestBuilders.get(uri))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.posts.length()", is(numberOfPosts)))
        .andExpect(jsonPath("$._embedded.posts[0].id", is("post-1-researcher")))
        .andExpect(jsonPath("$._embedded.posts[1].id", is("post-2-researcher")))
        .andExpect(jsonPath("$._embedded.posts[2].id", is("post-3-researcher")))
        .andExpect(jsonPath("$._embedded.posts[3].id", is("post-1-representative")))
        .andExpect(jsonPath("$._embedded.posts[4].id", is("post-2-representative")))
        .andExpect(jsonPath("$._embedded.posts[5].id", is("post-3-representative")))
        .andExpect(jsonPath("$._embedded.posts[6].id", is("post-4-representative")));
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
