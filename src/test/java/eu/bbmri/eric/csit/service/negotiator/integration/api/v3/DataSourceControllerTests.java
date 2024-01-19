package eu.bbmri.eric.csit.service.negotiator.integration.api.v3;

import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import eu.bbmri.eric.csit.service.negotiator.NegotiatorApplication;
import eu.bbmri.eric.csit.service.negotiator.api.controller.v3.DataSourceController;
import eu.bbmri.eric.csit.service.negotiator.database.model.DataSource;
import eu.bbmri.eric.csit.service.negotiator.database.repository.DataSourceRepository;
import eu.bbmri.eric.csit.service.negotiator.dto.datasource.DataSourceCreateDTO;
import jakarta.transaction.Transactional;
import java.net.URI;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
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
  @WithUserDetails("admin")
  public void testCreate_BadRequest_whenName_IsMissing() throws Exception {
    DataSourceCreateDTO request = TestUtils.createDataSourceRequest(false);
    request.setName(null);
    String requestBody = TestUtils.jsonFromRequest(request);

    mockMvc
        .perform(
            MockMvcRequestBuilders.post(URI.create(ENDPOINT))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithUserDetails("admin")
  public void testCreate_BadRequest_whenDescription_IsMissing() throws Exception {
    DataSourceCreateDTO request = TestUtils.createDataSourceRequest(false);
    request.setDescription(null);
    String requestBody = TestUtils.jsonFromRequest(request);

    mockMvc
        .perform(
            MockMvcRequestBuilders.post(URI.create(ENDPOINT))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithUserDetails("admin")
  public void testCreate_BadRequest_whenUrl_IsMissing() throws Exception {
    DataSourceCreateDTO request = TestUtils.createDataSourceRequest(false);
    request.setUrl(null);
    String requestBody = TestUtils.jsonFromRequest(request);

    mockMvc
        .perform(
            MockMvcRequestBuilders.post(URI.create(ENDPOINT))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithUserDetails("admin")
  public void testCreate_BadRequest_whenApiType_IsMissing() throws Exception {
    DataSourceCreateDTO request = TestUtils.createDataSourceRequest(false);
    request.setApiType(null);
    String requestBody = TestUtils.jsonFromRequest(request);

    mockMvc
        .perform(
            MockMvcRequestBuilders.post(URI.create(ENDPOINT))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithUserDetails("admin")
  public void testCreate_BadRequest_whenApiType_IsWrong() throws Exception {
    DataSourceCreateDTO request = TestUtils.createDataSourceRequest(false);
    String requestBody = TestUtils.jsonFromRequest(request);
    requestBody = requestBody.replace("MOLGENIS", "UNKNOWN");

    mockMvc
        .perform(
            MockMvcRequestBuilders.post(URI.create(ENDPOINT))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithUserDetails("admin")
  public void testCreate_BadRequest_whenApiUrl_IsMissing() throws Exception {
    DataSourceCreateDTO request = TestUtils.createDataSourceRequest(false);
    request.setApiUrl(null);
    String requestBody = TestUtils.jsonFromRequest(request);

    mockMvc
        .perform(
            MockMvcRequestBuilders.post(URI.create(ENDPOINT))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithUserDetails("admin")
  public void testCreate_BadRequest_whenApiUsername_IsMissing() throws Exception {
    DataSourceCreateDTO request = TestUtils.createDataSourceRequest(false);
    request.setApiUsername(null);
    String requestBody = TestUtils.jsonFromRequest(request);

    mockMvc
        .perform(
            MockMvcRequestBuilders.post(URI.create(ENDPOINT))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithUserDetails("admin")
  public void testCreate_BadRequest_whenApiPassword_IsMissing() throws Exception {
    DataSourceCreateDTO request = TestUtils.createDataSourceRequest(false);
    request.setApiPassword(null);
    String requestBody = TestUtils.jsonFromRequest(request);

    mockMvc
        .perform(
            MockMvcRequestBuilders.post(URI.create(ENDPOINT))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithUserDetails("admin")
  public void testCreate_BadRequest_whenResourceNetwork_IsMissing() throws Exception {
    DataSourceCreateDTO request = TestUtils.createDataSourceRequest(false);
    request.setResourceNetwork(null);
    String requestBody = TestUtils.jsonFromRequest(request);

    mockMvc
        .perform(
            MockMvcRequestBuilders.post(URI.create(ENDPOINT))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithUserDetails("admin")
  public void testCreate_BadRequest_whenResourceBiobank_IsMissing() throws Exception {
    DataSourceCreateDTO request = TestUtils.createDataSourceRequest(false);
    request.setResourceBiobank(null);
    String requestBody = TestUtils.jsonFromRequest(request);

    mockMvc
        .perform(
            MockMvcRequestBuilders.post(URI.create(ENDPOINT))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithUserDetails("admin")
  public void testCreate_BadRequest_whenResourceCollection_IsMissing() throws Exception {
    DataSourceCreateDTO request = TestUtils.createDataSourceRequest(false);
    request.setResourceCollection(null);
    String requestBody = TestUtils.jsonFromRequest(request);

    mockMvc
        .perform(
            MockMvcRequestBuilders.post(URI.create(ENDPOINT))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithUserDetails("admin")
  @Transactional
  public void testCreated_whenRequest_IsCorrect() throws Exception {
    DataSourceCreateDTO request = TestUtils.createDataSourceRequest(false);
    String requestBody = TestUtils.jsonFromRequest(request);

    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/v3/data-sources")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.name", is(TestUtils.DATA_SOURCE_NAME)))
            .andExpect(jsonPath("$.description", is(TestUtils.DATA_SOURCE_DESCRIPTION)))
            .andExpect(jsonPath("$.url", is(TestUtils.DATA_SOURCE_URL)))
            .andReturn();

    Integer dataSourceId =
        JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    Optional<DataSource> dataSource = repository.findById((long) dataSourceId);
    assert dataSource.isPresent();
    assertEquals(dataSource.get().getCreatedBy().getName(), "admin");

    assertEquals(repository.findAll().size(), 2);
    repository.deleteById((long) dataSourceId);
  }

  @Test
  public void testCreate_Unauthorized_whenNoAuth() throws Exception {
    DataSourceCreateDTO request = TestUtils.createDataSourceRequest(false);
    TestUtils.checkErrorResponse(
        mockMvc, HttpMethod.POST, request, status().isUnauthorized(), anonymous(), ENDPOINT);
  }

  @Test
  @Disabled // Disabled because without HTTP Basic authorization the case cannot happen
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
  @WithUserDetails("TheResearcher")
  public void testCreate_Forbidden_whenNoPermission() throws Exception {
    DataSourceCreateDTO request = TestUtils.createDataSourceRequest(false);
    String requestBody = TestUtils.jsonFromRequest(request);
    mockMvc
        .perform(
            MockMvcRequestBuilders.put(ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithUserDetails("admin")
  @Transactional
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
  @Disabled // Disabled because without HTTP Basic authorization the case cannot happen
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
  @WithUserDetails("TheResearcher")
  public void testUpdate_Forbidden_whenNoPermission() throws Exception {
    DataSourceCreateDTO request = TestUtils.createDataSourceRequest(false);
    String requestBody = TestUtils.jsonFromRequest(request);
    mockMvc
        .perform(
            MockMvcRequestBuilders.put(ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isForbidden());
  }
}
