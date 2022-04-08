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
import eu.bbmri.eric.csit.service.negotiator.service.DataService;
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

  @Autowired public DataService dataService;

  @Autowired public QueryRepository queryRepository;

  @BeforeEach
  public void before() {
    mockMvc = MockMvcBuilders.standaloneSetup(queryController).build();
  }

  @Test
  public void testBadRequest_whenCollectionNotFound() throws Exception {
    String requestBody =
        "{\"url\": \"http://datasource.dev\", "
            + "\"humanReadable\": \"Test request\", "
            + "\"collections\": [{\"biobankId\": \"biobank:1\", \"collectionId\": \"unknownn\"}]}";

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
            + "\"collections\": [{\"biobankId\": \"wrong_biobank\", \"collectionId\": \"collection:1\"}]}";

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
            + "\"collections\": [{\"biobankId\": \"wrong_biobank\", \"collectionId\": \"collection:1\"}]}";

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
        "{\"url\":\"http://datasource.dev\","
            + "\"humanReadable\":\"Test request\","
            + "\"collections\":[{\"biobankId\":\"biobank:1\",\"collectionId\":\"collection:1\"}]}";

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/v3/queries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.jsonPayload", is(requestBody)))
        .andExpect(jsonPath("$.dataSource.url", is("http://datasource.dev")))
        .andExpect(jsonPath("$.collections[0].sourceId", is("collection:1")))
        .andExpect(jsonPath("$.biobanks[0].sourceId", is("biobank:1")))
        .andExpect(jsonPath("$.request", nullValue()))
        .andExpect(jsonPath("$.queryToken", is(not(emptyString()))))
        .andExpect(jsonPath("$.biobanks[0].sourceId", is("biobank:1")));
  }
}
