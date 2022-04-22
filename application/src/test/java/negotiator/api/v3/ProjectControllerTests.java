package negotiator.api.v3;

import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.bbmri.eric.csit.service.negotiator.NegotiatorApplication;
import eu.bbmri.eric.csit.service.negotiator.api.v3.ProjectController;
import eu.bbmri.eric.csit.service.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(classes = NegotiatorApplication.class)
@ActiveProfiles("test")
public class ProjectControllerTests {
  @Autowired
  private WebApplicationContext context;
  private MockMvc mockMvc;

  @Autowired private ProjectController projectController;
  @Autowired private ProjectRepository projectRepository;

  private static final String TITLE = "project title";
  private static final String DESCRIPTION = "project description";
  private static final String ETHICS_VOTE = "ethics vote";
  private static final String EXPECTED_END_DATE = "2022-04-13";
  private static final boolean EXPECTED_DATA_GENERATION = true;
  private static final boolean IS_TEST_PROJECT = true;

  @BeforeEach
  public void before() {
    mockMvc = MockMvcBuilders
        .webAppContextSetup(context)
        .apply(springSecurity()).build();
    projectRepository.deleteAll();
  }

  private MockHttpServletRequestBuilder getRequest(String requestBody) {
    return MockMvcRequestBuilders.post("/v3/projects/")
        .with(httpBasic("researcher", "researcher"))
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody);
  }

  @Test
  public void testUnauthorized_whenAuthenticationFails() throws Exception {
    String requestBody = String.format(
        """
        {
        "title": "%s",
        "description": "%s",
        "ethicsVote": "%s",
        "expectedDataGeneration": %b,
        "expectedEndDate": "%s",
        "isTestProject": %b
        }""", TITLE, DESCRIPTION, ETHICS_VOTE, EXPECTED_DATA_GENERATION, EXPECTED_END_DATE, IS_TEST_PROJECT);

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/v3/projects/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
        .andExpect(status().isUnauthorized());
    assertEquals(projectRepository.findAll().size(), 0);
  }
  @Test
  public void testBadRequest_whenTitle_IsMissing() throws Exception {
    String requestBody = String.format(
        """
        {
        "description": "%s",
        "ethicsVote": "%s",
        "expectedDataGeneration": %b,
        "expectedEndDate": "%s",
        "isTestProject": %b
        }""", DESCRIPTION, ETHICS_VOTE, EXPECTED_DATA_GENERATION, EXPECTED_END_DATE, IS_TEST_PROJECT);

    mockMvc
        .perform(getRequest(requestBody))
        .andExpect(status().isBadRequest());
    assertEquals(projectRepository.findAll().size(), 0);
  }

  @Test
  public void testBadRequest_whenDescription_IsMissing() throws Exception {
    String requestBody = String.format(
        """
        {
        "title": "%s",
        "ethicsVote": "%s",
        "expectedDataGeneration": %b,
        "expectedEndDate": "%s",
        "isTestProject": %b
        }""", TITLE, ETHICS_VOTE, EXPECTED_DATA_GENERATION, EXPECTED_END_DATE, IS_TEST_PROJECT);

    mockMvc
        .perform(getRequest(requestBody))
        .andExpect(status().isBadRequest());
    assertEquals(projectRepository.findAll().size(), 0);
  }

  @Test
  public void testBadRequest_whenDescription_IsTooLong() throws Exception {
    String requestBody = String.format(
        """
        {
        "title": "%s",
        "description": "%s",
        "ethicsVote": "%s",
        "expectedDataGeneration": %b,
        "expectedEndDate": "%s",
        "isTestProject": %b
        }""", TITLE, "d".repeat(513), ETHICS_VOTE, EXPECTED_DATA_GENERATION, EXPECTED_END_DATE, IS_TEST_PROJECT);

    mockMvc
        .perform(getRequest(requestBody))
        .andExpect(status().isBadRequest());
    assertEquals(projectRepository.findAll().size(), 0);
  }

  @Test
  public void testBadRequest_whenEthicsVote_IsMissing() throws Exception {
    String requestBody = String.format(
        """
        {
        "title": "%s",
        "description": "%s",
        "expectedDataGeneration": %b,
        "expectedEndDate": "%s",
        "isTestProject": %b
        }""", TITLE, DESCRIPTION, EXPECTED_DATA_GENERATION, EXPECTED_END_DATE, IS_TEST_PROJECT);

    mockMvc
        .perform(getRequest(requestBody))
        .andExpect(status().isBadRequest());
    assertEquals(projectRepository.findAll().size(), 0);
  }

  @Test
  public void testBadRequest_whenEthicsVote_IsTooLong() throws Exception {
    String requestBody = String.format(
        """
        {
        "title": "%s",
        "description": "%s",
        "ethicsVote": "%s",
        "expectedDataGeneration": %b,
        "expectedEndDate": "%s",
        "isTestProject": %b
        }""", TITLE, DESCRIPTION, "e".repeat(513), EXPECTED_DATA_GENERATION, EXPECTED_END_DATE, IS_TEST_PROJECT);

    mockMvc
        .perform(getRequest(requestBody))
        .andExpect(status().isBadRequest());
    assertEquals(projectRepository.findAll().size(), 0);
  }

  @Test
  public void testBadRequest_whenExpectedEndDate_IsMissing() throws Exception {
    String requestBody = String.format(
        """
        {
        "title": "%s",
        "description": "%s",
        "ethicsVote": "%s",
        "expectedDataGeneration": %b,
        "isTestProject": %b
        }""", TITLE, DESCRIPTION, ETHICS_VOTE, EXPECTED_DATA_GENERATION, IS_TEST_PROJECT);

    mockMvc
        .perform(getRequest(requestBody))
        .andExpect(status().isBadRequest());
    assertEquals(projectRepository.findAll().size(), 0);
  }

  @Test
  public void testBadRequest_whenExpectedEndDate_HasWrongFormat() throws Exception {
    String requestBody = String.format(
        """
        {
        "title": "%s",
        "description": "%s",
        "ethicsVote": "%s",
        "expectedEndDate": "%s",
        "expectedDataGeneration": %b,
        "isTestProject": %b
        }""", TITLE, DESCRIPTION, ETHICS_VOTE, "13-04-2022", EXPECTED_DATA_GENERATION, IS_TEST_PROJECT);

    mockMvc
        .perform(getRequest(requestBody))
        .andExpect(status().isBadRequest());
    assertEquals(projectRepository.findAll().size(), 0);
  }

  @Test
  public void testBadRequest_whenExpectedDataGeneration_IsMissing() throws Exception {
    String requestBody = String.format(
        """
        {
        "title": "%s",
        "description": "%s",
        "ethicsVote": "%s",
        "expectedEndDate": "%s",
        "isTestProject": %b
        }""", TITLE, DESCRIPTION, ETHICS_VOTE, EXPECTED_END_DATE, IS_TEST_PROJECT);

    mockMvc
        .perform(getRequest(requestBody))
        .andExpect(status().isBadRequest());
    assertEquals(projectRepository.findAll().size(), 0);
  }

  @Test
  public void testBadRequest_whenExpectedDataGeneration_IsWrongType() throws Exception {
    String requestBody = String.format(
        """
        {
        "title": "%s",
        "description": "%s",
        "ethicsVote": "%s",
        "expectedEndDate": "%s",
        "isTestProject": "%b"
        }""", TITLE, DESCRIPTION, ETHICS_VOTE, EXPECTED_END_DATE, IS_TEST_PROJECT);

    mockMvc
        .perform(getRequest(requestBody))
        .andExpect(status().isBadRequest());
    assertEquals(projectRepository.findAll().size(), 0);
  }

  @Test
  public void testCreated_whenIsTestProject_isDefault() throws Exception {
    String requestBody = String.format(
        """
        {
        "title": "%s",
        "description": "%s",
        "ethicsVote": "%s",
        "expectedDataGeneration": %b,
        "expectedEndDate": "%s"
        }""", TITLE, DESCRIPTION, ETHICS_VOTE, EXPECTED_DATA_GENERATION, EXPECTED_END_DATE);

    mockMvc
        .perform(getRequest(requestBody))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id", is(1)))
        .andExpect(jsonPath("$.title", is(TITLE)))
        .andExpect(jsonPath("$.description", is(DESCRIPTION)))
        .andExpect(jsonPath("$.ethicsVote", is(ETHICS_VOTE)))
        .andExpect(jsonPath("$.expectedDataGeneration", is(true)))
        .andExpect(jsonPath("$.expectedEndDate", is(EXPECTED_END_DATE)))
        .andExpect(jsonPath("$.isTestProject", is(false)));
    assertEquals(projectRepository.findAll().size(), 1);
  }

  @Test
  public void testCreated_whenIsTestProject_isGivenInInput() throws Exception {
    String requestBody = String.format(
        """
        {
        "title": "%s",
        "description": "%s",
        "ethicsVote": "%s",
        "expectedDataGeneration": %b,
        "expectedEndDate": "%s",
        "isTestProject": %b
        }""", TITLE, DESCRIPTION, ETHICS_VOTE, EXPECTED_DATA_GENERATION, EXPECTED_END_DATE, IS_TEST_PROJECT);

    mockMvc
        .perform(getRequest(requestBody))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id", is(3)))  // 3 because it's the current autogenerated value
        .andExpect(jsonPath("$.title", is(TITLE)))
        .andExpect(jsonPath("$.description", is(DESCRIPTION)))
        .andExpect(jsonPath("$.ethicsVote", is(ETHICS_VOTE)))
        .andExpect(jsonPath("$.expectedDataGeneration", is(true)))
        .andExpect(jsonPath("$.expectedEndDate", is(EXPECTED_END_DATE)))
        .andExpect(jsonPath("$.isTestProject", is(IS_TEST_PROJECT)));
    assertEquals(projectRepository.findAll().size(), 1);
  }
}
