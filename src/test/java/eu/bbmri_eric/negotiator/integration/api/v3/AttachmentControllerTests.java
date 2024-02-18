package eu.bbmri_eric.negotiator.integration.api.v3;

import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import eu.bbmri_eric.negotiator.NegotiatorApplication;
import eu.bbmri_eric.negotiator.api.controller.v3.AttachmentController;
import eu.bbmri_eric.negotiator.database.model.Attachment;
import eu.bbmri_eric.negotiator.database.repository.AttachmentRepository;
import jakarta.transaction.Transactional;
import java.util.Optional;
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

  private static final String WITH_NEGOTIATIONS_ENDPOINT =
      "/v3/negotiations/negotiation-1/attachments";
  private static final String WITHOUT_NEGOTIATIONS_ENDPOINT = "/v3/attachments";
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
  @Transactional
  public void test_CreateForNegotiation_Ok() throws Exception {
    byte[] data = "Hello, World!".getBytes();
    String fileName = "text.txt";
    MockMultipartFile file =
        new MockMultipartFile("file", fileName, MediaType.APPLICATION_OCTET_STREAM_VALUE, data);

    MvcResult result =
        mockMvc
            .perform(multipart(WITH_NEGOTIATIONS_ENDPOINT).file(file))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").isString())
            .andExpect(jsonPath("$.name", is(fileName)))
            .andExpect(jsonPath("$.contentType", is(MediaType.APPLICATION_OCTET_STREAM_VALUE)))
            .andExpect(jsonPath("$.size", is(data.length)))
            .andReturn();

    String attachmentId = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    Optional<Attachment> attachment = repository.findById(attachmentId);
    assert attachment.isPresent();
    assertEquals(attachment.get().getCreatedBy().getName(), "TheResearcher");
  }

  @Test
  public void testCreateWithoutNegotiation_IsUnauthorized_whenNoAuth() throws Exception {
    byte[] data = "Hello, World!".getBytes();
    String fileName = "text.txt";
    MockMultipartFile file =
        new MockMultipartFile("file", fileName, MediaType.APPLICATION_OCTET_STREAM_VALUE, data);

    mockMvc
        .perform(multipart(WITH_NEGOTIATIONS_ENDPOINT).file(file).with(anonymous()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  public void testCreateWithoutNegotiation_IsUnauthorized_whenBasicAuth() throws Exception {
    byte[] data = "Hello, World!".getBytes();
    String fileName = "text.txt";
    MockMultipartFile file =
        new MockMultipartFile("file", fileName, MediaType.APPLICATION_OCTET_STREAM_VALUE, data);

    mockMvc
        .perform(
            multipart(WITH_NEGOTIATIONS_ENDPOINT)
                .file(file)
                .with(httpBasic("researcher", "wrong_pass")))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithUserDetails("TheResearcher")
  @Transactional
  public void test_CreateWithoutNegotiation_Ok() throws Exception {
    byte[] data = "Hello, World!".getBytes();
    String fileName = "text.txt";
    MockMultipartFile file =
        new MockMultipartFile("file", fileName, MediaType.APPLICATION_OCTET_STREAM_VALUE, data);

    MvcResult result =
        mockMvc
            .perform(multipart(WITHOUT_NEGOTIATIONS_ENDPOINT).file(file))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").isString())
            .andExpect(jsonPath("$.name", is(fileName)))
            .andExpect(jsonPath("$.contentType", is(MediaType.APPLICATION_OCTET_STREAM_VALUE)))
            .andExpect(jsonPath("$.size", is(data.length)))
            .andReturn();

    String attachmentId = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    Optional<Attachment> attachment = repository.findById(attachmentId);
    assert attachment.isPresent();
    assertEquals(attachment.get().getCreatedBy().getName(), "TheResearcher");
  }

  @Test
  public void testCreateForNegotiation_IsUnauthorized_whenNoAuth() throws Exception {
    byte[] data = "Hello, World!".getBytes();
    String fileName = "text.txt";
    MockMultipartFile file =
        new MockMultipartFile("file", fileName, MediaType.APPLICATION_OCTET_STREAM_VALUE, data);

    mockMvc
        .perform(multipart(WITHOUT_NEGOTIATIONS_ENDPOINT).file(file).with(anonymous()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  public void testCreateForNegotiation_IsUnauthorized_whenBasicAuth() throws Exception {
    byte[] data = "Hello, World!".getBytes();
    String fileName = "text.txt";
    MockMultipartFile file =
        new MockMultipartFile("file", fileName, MediaType.APPLICATION_OCTET_STREAM_VALUE, data);

    mockMvc
        .perform(
            multipart(WITHOUT_NEGOTIATIONS_ENDPOINT)
                .file(file)
                .with(httpBasic("researcher", "wrong_pass")))
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
        mockMvc
            .perform(multipart(WITH_NEGOTIATIONS_ENDPOINT).file(file))
            .andExpect(status().isCreated())
            .andReturn();

    String id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    mockMvc
        .perform(get(String.format("%s/%s", WITH_NEGOTIATIONS_ENDPOINT, id)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").isString())
        .andExpect(jsonPath("$.name", is(fileName)))
        .andExpect(jsonPath("$.contentType", is(MediaType.APPLICATION_OCTET_STREAM_VALUE)))
        .andExpect(jsonPath("$.size", is((int) file.getSize())));
  }

  @Test
  @WithUserDetails("TheResearcher")
  public void testGetById_NotFound() throws Exception {
    mockMvc
        .perform(get(String.format("%s/unknown", WITH_NEGOTIATIONS_ENDPOINT)))
        .andDo(print())
        .andExpect(status().isNotFound());
  }

  @Test
  public void testGetById_IsUnauthorized_whenNoAuth() throws Exception {
    mockMvc
        .perform(get(String.format("%s/1", WITH_NEGOTIATIONS_ENDPOINT)).with(anonymous()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  public void testGetById_IsUnauthorized_whenBasicAuth() throws Exception {
    mockMvc
        .perform(
            get(String.format("%s/1", WITH_NEGOTIATIONS_ENDPOINT))
                .with(httpBasic("researcher", "wrong_pass")))
        .andExpect(status().isUnauthorized());
  }

  //  @Test
  //  @WithUserDetails("researcher")
  //  public void test_GetList_NoAttachmentsAreReturned() throws Exception {
  //    mockMvc
  //        .perform(MockMvcRequestBuilders.get(ENDPOINT))
  //        .andExpect(status().isOk())
  //        .andExpect(content().json("[]"));
  //  }

  @Test
  public void testGetList_IsUnauthorized_whenNoAuth() throws Exception {
    mockMvc
        .perform(get(String.format("%s", WITH_NEGOTIATIONS_ENDPOINT)).with(anonymous()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  public void testGetList_IsUnauthorized_whenBasicAuth() throws Exception {
    mockMvc
        .perform(
            get(String.format("%s", WITH_NEGOTIATIONS_ENDPOINT))
                .with(httpBasic("researcher", "wrong_pass")))
        .andExpect(status().isUnauthorized());
  }
}
