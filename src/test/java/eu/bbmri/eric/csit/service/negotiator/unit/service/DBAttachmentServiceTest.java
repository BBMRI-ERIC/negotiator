package eu.bbmri.eric.csit.service.negotiator.unit.service;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import eu.bbmri.eric.csit.service.negotiator.database.model.Attachment;
import eu.bbmri.eric.csit.service.negotiator.database.model.Person;
import eu.bbmri.eric.csit.service.negotiator.database.repository.AttachmentRepository;
import eu.bbmri.eric.csit.service.negotiator.dto.attachments.AttachmentDTO;
import eu.bbmri.eric.csit.service.negotiator.dto.attachments.AttachmentMetadataDTO;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri.eric.csit.service.negotiator.mappers.AttachmentMapper;
import eu.bbmri.eric.csit.service.negotiator.service.DBAttachmentService;
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
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.web.multipart.MultipartFile;

public class DBAttachmentServiceTest {

  @Mock AttachmentRepository attachmentRepository;

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

  @Test
  public void test_UploadFile_FailWhenIOException() throws IOException {
    MultipartFile file = mock(MultipartFile.class);
    when(file.getBytes()).thenThrow(IOException.class);
    Assertions.assertThrows(RuntimeException.class, () -> service.create(file));
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

  /**
   * Tests that, when the provided file id is not found, the service propagates the exception raised
   * by the repository
   */
  @Test
  public void test_findById_NotFound() {
    when(attachmentRepository.findById("abcd")).thenThrow(EntityNotFoundException.class);
    Assert.assertThrows(EntityNotFoundException.class, () -> service.findById("abcd"));
  }
}
