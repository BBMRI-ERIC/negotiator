package eu.bbmri_eric.negotiator.unit.service;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import eu.bbmri_eric.negotiator.configuration.auth.NegotiatorUserDetails;
import eu.bbmri_eric.negotiator.database.model.Attachment;
import eu.bbmri_eric.negotiator.database.model.Negotiation;
import eu.bbmri_eric.negotiator.database.model.Person;
import eu.bbmri_eric.negotiator.database.repository.AttachmentRepository;
import eu.bbmri_eric.negotiator.database.repository.NegotiationRepository;
import eu.bbmri_eric.negotiator.dto.attachments.AttachmentDTO;
import eu.bbmri_eric.negotiator.dto.attachments.AttachmentMetadataDTO;
import eu.bbmri_eric.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.mappers.AttachmentMapper;
import eu.bbmri_eric.negotiator.service.DBAttachmentService;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.modelmapper.ModelMapper;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

public class DBAttachmentServiceTest {

  @Mock AttachmentRepository attachmentRepository;

  @Mock NegotiationRepository negotiationRepository;

  @Spy ModelMapper modelMapper = new ModelMapper();

  @InjectMocks DBAttachmentService service;

  @InjectMocks AttachmentMapper attachmentMapper;

  private AutoCloseable closeable;

  @BeforeEach
  void before() {
    closeable = MockitoAnnotations.openMocks(this);
    attachmentMapper.addMappings();
  }

  @AfterEach
  void after() throws Exception {
    closeable.close();
  }

  /** Tests a correct file upload. */
  @Test
  public void test_UploadFileForNegotiation_OK() {
    byte[] data = "Hello, World!".getBytes();
    String fileName = "text.txt";
    MockMultipartFile file =
        new MockMultipartFile("file", fileName, MediaType.APPLICATION_OCTET_STREAM_VALUE, data);

    Negotiation negotiation = Negotiation.builder().build();
    negotiation.setId("abcd");

    Attachment attachment =
        Attachment.builder()
            .name(fileName)
            .payload(data)
            .contentType(MediaType.APPLICATION_OCTET_STREAM_VALUE)
            .size((long) data.length)
            .build();

    when(attachmentRepository.save(argThat(f -> Objects.equals(f.getName(), fileName))))
        .thenReturn(attachment);
    when(negotiationRepository.findById("abcd")).thenReturn(Optional.of(negotiation));

    AttachmentMetadataDTO metadataDTO = service.createForNegotiation("abcd", file);
    Assertions.assertEquals(metadataDTO.getContentType(), MediaType.APPLICATION_OCTET_STREAM_VALUE);
    Assertions.assertEquals(metadataDTO.getName(), fileName);
    Assertions.assertEquals(metadataDTO.getSize(), data.length);
  }

  /**
   * Tests that, in case the Negotiation of the attachment is not found, the attachment is not
   * created and an appropriate Exception is thrown
   */
  @Test
  public void test_UploadFileForNegotiation_NegotiationNotFound() {
    byte[] data = "Hello, World!".getBytes();
    String fileName = "text.txt";
    MockMultipartFile file =
        new MockMultipartFile("file", fileName, MediaType.APPLICATION_OCTET_STREAM_VALUE, data);

    Attachment attachment =
        Attachment.builder()
            .name(fileName)
            .payload(data)
            .contentType(MediaType.APPLICATION_OCTET_STREAM_VALUE)
            .size((long) data.length)
            .build();

    when(attachmentRepository.save(argThat(f -> Objects.equals(f.getName(), fileName))))
        .thenReturn(attachment);
    when(negotiationRepository.findById("abcd")).thenReturn(Optional.empty());
    Assertions.assertThrows(
        EntityNotFoundException.class,
        () -> {
          service.createForNegotiation("abcd", file);
        });
  }

  /**
   * Tests that, in case an IOException is thrown, the attachment is not created and an appropriate
   * Exception is thrown
   */
  @Test
  public void test_UploadFile_FailWhenIOException() throws IOException {
    Negotiation negotiation = Negotiation.builder().build();
    negotiation.setId("abcd");

    MultipartFile file = mock(MultipartFile.class);
    when(file.getBytes()).thenThrow(IOException.class);

    Assertions.assertThrows(
        RuntimeException.class, () -> service.createForNegotiation("abcd", file));
  }

  /** Tests a correct file upload. */
  @Test
  public void test_UploadFileWithoutNegotiation_OK() {
    byte[] data = "Hello, World!".getBytes();
    String fileName = "text.txt";
    MockMultipartFile file =
        new MockMultipartFile("file", fileName, MediaType.APPLICATION_OCTET_STREAM_VALUE, data);

    Attachment attachment =
        Attachment.builder()
            .name(fileName)
            .payload(data)
            .contentType(MediaType.APPLICATION_OCTET_STREAM_VALUE)
            .size((long) data.length)
            .build();

    when(attachmentRepository.save(argThat(f -> Objects.equals(f.getName(), fileName))))
        .thenReturn(attachment);

    AttachmentMetadataDTO metadataDTO = service.create(file);
    Assertions.assertEquals(metadataDTO.getContentType(), MediaType.APPLICATION_OCTET_STREAM_VALUE);
    Assertions.assertEquals(metadataDTO.getName(), fileName);
    Assertions.assertEquals(metadataDTO.getSize(), data.length);
  }

  /** Tests a correct retrieval of the metadata of all the attachments belonging to a Negotiation */
  @Test
  public void test_GetAllForNegotiation_OK() {
    byte[] data = "Hello, World!".getBytes();
    String fileName = "text.txt";

    Attachment attachment =
        Attachment.builder()
            .name(fileName)
            .payload(data)
            .contentType(MediaType.APPLICATION_OCTET_STREAM_VALUE)
            .size((long) data.length)
            .build();

    when(attachmentRepository.findByNegotiation_Id("abcd")).thenReturn(List.of(attachment));

    List<AttachmentMetadataDTO> attachments = service.findByNegotiation("abcd");
    Assertions.assertEquals(
        attachments.get(0).getContentType(), MediaType.APPLICATION_OCTET_STREAM_VALUE);
    Assertions.assertEquals(attachments.get(0).getName(), fileName);
    Assertions.assertEquals(attachments.get(0).getSize(), data.length);
  }

  /**
   * Tests a correct retrieval of the metadata of all the attachments belonging to a Negotiation in
   * case of empty list
   */
  @Test
  public void test_GetAllForNegotiation_EmptyList() {
    when(attachmentRepository.findByNegotiation_Id("abcd")).thenReturn(Collections.emptyList());

    List<AttachmentMetadataDTO> attachments = service.findByNegotiation("abcd");
    Assertions.assertEquals(attachments.size(), 0);
  }

  /** Tests correct retrieval of an attachment's metadata by negotiation id and attachment id */
  @Test
  public void test_RetrieveByIdAndNegotiation_OK() {
    byte[] data = "Hello, World!".getBytes();
    String fileName = "text.txt";
    Attachment attachment =
        Attachment.builder()
            .name(fileName)
            .payload(data)
            .contentType(MediaType.APPLICATION_OCTET_STREAM_VALUE)
            .size((long) data.length)
            .build();

    when(attachmentRepository.findByIdAndNegotiation_Id("attachment-id", "negotiation-id"))
        .thenReturn(Optional.of(attachment));

    AttachmentMetadataDTO attachments =
        service.findByIdAndNegotiation("attachment-id", "negotiation-id");
    Assertions.assertEquals(attachments.getContentType(), MediaType.APPLICATION_OCTET_STREAM_VALUE);
    Assertions.assertEquals(attachments.getName(), fileName);
    Assertions.assertEquals(attachments.getSize(), data.length);
  }

  /**
   * Tests failure of attachment's retrieval in case one of negotiation id or attachment id is not
   * found
   */
  @Test
  public void test_RetrieveByNegotiationAndId_NotFound() {
    when(attachmentRepository.findByIdAndNegotiation_Id("attachment-id", "negotiation-id"))
        .thenReturn(Optional.empty());

    Assertions.assertThrows(
        EntityNotFoundException.class,
        () -> service.findByIdAndNegotiation("attachment-id", "negotiation-id"));
  }

  private void setAuthenticationContext(Person authenticatedUser) {
    NegotiatorUserDetails userDetails = mock(NegotiatorUserDetails.class);
    when(userDetails.getPerson()).thenReturn(authenticatedUser);
    Authentication authentication = mock(Authentication.class);
    when(authentication.getPrincipal()).thenReturn(userDetails);

    SecurityContext securityContext = mock(SecurityContext.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);
  }

  /**
   * Test correct download of an attachment when the creator of the attachment is authenticated user
   */
  @Test
  public void test_DownloadById_Ok_WhenCreatorIsTheAuthenticatedUser() {
    Person authenticatedUser = mock(Person.class);
    when(authenticatedUser.getId()).thenReturn(2L);
    setAuthenticationContext(authenticatedUser);

    byte[] data = "Hello, World!".getBytes();
    String fileName = "text.txt";
    Attachment attachment =
        Attachment.builder()
            .name(fileName)
            .payload(data)
            .contentType(MediaType.APPLICATION_OCTET_STREAM_VALUE)
            .size((long) data.length)
            .build();
    attachment.setCreatedBy(authenticatedUser);

    when(attachmentRepository.findById("attachment-id")).thenReturn(Optional.of(attachment));

    AttachmentDTO attachmentDTO = service.findById("attachment-id");
    Assertions.assertEquals(attachmentDTO.getName(), fileName);
    Assertions.assertEquals(
        attachmentDTO.getContentType(), MediaType.APPLICATION_OCTET_STREAM_VALUE);
    Assertions.assertEquals(attachmentDTO.getSize(), attachment.getSize());
    Assertions.assertEquals(Arrays.toString(attachmentDTO.getPayload()), Arrays.toString(data));
  }

  /**
   * Test correct download of an attachment when the creator of the negotiation is the authenticated
   * user even if he is different then the attachment's owner
   */
  @Test
  public void test_DownloadById_Ok_WhenNegotiationCreatorIsAuthenticatedUser() {
    Person authenticatedUser = mock(Person.class);
    when(authenticatedUser.getId()).thenReturn(2L);
    setAuthenticationContext(authenticatedUser);

    Person attachmentOwner = mock(Person.class);
    when(attachmentOwner.getId()).thenReturn(1L);
    setAuthenticationContext(attachmentOwner);

    Negotiation negotiation = mock(Negotiation.class);
    when(negotiation.getCreatedBy()).thenReturn(authenticatedUser);

    byte[] data = "Hello, World!".getBytes();
    String fileName = "text.txt";
    Attachment attachment =
        Attachment.builder()
            .name(fileName)
            .payload(data)
            .contentType(MediaType.APPLICATION_OCTET_STREAM_VALUE)
            .size((long) data.length)
            .negotiation(negotiation)
            .build();
    attachment.setCreatedBy(attachmentOwner);

    when(attachmentRepository.findById("attachment-id")).thenReturn(Optional.of(attachment));

    AttachmentDTO attachmentDTO = service.findById("attachment-id");
    Assertions.assertEquals(attachmentDTO.getName(), fileName);
    Assertions.assertEquals(
        attachmentDTO.getContentType(), MediaType.APPLICATION_OCTET_STREAM_VALUE);
    Assertions.assertEquals(attachmentDTO.getSize(), attachment.getSize());
    Assertions.assertEquals(Arrays.toString(attachmentDTO.getPayload()), Arrays.toString(data));
  }

  /**
   * Test that, if the creator of the attachment is not the authenticated person, it fails with
   * forbidden
   */
  @Test
  public void test_DownloadById_Forbidden() {
    Person authenticatedUser = mock(Person.class);
    when(authenticatedUser.getId()).thenReturn(2L);
    setAuthenticationContext(authenticatedUser);

    Person owner = mock(Person.class);
    when(owner.getId()).thenReturn(1L);

    byte[] data = "Hello, World!".getBytes();
    String fileName = "text.txt";
    Attachment attachment =
        Attachment.builder()
            .name(fileName)
            .payload(data)
            .contentType(MediaType.APPLICATION_OCTET_STREAM_VALUE)
            .size((long) data.length)
            .build();
    attachment.setCreatedBy(owner);

    when(attachmentRepository.findById("attachment-id")).thenReturn(Optional.of(attachment));
    Assertions.assertThrows(ResponseStatusException.class, () -> service.findById("attachment-id"));
  }

  @Test
  public void test_DownloadById_NotFound() {
    when(attachmentRepository.findById("attachment-id")).thenReturn(Optional.empty());
    Assertions.assertThrows(EntityNotFoundException.class, () -> service.findById("attachment-id"));
  }

  /**
   * Tests that, when the provided file id is not found, the service propagates the exception raised
   * by the repository
   */
  @Test
  public void test_findById_NotFound() {
    when(attachmentRepository.findById("abcd")).thenReturn(Optional.empty());
    Assert.assertThrows(EntityNotFoundException.class, () -> service.findById("abcd"));
  }
}
