package negotiator.api.v3;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.text.IsEmptyString.emptyString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri.eric.csit.service.negotiator.NegotiatorApplication;
import eu.bbmri.eric.csit.service.negotiator.api.v3.QueryController;
import eu.bbmri.eric.csit.service.negotiator.dto.request.QueryRequest;
import eu.bbmri.eric.csit.service.negotiator.dto.request.ResourceDTO;
import eu.bbmri.eric.csit.service.negotiator.service.QueryService;
import eu.bbmri.eric.csit.service.repository.QueryRepository;
import java.net.URI;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(classes = NegotiatorApplication.class)
@ActiveProfiles("test")
public class QueryControllerTests {

  private MockMvc mockMvc;
  @Autowired private WebApplicationContext context;
  @Autowired private QueryController controller;
  @Autowired public QueryService service;
  @Autowired public QueryRepository repository;

  private static final String URL = "http://datasource.dev";
  private static final String HUMAN_READABLE = "Query description";
  private static final String BIOBANK_ID = "biobank:1";
  private static final String BIOBANK_NAME = "Test Biobank";
  private static final String COLLECTION_ID = "collection:1";
  private static final String COLLECTION_NAME = "Test Collection";

  @BeforeEach
  public void before() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  private QueryRequest createRequest(boolean update) {
    String suffix = update ? "u" : "";
    ResourceDTO collection = ResourceDTO.builder().id(COLLECTION_ID).name(COLLECTION_NAME).build();
    ResourceDTO biobank =
        ResourceDTO.builder()
            .id(BIOBANK_ID)
            .name(BIOBANK_NAME)
            .children(Set.of(collection))
            .build();

    return QueryRequest.builder()
        .url(String.format("%s%s", URL, suffix))
        .humanReadable(String.format("%s%s", HUMAN_READABLE, suffix))
        .url(String.format("%s%s", URL, suffix))
        .resources(Set.of(biobank))
        .build();
  }

  private String jsonFromRequest(Object request) throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    return mapper.writeValueAsString(request);
  }

  private void checkErrorResponse(
      HttpMethod method, String requestBody, ResultMatcher statusMatcher, RequestPostProcessor auth)
      throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.request(method, URI.create("/v3/queries"))
                .with(auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(statusMatcher);
    assertEquals(repository.findAll().size(), 0);
  }

  private void checkErrorResponse(
      HttpMethod method,
      QueryRequest request,
      ResultMatcher statusMatcher,
      RequestPostProcessor auth)
      throws Exception {
    String requestBody = jsonFromRequest(request);
    checkErrorResponse(method, requestBody, statusMatcher, auth);
  }

  @Test
  public void testBadRequest_whenUrlFieldIsMissing() throws Exception {
    QueryRequest request = createRequest(false);
    request.setUrl(null);
    checkErrorResponse(
        HttpMethod.POST, request, status().isBadRequest(), httpBasic("directory", "directory"));
  }

  @Test
  public void testBadRequest_whenUrlHumanReadableFieldIsMissing() throws Exception {
    QueryRequest request = createRequest(false);
    request.setHumanReadable(null);
    checkErrorResponse(
        HttpMethod.POST, request, status().isBadRequest(), httpBasic("directory", "directory"));
  }

  @Test
  public void testBadRequest_whenResourcesFieldIsMissing() throws Exception {
    QueryRequest request = createRequest(false);
    request.setResources(null);
    checkErrorResponse(
        HttpMethod.POST, request, status().isBadRequest(), httpBasic("directory", "directory"));
  }

  @Test
  public void testBadRequest_whenResourcesFieldIsEmpty() throws Exception {
    QueryRequest request = createRequest(false);
    request.setResources(Set.of());
    checkErrorResponse(
        HttpMethod.POST, request, status().isBadRequest(), httpBasic("directory", "directory"));
  }

  @Test
  public void testBadRequest_whenCollectionNotFound() throws Exception {
    QueryRequest request = createRequest(false);
    Optional<ResourceDTO> biobank = request.getResources().stream().findFirst();
    assert biobank.isPresent();
    Optional<ResourceDTO> collection = biobank.get().getChildren().stream().findFirst();
    assert collection.isPresent();
    collection.get().setId("collection_unknown");
    checkErrorResponse(
        HttpMethod.POST, request, status().isBadRequest(), httpBasic("directory", "directory"));
  }

  @Test
  public void testBadRequest_whenCollectionAndBiobankMismatch() throws Exception {
    QueryRequest request = createRequest(false);
    Optional<ResourceDTO> biobank = request.getResources().stream().findFirst();
    assert biobank.isPresent();
    biobank.get().setId("wrong_biobank");
    checkErrorResponse(
        HttpMethod.POST, request, status().isBadRequest(), httpBasic("directory", "directory"));
  }

  @Test
  public void testBadRequest_whenDataSourceNotFound() throws Exception {
    QueryRequest request = createRequest(false);
    request.setUrl("http://wrong_data_source");
    checkErrorResponse(
        HttpMethod.POST, request, status().isBadRequest(), httpBasic("directory", "directory"));
  }

  @Test
  public void testCreated() throws Exception {
    QueryRequest request = createRequest(false);
    String requestBody = jsonFromRequest(request);

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/v3/queries")
                .with(httpBasic("directory", "directory"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.url", is("http://datasource.dev")))
        .andExpect(jsonPath("$.redirectUrl", containsString("http://localhost/researcher/query/")))
        .andExpect(jsonPath("$.resources[0].id", is("biobank:1")))
        .andExpect(jsonPath("$.resources[0].type", is("biobank")))
        .andExpect(jsonPath("$.resources[0].children[0].id", is("collection:1")))
        .andExpect(jsonPath("$.resources[0].children[0].type", is("collection")));
    assertEquals(repository.findAll().size(), 1);
    repository.deleteById(1L);
  }
}
