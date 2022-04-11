package negotiator.api.v3;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.text.IsEmptyString.emptyString;
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

@SpringBootTest(classes = NegotiatorApplication.class)
@ActiveProfiles("test")
public class QueryControllerTests {

  private MockMvc mockMvc;

  @Autowired private QueryController queryController;

  @Autowired public QueryService queryService;

  @Autowired public QueryRepository queryRepository;

  @BeforeEach
  public void before() {
    mockMvc = MockMvcBuilders.standaloneSetup(queryController).build();
  }

  @Test
  public void testBadRequest_whenUrlFieldIsMissing() throws Exception {
    String requestBody =
        "\"humanReadable\": \"string\","
            + "\"resources\": [{\"id\": \"biobank:1\",\"collections\": [{\"id\": \"collection:1\"}]}]}";

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/v3/queries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void testBadRequest_whenUrlHumanReadableFieldIsMissing() throws Exception {
    String requestBody =
        "{\"url\": \"http://datasource.dev\", "
            + "\"resources\": [{\"id\": \"biobank:1\",\"collections\": [{\"id\": \"collection:1\"}]}]}";

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/v3/queries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void testBadRequest_whenResoourcesFieldIsMissing() throws Exception {
    String requestBody =
        "{\"url\": \"http://datasource.dev\", "
            + "\"humanReadable\": \"Test request\"}";

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/v3/queries")
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
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void testBadRequest_whenCollectionNotFound() throws Exception {
    String requestBody =
        "{\"url\": \"http://datasource.dev\", "
            + "\"humanReadable\": \"Test request\", "
            + "\"resources\": [{\"id\": \"biobank:1\",\"collections\": [{\"id\": \"collection:unknown\"}]}]}";

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/v3/queries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void testBadRequest_whenCollectionAndBiobankMismatch() throws Exception {
    String requestBody =
        "{\"url\": \"http://datasource.dev\", "
            + "\"humanReadable\": \"Test request\", "
            + "\"resources\": [{\"id\": \"wrong_biobank\",\"collections\": [{\"id\": \"collection:1\"}]}]}";

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/v3/queries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isBadRequest())
        .andDo(print());
  }

  @Test
  public void testBadRequest_whenDataSourceNotFound() throws Exception {
    String requestBody =
        "{\"url\": \"http://wrong_data_source\", "
            + "\"humanReadable\": \"Test request\", "
            + "\"resources\": [{\"id\": \"biobank:1\",\"collections\": [{\"id\": \"collection:1\"}]}]}";

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/v3/queries")
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
            + "\"resources\": [{\"id\": \"biobank:1\",\"collections\": [{\"id\": \"collection:1\"}]}]}";

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/v3/queries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id", is(1)))
        .andExpect(jsonPath("$.url", is("http://datasource.dev")))
        .andExpect(jsonPath("$.resources[0].id", is("biobank:1")))
        .andExpect(jsonPath("$.resources[0].collections[0].id", is("collection:1")))
        .andExpect(jsonPath("$.queryToken", is(not(emptyString()))));
  }
}
