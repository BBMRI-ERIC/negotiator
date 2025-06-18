package eu.bbmri_eric.negotiator.integration.api.v3;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import eu.bbmri_eric.negotiator.attachment.Attachment;
import eu.bbmri_eric.negotiator.attachment.AttachmentController;
import eu.bbmri_eric.negotiator.attachment.AttachmentRepository;
import eu.bbmri_eric.negotiator.user.Person;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import eu.bbmri_eric.negotiator.util.WithMockNegotiatorUser;
import jakarta.transaction.Transactional;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;

@IntegrationTest(loadTestData = true)
@AutoConfigureMockMvc
public class AttachmentControllerTests {

  private static final String WITH_NEGOTIATIONS_ENDPOINT =
      "/v3/negotiations/negotiation-1/attachments";
  private static final String WITHOUT_NEGOTIATIONS_ENDPOINT = "/v3/attachments";
  @Autowired private MockMvc mockMvc;
  @Autowired private WebApplicationContext context;
  @Autowired private ModelMapper modelMapper;
  @Autowired private AttachmentController controller;
  @Autowired private AttachmentRepository repository;
  @Autowired private PersonRepository personRepository;

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
  @WithUserDetails("TheResearcher")
  @Transactional
  public void test_DeleteAttachment_Ok() throws Exception {
    // First, create an attachment to delete
    byte[] data = "Hello, World!".getBytes();
    String fileName = "text.txt";
    MockMultipartFile file =
        new MockMultipartFile("file", fileName, MediaType.APPLICATION_OCTET_STREAM_VALUE, data);

    MvcResult creationResult =
        mockMvc
            .perform(multipart(WITHOUT_NEGOTIATIONS_ENDPOINT).file(file))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isString())
            .andReturn();

    String attachmentId = JsonPath.read(creationResult.getResponse().getContentAsString(), "$.id");
    assertTrue(repository.findById(attachmentId).isPresent());

    // Now delete the attachment
    mockMvc.perform(delete("/v3/attachments/{id}", attachmentId)).andExpect(status().isNoContent());

    // Assert the attachment is no longer in the repository
    assertFalse(repository.findById(attachmentId).isPresent());
  }

  @Test
  @WithUserDetails("TheResearcher")
  @Transactional
  public void test_DeleteNegotiationAttachment_Ok() throws Exception {
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
    mockMvc.perform(delete("/v3/attachments/{id}", attachmentId)).andExpect(status().isNoContent());
    assertFalse(repository.findById(attachmentId).isPresent());
  }

  @Test
  @WithUserDetails("TheResearcher")
  @Transactional
  public void test_DeleteAttachment_CreatedByAnotherUser_Forbidden() throws Exception {
    // Simulate another user who creates the attachment
    Person otherUser = personRepository.findByName("TheBiobanker").orElseThrow();

    Attachment attachment = Attachment.builder()
            .name("secret.txt")
            .payload("Top secret data".getBytes())
            .size(16L)
            .contentType(MediaType.TEXT_PLAIN_VALUE)
            .build();
    attachment.setCreatedBy(otherUser); // inherited from AuditEntity

    repository.saveAndFlush(attachment);
    String attachmentId = attachment.getId();
    assertNotNull(attachmentId);
    mockMvc
            .perform(delete("/attachments/{id}", attachmentId))
            .andExpect(status().isForbidden()); // or .isNotFound() if access is hidden
    assertTrue(repository.findById(attachmentId).isPresent());
  }

  @Test
  @WithUserDetails("TheResearcher")
  @Transactional
  public void test_DeleteFakeAttachment_404() throws Exception {
    mockMvc.perform(delete("/v3/attachments/non-existent")).andExpect(status().isNotFound());
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

  @Test
  @WithUserDetails("researcher")
  void createAttachmentForNegotiation_notPartOfNegotiation_forbidden() throws Exception {
    byte[] data = "Hello, World!".getBytes();
    String fileName = "text.txt";
    MockMultipartFile file =
        new MockMultipartFile("file", fileName, MediaType.APPLICATION_OCTET_STREAM_VALUE, data);

    mockMvc
        .perform(multipart(WITH_NEGOTIATIONS_ENDPOINT).file(file))
        .andExpect(status().isForbidden());
  }

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

  @Test
  @WithUserDetails("TheResearcher")
  @Transactional
  public void test_CreateWithExeFile_ShouldReject() throws Exception {
    byte[] data = new byte[] {0x4D, 0x5A, (byte) 0x90, 0x00};
    String fileName = "malicious_file.exe";
    MockMultipartFile file =
        new MockMultipartFile("file", fileName, MediaType.APPLICATION_OCTET_STREAM_VALUE, data);
    mockMvc
        .perform(multipart(WITHOUT_NEGOTIATIONS_ENDPOINT).file(file))
        .andDo(print())
        .andExpect(status().isBadRequest()) // Expecting bad request if validation fails
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.detail", containsString("not supported")));
    fileName = "malicious_file.exe.pdf";
    file = new MockMultipartFile("file", fileName, MediaType.APPLICATION_OCTET_STREAM_VALUE, data);
    mockMvc
        .perform(multipart(WITH_NEGOTIATIONS_ENDPOINT).file(file))
        .andDo(print())
        .andExpect(status().isBadRequest()) // Expecting bad request if validation fails
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.detail", containsString("not supported")));
  }

  @Test
  @WithMockNegotiatorUser(authorities = "ROLE_ADMIN", id = 101L)
  @Transactional
  void uploadAttachmentForNegotiation_asAdmin_ok() throws Exception {
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
    assertEquals("USER", attachment.get().getCreatedBy().getName());
  }
}
