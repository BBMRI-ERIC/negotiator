package eu.bbmri.eric.csit.service.negotiator.integration.api.v3;

import static org.hamcrest.core.Is.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import eu.bbmri.eric.csit.service.negotiator.NegotiatorApplication;
import eu.bbmri.eric.csit.service.negotiator.api.controller.v3.AttachmentController;
import eu.bbmri.eric.csit.service.negotiator.database.repository.AttachmentRepository;
import lombok.extern.apachecommons.CommonsLog;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(classes = NegotiatorApplication.class)
@ActiveProfiles("test")
@CommonsLog
public class AttachmentControllerTests {

  private static final String ENDPOINT = "/v3/attachments";
  private MockMvc mockMvc;
  @Autowired private WebApplicationContext context;
  @Autowired private ModelMapper modelMapper;
  @Autowired private AttachmentController controller;
  @Autowired private AttachmentRepository repository;

  @BeforeEach
  public void before() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @AfterEach
  public void after() {
    repository.deleteAll();
  }

  @Test
  @WithUserDetails("TheResearcher")
  public void test_Create_Ok() throws Exception {
    byte[] data = "Hello, World!".getBytes();
    String fileName = "text.txt";
    MockMultipartFile file =
        new MockMultipartFile("file", fileName, MediaType.APPLICATION_OCTET_STREAM_VALUE, data);

    mockMvc
        .perform(multipart(ENDPOINT).file(file))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").isString())
        .andExpect(jsonPath("$.name", is(fileName)))
        .andExpect(jsonPath("$.contentType", is(MediaType.APPLICATION_OCTET_STREAM_VALUE)))
        .andExpect(jsonPath("$.size", is(data.length)));
  }

  @Test
  public void testCreate_IsUnauthorized_whenNoAuth() throws Exception {
    byte[] data = "Hello, World!".getBytes();
    String fileName = "text.txt";
    MockMultipartFile file =
        new MockMultipartFile("file", fileName, MediaType.APPLICATION_OCTET_STREAM_VALUE, data);

    mockMvc
        .perform(multipart(ENDPOINT).file(file).with(anonymous()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  public void testCreate_IsUnauthorized_whenBasicAuth() throws Exception {
    byte[] data = "Hello, World!".getBytes();
    String fileName = "text.txt";
    MockMultipartFile file =
        new MockMultipartFile("file", fileName, MediaType.APPLICATION_OCTET_STREAM_VALUE, data);

    mockMvc
        .perform(multipart(ENDPOINT).file(file).with(httpBasic("researcher", "wrong_pass")))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithUserDetails("TheResearcher")
  public void testGetById_Ok() throws Exception {
    byte[] data = "Hello, World!".getBytes();
    String fileName = "text.txt";
    MockMultipartFile file =
        new MockMultipartFile("file", fileName, MediaType.APPLICATION_OCTET_STREAM_VALUE, data);

    MvcResult result =
        mockMvc.perform(multipart(ENDPOINT).file(file)).andExpect(status().isCreated()).andReturn();

    String id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    mockMvc
        .perform(get(String.format("%s/%s", ENDPOINT, id)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM_VALUE))
        .andExpect(content().bytes(data));
  }

  @Test
  @WithUserDetails("TheResearcher")
  public void testGetById_NotFound() throws Exception {
    mockMvc.perform(get(String.format("%s/unknown", ENDPOINT))).andExpect(status().isNotFound());
  }

  @Test
  public void testGetById_IsUnauthorized_whenNoAuth() throws Exception {
    mockMvc
        .perform(get(String.format("%s/1", ENDPOINT)).with(anonymous()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  public void testGetById_IsUnauthorized_whenBasicAuth() throws Exception {
    mockMvc
        .perform(get(String.format("%s/1", ENDPOINT)).with(httpBasic("researcher", "wrong_pass")))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithUserDetails("TheResearcher")
  public void testGetList_Ok() throws Exception {
    byte[] data = "Hello, World!".getBytes();
    String fileName = "text.txt";
    MockMultipartFile file =
        new MockMultipartFile("file", fileName, MediaType.APPLICATION_OCTET_STREAM_VALUE, data);

    MvcResult result =
        mockMvc.perform(multipart(ENDPOINT).file(file)).andExpect(status().isCreated()).andReturn();

    String id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");

    mockMvc
        .perform(get(String.format("%s", ENDPOINT)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$[0].id", is(id)))
        .andExpect(jsonPath("$[0].name", is(fileName)))
        .andExpect(jsonPath("$[0].contentType", is(MediaType.APPLICATION_OCTET_STREAM_VALUE)))
        .andExpect(jsonPath("$[0].size", is(data.length)));
  }

  @Test
  public void testGetList_IsUnauthorized_whenNoAuth() throws Exception {
    mockMvc
        .perform(get(String.format("%s", ENDPOINT)).with(anonymous()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  public void testGetList_IsUnauthorized_whenBasicAuth() throws Exception {
    mockMvc
        .perform(get(String.format("%s", ENDPOINT)).with(httpBasic("researcher", "wrong_pass")))
        .andExpect(status().isUnauthorized());
  }
}
