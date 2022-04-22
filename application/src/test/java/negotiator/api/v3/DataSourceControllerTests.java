package negotiator.api.v3;

import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri.eric.csit.service.model.DataSource;
import eu.bbmri.eric.csit.service.model.DataSource.ApiType;
import eu.bbmri.eric.csit.service.negotiator.NegotiatorApplication;
import eu.bbmri.eric.csit.service.negotiator.api.v3.DataSourceController;
import eu.bbmri.eric.csit.service.negotiator.dto.request.DataSourceRequest;
import eu.bbmri.eric.csit.service.repository.DataSourceRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
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
public class DataSourceControllerTests {

  private MockMvc mockMvc;
  @Autowired private WebApplicationContext context;
  @Autowired private DataSourceController controller;
  @Autowired private DataSourceRepository repository;
  @Autowired private ModelMapper modelMapper;

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
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  private DataSourceRequest createRequest(boolean update) {
    String suffix = update ? "u" : "";
    return DataSourceRequest.builder()
        .name(String.format("%s%s", NAME, suffix))
        .description(String.format("%s%s", DESCRIPTION, suffix))
        .url(String.format("%s%s", URL, suffix))
        .apiType(ApiType.MOLGENIS)
        .apiUsername(String.format("%s%s", API_USERNAME, suffix))
        .apiPassword(String.format("%s%s", API_PASSWORD, suffix))
        .apiUrl(String.format("%s%s", API_URL, suffix))
        .resourceNetwork(String.format("%s%s", RESOURCE_NETWORK, suffix))
        .resourceBiobank(String.format("%s%s", RESOURCE_BIOBANK, suffix))
        .resourceCollection(String.format("%s%s", RESOURCE_COLLECTION, suffix))
        .syncActive(update)
        .sourcePrefix(update)
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
    DataSourceRequest request = createRequest(false);
    request.setName(null);
    checkBadRequest(request);
  }

  @Test
  public void testBadRequest_whenDescription_IsMissing() throws Exception {
    DataSourceRequest request = createRequest(false);
    request.setDescription(null);
    checkBadRequest(request);
  }

  @Test
  public void testBadRequest_whenUrl_IsMissing() throws Exception {
    DataSourceRequest request = createRequest(false);
    request.setUrl(null);
    checkBadRequest(request);
  }

  @Test
  public void testBadRequest_whenApiType_IsMissing() throws Exception {
    DataSourceRequest request = createRequest(false);
    request.setApiType(null);
    checkBadRequest(request);
  }

  @Test
  public void testBadRequest_whenApiType_IsWrong() throws Exception {
    DataSourceRequest request = createRequest(false);
    String requestBody = jsonFromRequest(request);
    requestBody = requestBody.replace("MOLGENIS", "UNKNOWN");
    checkBadRequest(requestBody);
  }

  @Test
  public void testBadRequest_whenApiUrl_IsMissing() throws Exception {
    DataSourceRequest request = createRequest(false);
    request.setApiUrl(null);
    checkBadRequest(request);
  }

  @Test
  public void testBadRequest_whenApiUsername_IsMissing() throws Exception {
    DataSourceRequest request = createRequest(false);
    request.setApiUsername(null);
    checkBadRequest(request);
  }

  @Test
  public void testBadRequest_whenApiPassword_IsMissing() throws Exception {
    DataSourceRequest request = createRequest(false);
    request.setApiPassword(null);
    checkBadRequest(request);
  }

  @Test
  public void testBadRequest_whenResourceNetwork_IsMissing() throws Exception {
    DataSourceRequest request = createRequest(false);
    request.setResourceNetwork(null);
    checkBadRequest(request);
  }

  @Test
  public void testBadRequest_whenResourceBiobank_IsMissing() throws Exception {
    DataSourceRequest request = createRequest(false);
    request.setResourceBiobank(null);
    checkBadRequest(request);
  }

  @Test
  public void testBadRequest_whenResourceCollection_IsMissing() throws Exception {
    DataSourceRequest request = createRequest(false);
    request.setResourceCollection(null);
    checkBadRequest(request);
  }

  @Test
  public void testCreated_whenRequest_IsCorrect() throws Exception {
    DataSourceRequest request = createRequest(false);
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

  @Test
  public void testUpdate_whenIsCorrect() throws Exception {
    // The data source to be updated
    DataSource dataSourceEntity = modelMapper.map(createRequest(true), DataSource.class);
    repository.save(dataSourceEntity);

    // Request body with updated values
    DataSourceRequest request = createRequest(true);

    String requestBody = jsonFromRequest(request);
    mockMvc
        .perform(
            MockMvcRequestBuilders.put("/v3/data-sources/%s".formatted(dataSourceEntity.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isNoContent())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON));

    Optional<DataSource> updateDataSource = repository.findById(dataSourceEntity.getId());
    assert updateDataSource.isPresent();
    assertEquals(updateDataSource.get(), modelMapper.map(request, DataSource.class));

    repository.deleteById(dataSourceEntity.getId());
  }
}
