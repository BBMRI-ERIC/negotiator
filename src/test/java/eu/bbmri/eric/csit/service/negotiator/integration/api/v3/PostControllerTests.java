package eu.bbmri.eric.csit.service.negotiator.integration.api.v3;

import static org.hamcrest.core.Is.is;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.bbmri.eric.csit.service.negotiator.NegotiatorApplication;
import eu.bbmri.eric.csit.service.negotiator.database.repository.PostRepository;
import eu.bbmri.eric.csit.service.negotiator.dto.post.PostCreateDTO;
import java.net.URI;
import lombok.extern.apachecommons.CommonsLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(classes = NegotiatorApplication.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@CommonsLog
public class PostControllerTests {

  private static final String NEGOTIATION_1_ID = "negotiation-1";
  private static final String NEGOTIATION_2_ID = "negotiation-2";
  private static final String NEGOTIATION_1_RESOURCE_ID = "biobank:1";
  private static final String NEGOTIATION_2_RESOURCE_ID = "biobank:2";
  private static final String NEGOTIATIONS_URI = "/v3/negotiations";
  private static final String POSTS_URI = "posts";
  private static final String RESEARCHER_ROLE = "RESEARCHER";
  private static final String REPRESENTATIVE_ROLE = "REPRESENTATIVE";
  private static final String POST_ID = "post-1-representative";
  @Autowired
  private WebApplicationContext context;
  @Autowired
  private PostRepository postRepository;
  private MockMvc mockMvc;

  @BeforeEach
  public void beforeEach() throws Exception {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @Test
  @WithUserDetails("TheResearcher")
  public void testCreateOK() throws Exception {
    PostCreateDTO request = TestUtils.createPost(NEGOTIATION_1_RESOURCE_ID, "message", null);
    String requestBody = TestUtils.jsonFromRequest(request);
    String uri = String.format("%s/%s/%s", NEGOTIATIONS_URI,
        NEGOTIATION_1_ID, POSTS_URI);
    System.out.println(requestBody);
    System.out.println(uri);
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(URI.create(uri))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.text",
            is("message")))
        .andExpect(jsonPath("$.status",
            is("CREATED")));
  }

  @Test
  @WithUserDetails("TheResearcher")
  public void testGetAll() throws Exception {
    int numberOfPosts = (int) postRepository.count();
    String uri = String.format("%s/%s/%s", NEGOTIATIONS_URI,
        NEGOTIATION_1_ID, POSTS_URI);
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(uri))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.length()", is(numberOfPosts)))
        .andExpect(jsonPath("$[0].id", is("post-1-researcher")))
        .andExpect(jsonPath("$[1].id", is("post-2-researcher")))
        .andExpect(jsonPath("$[2].id", is("post-1-representative")))
        .andExpect(jsonPath("$[3].id", is("post-2-representative")));

  }

  @Test
  @WithUserDetails("TheResearcher")
  public void testGetResearcherPostsOnly() throws Exception {
    int numberOfPosts = (int) postRepository.count();
    String uri = String.format("%s/%s/%s/%s", NEGOTIATIONS_URI,
        NEGOTIATION_1_ID, RESEARCHER_ROLE, POSTS_URI);
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(uri))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.length()", is(2)))
        .andExpect(jsonPath("$[0].id", is("post-1-researcher")))
        .andExpect(jsonPath("$[1].id", is("post-2-researcher")));

  }

  @Test
  @WithUserDetails("TheBiobanker")
  public void testGetRepresentativePostsOnly() throws Exception {
    int numberOfPosts = (int) postRepository.count();
    String uri = String.format("%s/%s/%s/%s", NEGOTIATIONS_URI,
        NEGOTIATION_1_ID, REPRESENTATIVE_ROLE, POSTS_URI);
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(uri))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.length()", is(2)))
        .andExpect(jsonPath("$[0].id", is("post-1-representative")))
        .andExpect(jsonPath("$[1].id", is("post-2-representative")));

  }

  @Test
  @WithUserDetails("TheBiobanker")
  public void testMarkPostAsRead() throws Exception {
    int numberOfPosts = (int) postRepository.count();
    PostCreateDTO request = TestUtils.createPost(NEGOTIATION_1_RESOURCE_ID, "message", "READ");
    String requestBody = TestUtils.jsonFromRequest(request);
    String uri = String.format("%s/%s/%s/%s", NEGOTIATIONS_URI,
        NEGOTIATION_1_ID, POSTS_URI, POST_ID);
    mockMvc
        .perform(
            MockMvcRequestBuilders.put(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status",
            is("READ")));

//    mockMvc
//        .perform(
//            MockMvcRequestBuilders.post(URI.create(uri))
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(requestBody))
//        .andExpect(status().isCreated())
//        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
//        .andExpect(jsonPath("$.text",
//            is("message")))
//        .andExpect(jsonPath("$.status",
//            is("CREATED")));

  }


}
