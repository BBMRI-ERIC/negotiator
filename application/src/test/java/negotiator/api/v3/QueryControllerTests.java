package negotiator.api.v3;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.text.IsEmptyString.emptyString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.bbmri.eric.csit.service.negotiator.NegotiatorApplication;
import eu.bbmri.eric.csit.service.negotiator.api.v3.QueryController;
import eu.bbmri.eric.csit.service.negotiator.service.QueryService;
import eu.bbmri.eric.csit.service.repository.QueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(classes = NegotiatorApplication.class)
@ActiveProfiles("test")
public class QueryControllerTests {

  private MockMvc mockMvc;
  @Autowired private WebApplicationContext context;
  @Autowired private QueryController queryController;
  @Autowired public QueryService queryService;
  @Autowired public QueryRepository queryRepository;

  @BeforeEach
  public void before() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @Test
  public void testBadRequest_whenUrlFieldIsMissing() throws Exception {
    String requestBody =
        "{\"humanReadable\": \"string\","
            + "\"resources\":[{\"type\": \"biobank\",\"id\":\"biobank:1\",\"children\":[{\"type\":\"collection\",\"id\":\"collection:1\"}]}]}";

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/v3/queries")
                .with(httpBasic("directory", "directory"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void testBadRequest_whenUrlHumanReadableFieldIsMissing() throws Exception {
    String requestBody =
        "{\"url\": \"http://datasource.dev\","
            + "\"resources\":[{\"type\": \"biobank\",\"id\":\"biobank:1\",\"children\":[{\"type\":\"collection\",\"id\":\"collection:1\"}]}]}";

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/v3/queries")
                .with(httpBasic("directory", "directory"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void testBadRequest_whenResourcesFieldIsMissing() throws Exception {
    String requestBody =
        "{\"url\": \"http://datasource.dev\", " + "\"humanReadable\": \"Test request\"}";

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/v3/queries")
                .with(httpBasic("directory", "directory"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void testBadRequest_whenResourcesFieldIsEmpty() throws Exception {
    String requestBody =
        "{\"url\": \"http://datasource.dev\", "
            + "\"humanReadable\": \"Test request\", "
            + "\"resources\": []}";

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/v3/queries")
                .with(httpBasic("directory", "directory"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void testBadRequest_whenCollectionNotFound() throws Exception {
    String requestBody =
        "{\"url\": \"http://datasource.dev\","
            + "\"humanReadable\": \"string\","
            + "\"resources\":[{\"type\": \"biobank\",\"id\":\"biobank:1\",\"children\":[{\"type\":\"collection\",\"id\":\"colllection_unknown\"}]}]}";

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/v3/queries")
                .with(httpBasic("directory", "directory"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void testBadRequest_whenCollectionAndBiobankMismatch() throws Exception {
    String requestBody =
        "{\"url\": \"http://datasource.dev\","
            + "\"humanReadable\": \"string\","
            + "\"resources\":[{\"type\": \"biobank\",\"id\":\"wrong_biobank\",\"children\":[{\"type\":\"collection\",\"id\":\"collection:1\"}]}]}";

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/v3/queries")
                .with(httpBasic("directory", "directory"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isBadRequest())
        .andDo(print());
  }

  @Test
  public void testBadRequest_whenDataSourceNotFound() throws Exception {
    String requestBody =
        "{\"url\": \"http://wrong_data_source\","
            + "\"humanReadable\": \"string\","
            + "\"resources\":[{\"type\": \"biobank\",\"id\":\"biobank:1\",\"children\":[{\"type\":\"collection\",\"id\":\"collection:1\"}]}]}";

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/v3/queries")
                .with(httpBasic("directory", "directory"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isBadRequest())
        .andDo(print());
  }

  @Test
  public void testCreated() throws Exception {
    String requestBody =
        "{\"url\": \"http://datasource.dev\","
            + "\"humanReadable\": \"string\","
            + "\"resources\":[{\"type\": \"biobank\",\"id\":\"biobank:1\",\"children\":[{\"type\":\"collection\",\"id\":\"collection:1\"}]}]}";

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/v3/queries")
                .with(httpBasic("directory", "directory"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id", is(1)))
        .andExpect(jsonPath("$.url", is("http://datasource.dev")))
        .andExpect(jsonPath("$.resources[0].id", is("biobank:1")))
        .andExpect(jsonPath("$.resources[0].type", is("biobank")))
        .andExpect(jsonPath("$.resources[0].children[0].id", is("collection:1")))
        .andExpect(jsonPath("$.resources[0].children[0].type", is("collection")))
        .andExpect(jsonPath("$.queryToken", is(not(emptyString()))));
    assertEquals(queryRepository.findAll().size(), 1);
  }
}
