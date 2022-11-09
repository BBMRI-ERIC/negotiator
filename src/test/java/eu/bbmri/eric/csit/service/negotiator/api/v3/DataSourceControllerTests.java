package eu.bbmri.eric.csit.service.negotiator.api.v3;

import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.bbmri.eric.csit.service.negotiator.NegotiatorApplication;
import eu.bbmri.eric.csit.service.negotiator.api.controller.v3.DataSourceController;
import eu.bbmri.eric.csit.service.negotiator.api.dto.datasource.DataSourceCreateDTO;
import eu.bbmri.eric.csit.service.negotiator.database.model.DataSource;
import eu.bbmri.eric.csit.service.negotiator.database.repository.DataSourceRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
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
public class DataSourceControllerTests {

  private static final String ENDPOINT = "/v3/data-sources";
  private MockMvc mockMvc;
  @Autowired private WebApplicationContext context;
  @Autowired private DataSourceController controller;
  @Autowired private DataSourceRepository repository;
  @Autowired private ModelMapper modelMapper;

  @BeforeEach
  public void before() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @Test
  public void testCreate_BadRequest_whenName_IsMissing() throws Exception {
    DataSourceCreateDTO request = TestUtils.createDataSourceRequest(false);
    request.setName(null);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        httpBasic("admin", "admin"),
        ENDPOINT);
  }

  @Test
  public void testCreate_BadRequest_whenDescription_IsMissing() throws Exception {
    DataSourceCreateDTO request = TestUtils.createDataSourceRequest(false);
    request.setDescription(null);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        httpBasic("admin", "admin"),
        ENDPOINT);
  }

  @Test
  public void testCreate_BadRequest_whenUrl_IsMissing() throws Exception {
    DataSourceCreateDTO request = TestUtils.createDataSourceRequest(false);
    request.setUrl(null);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        httpBasic("admin", "admin"),
        ENDPOINT);
  }

  @Test
  public void testCreate_BadRequest_whenApiType_IsMissing() throws Exception {
    DataSourceCreateDTO request = TestUtils.createDataSourceRequest(false);
    request.setApiType(null);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        httpBasic("admin", "admin"),
        ENDPOINT);
  }

  @Test
  public void testCreate_BadRequest_whenApiType_IsWrong() throws Exception {
    DataSourceCreateDTO request = TestUtils.createDataSourceRequest(false);
    String requestBody = TestUtils.jsonFromRequest(request);
    requestBody = requestBody.replace("MOLGENIS", "UNKNOWN");
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        requestBody,
        status().isBadRequest(),
        httpBasic("admin", "admin"),
        ENDPOINT);
  }

  @Test
  public void testCreate_BadRequest_whenApiUrl_IsMissing() throws Exception {
    DataSourceCreateDTO request = TestUtils.createDataSourceRequest(false);
    request.setApiUrl(null);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        httpBasic("admin", "admin"),
        ENDPOINT);
  }

  @Test
  public void testCreate_BadRequest_whenApiUsername_IsMissing() throws Exception {
    DataSourceCreateDTO request = TestUtils.createDataSourceRequest(false);
    request.setApiUsername(null);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        httpBasic("admin", "admin"),
        ENDPOINT);
  }

  @Test
  public void testCreate_BadRequest_whenApiPassword_IsMissing() throws Exception {
    DataSourceCreateDTO request = TestUtils.createDataSourceRequest(false);
    request.setApiPassword(null);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        httpBasic("admin", "admin"),
        ENDPOINT);
  }

  @Test
  public void testCreate_BadRequest_whenResourceNetwork_IsMissing() throws Exception {
    DataSourceCreateDTO request = TestUtils.createDataSourceRequest(false);
    request.setResourceNetwork(null);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        httpBasic("admin", "admin"),
        ENDPOINT);
  }

  @Test
  public void testCreate_BadRequest_whenResourceBiobank_IsMissing() throws Exception {
    DataSourceCreateDTO request = TestUtils.createDataSourceRequest(false);
    request.setResourceBiobank(null);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        httpBasic("admin", "admin"),
        ENDPOINT);
  }

  @Test
  public void testCreate_BadRequest_whenResourceCollection_IsMissing() throws Exception {
    DataSourceCreateDTO request = TestUtils.createDataSourceRequest(false);
    request.setResourceCollection(null);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        httpBasic("admin", "admin"),
        ENDPOINT);
  }

  @Test
  public void testCreated_whenRequest_IsCorrect() throws Exception {
    DataSourceCreateDTO request = TestUtils.createDataSourceRequest(false);
    String requestBody = TestUtils.jsonFromRequest(request);

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/v3/data-sources")
                .with(httpBasic("admin", "admin"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.name", is(TestUtils.DATA_SOURCE_NAME)))
        .andExpect(jsonPath("$.description", is(TestUtils.DATA_SOURCE_DESCRIPTION)))
        .andExpect(jsonPath("$.url", is(TestUtils.DATA_SOURCE_URL)));
    assertEquals(repository.findAll().size(), 2);

    repository.deleteById(2L);
  }

  @Test
  public void testCreate_Unauthorized_whenNoAuth() throws Exception {
    DataSourceCreateDTO request = TestUtils.createDataSourceRequest(false);
    TestUtils.checkErrorResponse(
        mockMvc, HttpMethod.POST, request, status().isUnauthorized(), anonymous(), ENDPOINT);
  }

  @Test
  public void testCreate_Unauthorized_whenWrongAuth() throws Exception {
    DataSourceCreateDTO request = TestUtils.createDataSourceRequest(false);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isUnauthorized(),
        httpBasic("admin", "wrong_pass"),
        ENDPOINT);
  }

  @Test
  public void testCreate_Forbidden_whenNoPermission() throws Exception {
    DataSourceCreateDTO request = TestUtils.createDataSourceRequest(false);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isForbidden(),
        httpBasic("researcher", "researcher"),
        ENDPOINT);
  }

  @Test
  public void testUpdate_whenIsCorrect() throws Exception {
    // The data source to be updated
    DataSource dataSourceEntity =
        modelMapper.map(TestUtils.createDataSourceRequest(false), DataSource.class);
    repository.save(dataSourceEntity);

    // Negotiation body with updated values
    DataSourceCreateDTO request = TestUtils.createDataSourceRequest(true);

    String requestBody = TestUtils.jsonFromRequest(request);
    mockMvc
        .perform(
            MockMvcRequestBuilders.put("/v3/data-sources/%s".formatted(dataSourceEntity.getId()))
                .with(httpBasic("admin", "admin"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isNoContent())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON));

    Optional<DataSource> updateDataSource = repository.findById(dataSourceEntity.getId());
    assert updateDataSource.isPresent();
    assertEquals(updateDataSource.get(), modelMapper.map(request, DataSource.class));

    repository.deleteById(dataSourceEntity.getId());
  }

  @Test
  public void testUpdate_Unauthorized_whenNoAuth() throws Exception {
    DataSourceCreateDTO request = TestUtils.createDataSourceRequest(false);
    TestUtils.checkErrorResponse(
        mockMvc, HttpMethod.PUT, request, status().isUnauthorized(), anonymous(), ENDPOINT);
  }

  @Test
  public void testUpdate_Unauthorized_whenWrongAuth() throws Exception {
    DataSourceCreateDTO request = TestUtils.createDataSourceRequest(false);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.PUT,
        request,
        status().isUnauthorized(),
        httpBasic("admin", "wrong_pass"),
        ENDPOINT);
  }

  @Test
  public void testUpdate_Forbidden_whenNoPermission() throws Exception {
    DataSourceCreateDTO request = TestUtils.createDataSourceRequest(false);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.PUT,
        request,
        status().isForbidden(),
        httpBasic("researcher", "researcher"),
        ENDPOINT);
  }
}
