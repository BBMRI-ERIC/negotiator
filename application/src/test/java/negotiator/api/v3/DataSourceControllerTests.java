package negotiator.api.v3;

import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri.eric.csit.service.model.DataSource.ApiType;
import eu.bbmri.eric.csit.service.negotiator.NegotiatorApplication;
import eu.bbmri.eric.csit.service.negotiator.api.v3.DataSourceController;
import eu.bbmri.eric.csit.service.negotiator.dto.request.DataSourceRequest;
import eu.bbmri.eric.csit.service.repository.DataSourceRepository;
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
public class DataSourceControllerTests {

  private MockMvc mockMvc;

  @Autowired private DataSourceController controller;
  @Autowired private DataSourceRepository repository;

  private static final String NAME = "Data Source";
  private static final String DESCRIPTION = "This is a data source";
  private static final String URL = "http://datasource.test";
  private static final String API_URL = "http://datasource.test/api";
  private static final String API_USERNAME = "test";
  private static final String API_PASSWORD = "test";
  private static final String RESOURCE_NETWORK = "resource_networks";
  private static final String RESOURCE_BIOBANK = "resource_biobanks";
  private static final String RESOURCE_COLLECTION = "resource_collections";
  private static final boolean SYNC_ACTIVE = false;

  @BeforeEach
  public void before() {
    mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
  }

  private DataSourceRequest createRequest() {
    return DataSourceRequest.builder()
        .name(NAME)
        .description(DESCRIPTION)
        .url(URL)
        .apiType(ApiType.MOLGENIS)
        .apiUsername(API_USERNAME)
        .apiPassword(API_PASSWORD)
        .apiUrl(API_URL)
        .resourceNetwork(RESOURCE_NETWORK)
        .resourceBiobank(RESOURCE_BIOBANK)
        .resourceCollection(RESOURCE_COLLECTION)
        .syncActive(false)
        .sourcePrefix(false)
        .build();
  }

  private String jsonFromRequest(DataSourceRequest request) throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    return mapper.writeValueAsString(request);
  }

  private void checkBadRequest(String requestBody) throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/v3/data-sources")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isBadRequest());
    assertEquals(repository.findAll().size(), 1);
  }

  private void checkBadRequest(DataSourceRequest request) throws Exception {
    String requestBody = jsonFromRequest(request);
    checkBadRequest(requestBody);
  }

  @Test
  public void testBadRequest_whenName_IsMissing() throws Exception {
    DataSourceRequest request = createRequest();
    request.setName(null);
    checkBadRequest(request);
  }

  @Test
  public void testBadRequest_whenDescription_IsMissing() throws Exception {
    DataSourceRequest request = createRequest();
    request.setDescription(null);
    checkBadRequest(request);
  }

  @Test
  public void testBadRequest_whenUrl_IsMissing() throws Exception {
    DataSourceRequest request = createRequest();
    request.setUrl(null);
    checkBadRequest(request);
  }

  @Test
  public void testBadRequest_whenApiType_IsMissing() throws Exception {
    DataSourceRequest request = createRequest();
    request.setApiType(null);
    checkBadRequest(request);
  }

  @Test
  public void testBadRequest_whenApiType_IsWrong() throws Exception {
    DataSourceRequest request = createRequest();
    String requestBody = jsonFromRequest(request);
    requestBody = requestBody.replace("MOLGENIS", "UNKNOWN");
    checkBadRequest(requestBody);
  }

  @Test
  public void testBadRequest_whenApiUrl_IsMissing() throws Exception {
    DataSourceRequest request = createRequest();
    request.setApiUrl(null);
    checkBadRequest(request);
  }

  @Test
  public void testBadRequest_whenApiUsername_IsMissing() throws Exception {
    DataSourceRequest request = createRequest();
    request.setApiUsername(null);
    checkBadRequest(request);
  }

  @Test
  public void testBadRequest_whenApiPassword_IsMissing() throws Exception {
    DataSourceRequest request = createRequest();
    request.setApiPassword(null);
    checkBadRequest(request);
  }

  @Test
  public void testBadRequest_whenResourceNetwork_IsMissing() throws Exception {
    DataSourceRequest request = createRequest();
    request.setResourceNetwork(null);
    checkBadRequest(request);
  }

  @Test
  public void testBadRequest_whenResourceBiobank_IsMissing() throws Exception {
    DataSourceRequest request = createRequest();
    request.setResourceBiobank(null);
    checkBadRequest(request);
  }

  @Test
  public void testBadRequest_whenResourceCollection_IsMissing() throws Exception {
    DataSourceRequest request = createRequest();
    request.setResourceCollection(null);
    checkBadRequest(request);
  }

  @Test
  public void testCreated_whenRequest_IsCorrect() throws Exception {
    DataSourceRequest request = createRequest();
    String requestBody = jsonFromRequest(request);

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/v3/data-sources")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id", is(2))) // 3 because it's the current autogenerated value
        .andExpect(jsonPath("$.name", is(NAME)))
        .andExpect(jsonPath("$.description", is(DESCRIPTION)))
        .andExpect(jsonPath("$.url", is(URL)));
    assertEquals(repository.findAll().size(), 2);
    repository.deleteById(2L);
  }
}
