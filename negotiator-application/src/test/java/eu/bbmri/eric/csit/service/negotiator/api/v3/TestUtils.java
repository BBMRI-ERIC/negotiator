package eu.bbmri.eric.csit.service.negotiator.api.v3;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import eu.bbmri.eric.csit.service.negotiator.dto.request.DataSourceRequest;
import eu.bbmri.eric.csit.service.negotiator.dto.request.PerunUserRequest;
import eu.bbmri.eric.csit.service.negotiator.dto.request.ProjectRequest;
import eu.bbmri.eric.csit.service.negotiator.dto.request.QueryRequest;
import eu.bbmri.eric.csit.service.negotiator.dto.request.RequestRequest;
import eu.bbmri.eric.csit.service.negotiator.dto.request.ResourceDTO;
import eu.bbmri.eric.csit.service.negotiator.model.DataSource.ApiType;
import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

public class TestUtils {

  public static final String DATA_SOURCE_NAME = "Data Source";
  public static final String DATA_SOURCE_DESCRIPTION = "This is a data source";
  public static final String DATA_SOURCE_URL = "http://datasource.test";
  public static final String DATA_SOURCE_API_URL = "http://datasource.test/api";
  public static final String DATA_SOURCE_API_USERNAME = "test";
  public static final String DATA_SOURCE_API_PASSWORD = "test";
  public static final String DATA_SOURCE_RESOURCE_NETWORK = "resource_networks";
  public static final String DATA_SOURCE_RESOURCE_BIOBANK = "resource_biobanks";
  public static final String DATA_SOURCE_RESOURCE_COLLECTION = "resource_collections";
  public static final boolean DATA_SOURCE_SYNC_ACTIVE = false;

  public static final String QUERY_URL = "http://datasource.dev";
  public static final String QUERY_HUMAN_READABLE = "Query description";
  public static final String QUERY_BIOBANK_ID = "biobank:1";
  public static final String QUERY_BIOBANK_NAME = "Test Biobank";
  public static final String QUERY_COLLECTION_ID = "biobank:1:collection:1";
  public static final String QUERY_COLLECTION_NAME = "Test Collection";

  public static final String PROJECT_TITLE = "project title";
  public static final String PROJECT_DESCRIPTION = "project description";
  public static final String PROJECT_ETHICS_VOTE = "ethics vote";
  public static final String PROJECT_EXPECTED_END_DATE = "2022-04-13";
  public static final boolean PROJECT_EXPECTED_DATA_GENERATION = true;
  public static final boolean PROJECT_IS_TEST_PROJECT = true;

  public static final String REQUEST_TITLE = "request title";
  public static final String REQUEST_DESCRIPTION = "request description";

  public static final String PERUN_USER_ORGANIZATION = "perun user organization";
  public static final Integer PERUN_USER_ID = 1;
  public static final String PERUN_USER_DISPLAY_NAME = "display name";
  public static final String PERUN_USER_STATUS = "perun user status";
  public static final String PERUN_USER_MAIL = "perunusermail@mail.it";
  public static final String[] PERUN_USER_IDENTITIES = {
    "perun user identity 1", "perun user identity 2"
  };

  public static DataSourceRequest createDataSourceRequest(boolean update) {
    String suffix = update ? "u" : "";
    return DataSourceRequest.builder()
        .name(String.format("%s%s", DATA_SOURCE_NAME, suffix))
        .description(String.format("%s%s", DATA_SOURCE_DESCRIPTION, suffix))
        .url(String.format("%s%s", DATA_SOURCE_URL, suffix))
        .apiType(ApiType.MOLGENIS)
        .apiUsername(String.format("%s%s", DATA_SOURCE_API_USERNAME, suffix))
        .apiPassword(String.format("%s%s", DATA_SOURCE_API_PASSWORD, suffix))
        .apiUrl(String.format("%s%s", DATA_SOURCE_API_URL, suffix))
        .resourceNetwork(String.format("%s%s", DATA_SOURCE_RESOURCE_NETWORK, suffix))
        .resourceBiobank(String.format("%s%s", DATA_SOURCE_RESOURCE_BIOBANK, suffix))
        .resourceCollection(String.format("%s%s", DATA_SOURCE_RESOURCE_COLLECTION, suffix))
        .syncActive(update)
        .sourcePrefix(update)
        .build();
  }

  public static QueryRequest createQueryRequest(boolean update) {
    String suffix = update ? "u" : "";
    ResourceDTO collection =
        ResourceDTO.builder().id(QUERY_COLLECTION_ID).name(QUERY_COLLECTION_NAME).build();
    ResourceDTO biobank =
        ResourceDTO.builder()
            .id(QUERY_BIOBANK_ID)
            .name(QUERY_BIOBANK_NAME)
            .children(Set.of(collection))
            .build();

    return QueryRequest.builder()
        .humanReadable(String.format("%s%s", QUERY_HUMAN_READABLE, suffix))
        .url(String.format("%s%s", QUERY_URL, suffix))
        .resources(Set.of(biobank))
        .build();
  }

  public static ProjectRequest createProjectRequest(boolean update) {
    String suffix = update ? "u" : "";

    return ProjectRequest.builder()
        .title(String.format("%s%s", PROJECT_TITLE, suffix))
        .description(String.format("%s%s", PROJECT_DESCRIPTION, suffix))
        .ethicsVote(String.format("%s%s", PROJECT_ETHICS_VOTE, suffix))
        .expectedEndDate(LocalDate.parse(PROJECT_EXPECTED_END_DATE))
        .expectedDataGeneration(PROJECT_EXPECTED_DATA_GENERATION)
        .isTestProject(PROJECT_IS_TEST_PROJECT)
        .build();
  }

  public static List<PerunUserRequest> createPerunUserRequestList(boolean update, int size) {
    String suffix = update ? "u" : "";
    List<PerunUserRequest> perunUserRequestList = new ArrayList<>();

    for (int i = 0; i < size; i++) {
      PerunUserRequest request =
          PerunUserRequest.builder()
              .id(PERUN_USER_ID + i)
              .displayName(String.format("%s_%s", PERUN_USER_DISPLAY_NAME, i))
              .organization(String.format("%s_%s", PERUN_USER_ORGANIZATION, i))
              .status(String.format("%s_%s", PERUN_USER_STATUS, i))
              .mail(String.format("%s_%s", PERUN_USER_MAIL, i))
              .identities(PERUN_USER_IDENTITIES)
              .build();
      perunUserRequestList.add(request);
    }
    return perunUserRequestList;
  }

  public static RequestRequest createRequest(
      boolean update, boolean includeProject, Set<Long> queriesId) {
    String suffix = update ? "u" : "";

    RequestRequest.RequestRequestBuilder builder =
        RequestRequest.builder()
            .title(String.format("%s%s", REQUEST_TITLE, suffix))
            .description(String.format("%s%s", REQUEST_DESCRIPTION, suffix))
            .queries(queriesId);
    if (includeProject) {
      builder.project(TestUtils.createProjectRequest(update));
    }
    return builder.build();
  }

  public static String jsonFromRequest(Object request) throws JsonProcessingException {
    ObjectMapper mapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
    return mapper.writeValueAsString(request);
  }

  public static void checkErrorResponse(
      MockMvc mockMvc,
      HttpMethod method,
      Object request,
      ResultMatcher statusMatcher,
      RequestPostProcessor auth,
      String endpoint)
      throws Exception {
    String requestBody = TestUtils.jsonFromRequest(request);
    checkErrorResponse(mockMvc, method, requestBody, statusMatcher, auth, endpoint);
  }

  public static void checkErrorResponse(
      MockMvc mockMvc,
      HttpMethod method,
      String requestBody,
      ResultMatcher statusMatcher,
      RequestPostProcessor auth,
      String endpoint)
      throws Exception {

    mockMvc
        .perform(
            MockMvcRequestBuilders.request(method, URI.create(endpoint))
                .with(auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(statusMatcher);
    //    assertEquals(requestRepository.findAll().size(), 0);
  }

  public static void checkErrorResponse(
      MockMvc mockMvc,
      HttpMethod method,
      Object request,
      ResultMatcher statusMatcher,
      String token,
      String endpoint)
      throws Exception {
    String requestBody = TestUtils.jsonFromRequest(request);
    checkErrorResponse(mockMvc, method, requestBody, statusMatcher, token, endpoint);
  }

  public static void checkErrorResponse(
      MockMvc mockMvc,
      HttpMethod method,
      String requestBody,
      ResultMatcher statusMatcher,
      String token,
      String endpoint)
      throws Exception {

    mockMvc
        .perform(
            MockMvcRequestBuilders.request(method, URI.create(endpoint))
                .header("Authorization", "Bearer %s".formatted(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(statusMatcher);
    //    assertEquals(requestRepository.findAll().size(), 0);
  }
}
