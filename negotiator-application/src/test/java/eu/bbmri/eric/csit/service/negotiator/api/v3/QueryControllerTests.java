package eu.bbmri.eric.csit.service.negotiator.api.v3;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.bbmri.eric.csit.service.negotiator.NegotiatorApplication;
import eu.bbmri.eric.csit.service.negotiator.dto.request.QueryRequest;
import eu.bbmri.eric.csit.service.negotiator.dto.request.ResourceDTO;
import eu.bbmri.eric.csit.service.negotiator.repository.QueryRepository;
import eu.bbmri.eric.csit.service.negotiator.service.QueryService;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(classes = NegotiatorApplication.class)
@ActiveProfiles("test")
@TestMethodOrder(OrderAnnotation.class)
public class QueryControllerTests {
  private static final String ENDPOINT = "/v3/queries";
  @Autowired public QueryService service;
  @Autowired public QueryRepository repository;
  private MockMvc mockMvc;
  @Autowired private WebApplicationContext context;
  @Autowired private QueryController controller;

  @BeforeEach
  public void before() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @Test
  public void testBadRequest_whenUrlFieldIsMissing() throws Exception {
    QueryRequest request = TestUtils.createQueryRequest(false);
    request.setUrl(null);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        httpBasic("directory", "directory"),
        ENDPOINT);
  }

  @Test
  public void testBadRequest_whenUrlHumanReadableFieldIsMissing() throws Exception {
    QueryRequest request = TestUtils.createQueryRequest(false);
    request.setHumanReadable(null);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        httpBasic("directory", "directory"),
        ENDPOINT);
  }

  @Test
  public void testBadRequest_whenResourcesFieldIsMissing() throws Exception {
    QueryRequest request = TestUtils.createQueryRequest(false);
    request.setResources(null);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        httpBasic("directory", "directory"),
        ENDPOINT);
  }

  @Test
  public void testBadRequest_whenResourcesFieldIsEmpty() throws Exception {
    QueryRequest request = TestUtils.createQueryRequest(false);
    request.setResources(Set.of());
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        httpBasic("directory", "directory"),
        ENDPOINT);
  }

  @Test
  public void testBadRequest_whenCollectionNotFound() throws Exception {
    QueryRequest request = TestUtils.createQueryRequest(false);
    Optional<ResourceDTO> biobank = request.getResources().stream().findFirst();
    assert biobank.isPresent();
    Optional<ResourceDTO> collection = biobank.get().getChildren().stream().findFirst();
    assert collection.isPresent();
    collection.get().setId("collection_unknown");
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        httpBasic("directory", "directory"),
        ENDPOINT);
  }

  @Test
  public void testBadRequest_whenCollectionAndBiobankMismatch() throws Exception {
    QueryRequest request = TestUtils.createQueryRequest(false);
    Optional<ResourceDTO> biobank = request.getResources().stream().findFirst();
    assert biobank.isPresent();
    biobank.get().setId("wrong_biobank");
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        httpBasic("directory", "directory"),
        ENDPOINT);
  }

  @Test
  public void testBadRequest_whenDataSourceNotFound() throws Exception {
    QueryRequest request = TestUtils.createQueryRequest(false);
    request.setUrl("http://wrong_data_source");
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        httpBasic("directory", "directory"),
        ENDPOINT);
  }

  @Test
  @Order(1)
  public void testCreate_Ok() throws Exception {
    QueryRequest request = TestUtils.createQueryRequest(false);
    String requestBody = TestUtils.jsonFromRequest(request);

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
        .andExpect(jsonPath("$.resources[0].children[0].id", is("biobank:1:collection:1")))
        .andExpect(jsonPath("$.resources[0].children[0].type", is("collection")));
    assertEquals(repository.findAll().size(), 1);
    repository.deleteAll();
  }
}
