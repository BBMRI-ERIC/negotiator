package eu.bbmri.eric.csit.service.negotiator.unit.service;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import eu.bbmri.eric.csit.service.negotiator.configuration.auth.NegotiatorUserDetails;
import eu.bbmri.eric.csit.service.negotiator.database.model.Attachment;
import eu.bbmri.eric.csit.service.negotiator.database.model.Negotiation;
import eu.bbmri.eric.csit.service.negotiator.database.model.Person;
import eu.bbmri.eric.csit.service.negotiator.database.repository.AttachmentRepository;
import eu.bbmri.eric.csit.service.negotiator.database.repository.NegotiationRepository;
import eu.bbmri.eric.csit.service.negotiator.dto.attachments.AttachmentDTO;
import eu.bbmri.eric.csit.service.negotiator.dto.attachments.AttachmentMetadataDTO;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri.eric.csit.service.negotiator.mappers.AttachmentMapper;
import eu.bbmri.eric.csit.service.negotiator.service.DBAttachmentService;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.modelmapper.ModelMapper;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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

  @Test
  public void test_UploadFile_OK() {
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

    AttachmentMetadataDTO metadataDTO = service.create("abcd", file);
    Assertions.assertEquals(metadataDTO.getContentType(), MediaType.APPLICATION_OCTET_STREAM_VALUE);
    Assertions.assertEquals(metadataDTO.getName(), fileName);
    Assertions.assertEquals(metadataDTO.getSize(), data.length);
  }

  @Test
  public void test_UploadFile_NegotiationNotFound() {
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
          service.create("abcd", file);
        });
  }

  @Test
  public void test_UploadFile_FailWhenIOException() throws IOException {
    Negotiation negotiation = Negotiation.builder().build();
    negotiation.setId("abcd");

    MultipartFile file = mock(MultipartFile.class);
    when(file.getBytes()).thenThrow(IOException.class);

    Assertions.assertThrows(RuntimeException.class, () -> service.create("abcd", file));
  }

  @Test
  public void test_GetAllForNegotiation_OK() {
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

    when(attachmentRepository.findByNegotiation_Id("abcd")).thenReturn(List.of(attachment));

    List<AttachmentMetadataDTO> attachments = service.findByNegotiation("abcd");
    Assertions.assertEquals(
        attachments.get(0).getContentType(), MediaType.APPLICATION_OCTET_STREAM_VALUE);
    Assertions.assertEquals(attachments.get(0).getName(), fileName);
    Assertions.assertEquals(attachments.get(0).getSize(), data.length);
  }

  @Test
  public void test_GetAllForNegotiation_EmptyList() {
    when(attachmentRepository.findByNegotiation_Id("abcd")).thenReturn(Collections.emptyList());

    List<AttachmentMetadataDTO> attachments = service.findByNegotiation("abcd");
    Assertions.assertEquals(attachments.size(), 0);
  }

  @Test
  public void test_RetrieveByNegotiationAndId_OK() {
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

  @Test
  public void test_RetrieveByNegotiationAndId_NotFound() {
    when(attachmentRepository.findByIdAndNegotiation_Id("attachment-id", "negotiation-id"))
        .thenReturn(Optional.empty());

    Assertions.assertThrows(
        EntityNotFoundException.class,
        () -> service.findByIdAndNegotiation("attachment-id", "negotiation-id"));
  }

  private void setAuthenticationContext(
      Person authenticatedUser, @Nullable Collection<SimpleGrantedAuthority> authorities) {
    NegotiatorUserDetails userDetails = Mockito.mock(NegotiatorUserDetails.class);
    when(userDetails.getPerson()).thenReturn(authenticatedUser);
    Authentication authentication = Mockito.mock(Authentication.class);
    when(authentication.getPrincipal()).thenReturn(userDetails);

    SecurityContext securityContext = Mockito.mock(SecurityContext.class);
    Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);
  }

  @Test
  public void test_DownloadById_Ok_WhenCreatorIsTheAuthenticatedUser() {
    Person authenticatedUser = mock(Person.class);
    when(authenticatedUser.getId()).thenReturn(2L);
    setAuthenticationContext(authenticatedUser, null);

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

  //  @Test
  //  public void test_DownloadById_Ok_WhenAuthenticatedUserIsAdmin() {
  //    Person authenticatedUser = mock(Person.class);
  //    when(authenticatedUser.getId()).thenReturn(2L);
  //    Collection<SimpleGrantedAuthority> authorities = Set.of(new
  // SimpleGrantedAuthority("ADMIN"));
  //    setAuthenticationContext(authenticatedUser, authorities);
  //
  //    Person owner = mock(Person.class);
  //    when(owner.getId()).thenReturn(1L);
  //    byte[] data = "Hello, World!".getBytes();
  //    String fileName = "text.txt";
  //    Attachment attachment =
  //        Attachment.builder()
  //            .name(fileName)
  //            .payload(data)
  //            .contentType(MediaType.APPLICATION_OCTET_STREAM_VALUE)
  //            .size((long) data.length)
  //            .build();
  //    attachment.setCreatedBy(owner);
  //
  //    when(attachmentRepository.findById("attachment-id")).thenReturn(Optional.of(attachment));
  //
  //    AttachmentDTO attachmentDTO = service.findById("attachment-id");
  //    Assertions.assertEquals(attachmentDTO.getName(), fileName);
  //    Assertions.assertEquals(
  //        attachmentDTO.getContentType(), MediaType.APPLICATION_OCTET_STREAM_VALUE);
  //    Assertions.assertEquals(attachmentDTO.getSize(), attachment.getSize());
  //    Assertions.assertEquals(Arrays.toString(attachmentDTO.getPayload()), Arrays.toString(data));
  //  }

  /**
   * Test that, if the creator of the attachment is not the authenticated person, it fails with
   * forbidden
   */
  @Test
  public void test_DownloadById_Forbidden() {
    Person authenticatedUser = mock(Person.class);
    when(authenticatedUser.getId()).thenReturn(2L);
    setAuthenticationContext(authenticatedUser, null);

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

  //  /**
  //   * Tests a correct file retrieval. Check that the returned DTO contains the correct
  // information,
  //   * included the payload
  //   */
  //  @Test
  //  @WithUserDetails("TheResearcher")
  //  public void test_findById_Ok() {
  //    byte[] data = "Hello, World!".getBytes();
  //    String fileName = "text.txt";
  //    Attachment attachment =
  //        Attachment.builder()
  //            .name(fileName)
  //            .payload(data)
  //            .contentType(MediaType.APPLICATION_OCTET_STREAM_VALUE)
  //            .size((long) data.length)
  //            .build();
  //    Person person = Person.builder()
  //        .id(104L)
  //        .authName("TheResearcher")
  //        .authSubject("researcher")
  //        .authEmail("researcher@negotiator.dev")
  //        .build();
  //    attachment.setCreatedBy(person);
  //
  //    when(attachmentRepository.findById("abcd")).thenReturn(Optional.of(attachment));
  //    AttachmentDTO attachmentDTO = service.findById("abcd");
  //    Assertions.assertEquals(attachmentDTO.getName(), fileName);
  //    Assertions.assertEquals(
  //        attachmentDTO.getContentType(), MediaType.APPLICATION_OCTET_STREAM_VALUE);
  //    Assertions.assertEquals(Arrays.toString(attachmentDTO.getPayload()), Arrays.toString(data));
  //  }
}
