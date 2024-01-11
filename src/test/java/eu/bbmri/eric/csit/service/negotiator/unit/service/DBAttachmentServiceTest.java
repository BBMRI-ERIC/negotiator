package eu.bbmri.eric.csit.service.negotiator.unit.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import eu.bbmri.eric.csit.service.negotiator.database.model.Attachment;
import eu.bbmri.eric.csit.service.negotiator.database.model.DataSource;
import eu.bbmri.eric.csit.service.negotiator.database.model.Negotiation;
import eu.bbmri.eric.csit.service.negotiator.database.model.Organization;
import eu.bbmri.eric.csit.service.negotiator.database.model.Person;
import eu.bbmri.eric.csit.service.negotiator.database.model.Request;
import eu.bbmri.eric.csit.service.negotiator.database.model.Resource;
import eu.bbmri.eric.csit.service.negotiator.database.repository.AttachmentRepository;
import eu.bbmri.eric.csit.service.negotiator.database.repository.NegotiationRepository;
import eu.bbmri.eric.csit.service.negotiator.dto.attachments.AttachmentDTO;
import eu.bbmri.eric.csit.service.negotiator.dto.attachments.AttachmentMetadataDTO;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri.eric.csit.service.negotiator.exceptions.ForbiddenRequestException;
import eu.bbmri.eric.csit.service.negotiator.mappers.AttachmentMapper;
import eu.bbmri.eric.csit.service.negotiator.service.DBAttachmentService;
import eu.bbmri.eric.csit.service.negotiator.service.NegotiationService;
import eu.bbmri.eric.csit.service.negotiator.service.PersonService;
import eu.bbmri.eric.csit.service.negotiator.unit.context.WithMockNegotiatorUser;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.modelmapper.ModelMapper;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(SpringExtension.class)
@ContextConfiguration
public class DBAttachmentServiceTest {

  private static final long RESEARCHER_ID = 2L;
  private static final String RESEARCHER_AUTH_NAME = "researcher";
  private static final String RESEARCHER_AUTH_SUBJECT = "researcher@aai.eu";
  private static final String RESEARCHER_AUTH_EMAIL = "researcher@aai.eu";
  private static final long BIOBANKER_1_ID = 3L;
  private static final String BIOBANKER_1_AUTH_NAME = "biobanker_1";
  private static final String BIOBANKER_1_AUTH_SUBJECT = "biobanker_1@aai.eu";
  private static final String BIOBANKER_1_AUTH_EMAIL = "biobanker_1@aai.eu";
  private static final long BIOBANKER_2_ID = 4L;
  private static final String BIOBANKER_2_AUTH_NAME = "biobanker_2";
  private static final String BIOBANKER_2_AUTH_SUBJECT = "biobanker_2@aai.eu";
  private static final String BIOBANKER_2_AUTH_EMAIL = "biobanker_2@aai.eu";
  private static final long ADMIN_ID = 5L;
  private static final String ADMIN_AUTH_NAME = "admin";
  private static final String ADMIN_AUTH_SUBJECT = "admin@aai.eu";
  private static final String ADMIN_AUTH_EMAIL = "admin@aai.eu";
  private static final String ORG_1 = "Organization_1";
  private static final String ORG_2 = "Organization_2";
  private static Person researcher;
  @Mock AttachmentRepository attachmentRepository;
  @Mock NegotiationRepository negotiationRepository;

  @Mock PersonService personService;

  @Mock NegotiationService negotiationService;
  @Spy ModelMapper modelMapper = new ModelMapper();

  @InjectMocks DBAttachmentService service;

  @InjectMocks AttachmentMapper attachmentMapper;

  @InjectMocks DBAttachmentService attachmentService;

  private Attachment privateAttachment; // attachment which is not assigned to a negotiation
  private Attachment publicNegotiationAttachment; // public attachment of a negotiation
  private Attachment privateNegotiationAttachment; // private attachment of a negotiation

  @BeforeEach
  void before() {
    attachmentMapper.addMappings();
    Person researcher =
        Person.builder()
            .id(RESEARCHER_ID)
            .name(RESEARCHER_AUTH_NAME)
            .email(RESEARCHER_AUTH_EMAIL)
            .subjectId(RESEARCHER_AUTH_SUBJECT)
            .build();

    DataSource dataSource = new DataSource();
    Organization organization1 = Organization.builder().externalId(ORG_1).build();
    Organization organization2 = Organization.builder().externalId(ORG_2).build();

    Resource resource1 =
        Resource.builder()
            .dataSource(dataSource)
            .sourceId("resource:1")
            .name("Resource 1")
            .organization(organization1)
            .build();

    Resource resource2 =
        Resource.builder()
            .dataSource(dataSource)
            .sourceId("resource:2")
            .name("Resource 2")
            .organization(organization2)
            .build();

    organization1.setResources(Set.of(resource1));
    organization2.setResources(Set.of(resource2));

    Request request = Request.builder().resources(Set.of(resource1)).build();

    Negotiation negotiation = Negotiation.builder().requests(Set.of(request)).build();
    negotiation.setCreatedBy(researcher);

    byte[] data = "Hello, World!".getBytes();
    String fileName = "text.txt";

    privateAttachment =
        Attachment.builder()
            .name(fileName)
            .payload(data)
            .contentType(MediaType.APPLICATION_OCTET_STREAM_VALUE)
            .size((long) data.length)
            .build();
    privateAttachment.setCreatedBy(researcher);

    publicNegotiationAttachment =
        Attachment.builder()
            .name(fileName)
            .payload(data)
            .contentType(MediaType.APPLICATION_OCTET_STREAM_VALUE)
            .size((long) data.length)
            .negotiation(negotiation)
            .build();
    publicNegotiationAttachment.setCreatedBy(researcher);

    privateNegotiationAttachment =
        Attachment.builder()
            .name(fileName)
            .payload(data)
            .contentType(MediaType.APPLICATION_OCTET_STREAM_VALUE)
            .size((long) data.length)
            .negotiation(negotiation)
            .organization(organization2)
            .build();
    privateNegotiationAttachment.setCreatedBy(researcher);
    when(personService.isRepresentativeOfAnyResource(BIOBANKER_1_ID, List.of("resource:1")))
        .thenReturn(true);
  }

  @Test
  public void test_CreateFileForNegotiation_OK() {
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

    AttachmentMetadataDTO metadataDTO = service.createForNegotiation("abcd", null, file);
    Assertions.assertEquals(metadataDTO.getContentType(), MediaType.APPLICATION_OCTET_STREAM_VALUE);
    Assertions.assertEquals(metadataDTO.getName(), fileName);
    Assertions.assertEquals(metadataDTO.getSize(), data.length);
  }

  @Test
  public void test_CreateFileForNegotiation_NegotiationNotFound() {
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
          service.createForNegotiation("abcd", null, file);
        });
  }

  @Test
  public void test_CreateFile_FailWhenIOException() throws IOException {
    Negotiation negotiation = Negotiation.builder().build();
    negotiation.setId("abcd");

    MultipartFile file = mock(MultipartFile.class);
    when(file.getBytes()).thenThrow(IOException.class);

    Assertions.assertThrows(
        RuntimeException.class, () -> service.createForNegotiation("abcd", null, file));
  }

  @Test
  public void test_CreateFileWithoutNegotiation_OK() {
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
  @WithMockNegotiatorUser(
      id = RESEARCHER_ID,
      authName = RESEARCHER_AUTH_NAME,
      authSubject = RESEARCHER_AUTH_SUBJECT,
      authEmail = RESEARCHER_AUTH_EMAIL,
      authorities = {"ROLE_RESEARCHER"})
  public void test_FindByNegotiation_All_AsResearcher() {
    List<Attachment> attachments =
        List.of(publicNegotiationAttachment, privateNegotiationAttachment);
    when(attachmentRepository.findByNegotiationId("abcd")).thenReturn(attachments);
    when(negotiationService.isAuthorizedForNegotiation(any())).thenReturn(true);
    List<AttachmentMetadataDTO> attachmentsMetadata = service.findByNegotiation("abcd");
    Assertions.assertEquals(attachmentsMetadata.size(), attachments.size());
    Assertions.assertEquals(
        attachmentsMetadata.get(0).getContentType(), MediaType.APPLICATION_OCTET_STREAM_VALUE);
    Assertions.assertEquals(
        attachmentsMetadata.get(0).getName(), publicNegotiationAttachment.getName());
    Assertions.assertEquals(
        attachmentsMetadata.get(0).getSize(), publicNegotiationAttachment.getSize());
  }

  @Test
  @WithMockNegotiatorUser(
      id = ADMIN_ID,
      authName = ADMIN_AUTH_NAME,
      authSubject = ADMIN_AUTH_SUBJECT,
      authEmail = ADMIN_AUTH_EMAIL,
      authorities = {"ROLE_ADMIN"})
  public void test_FindByNegotiation_All_AsAdmin() {
    List<Attachment> attachments =
        List.of(publicNegotiationAttachment, privateNegotiationAttachment);
    when(attachmentRepository.findByNegotiationId("abcd")).thenReturn(attachments);

    List<AttachmentMetadataDTO> attachmentsMetadata = service.findByNegotiation("abcd");
    Assertions.assertEquals(attachmentsMetadata.size(), attachments.size());
    Assertions.assertEquals(
        attachmentsMetadata.get(0).getContentType(), MediaType.APPLICATION_OCTET_STREAM_VALUE);
    Assertions.assertEquals(
        attachmentsMetadata.get(0).getName(), publicNegotiationAttachment.getName());
    Assertions.assertEquals(
        attachmentsMetadata.get(0).getSize(), publicNegotiationAttachment.getSize());
  }

  @Test
  @WithMockNegotiatorUser(
      id = BIOBANKER_1_ID,
      authName = BIOBANKER_1_AUTH_NAME,
      authSubject = BIOBANKER_1_AUTH_SUBJECT,
      authEmail = BIOBANKER_1_AUTH_EMAIL,
      authorities = {"ROLE_REPRESENTATIVE", "ROLE_REPRESENTATIVE_resource:1"})
  public void test_FindByNegotiation_All_AsBiobanker() {
    List<Attachment> attachments =
        List.of(publicNegotiationAttachment, privateNegotiationAttachment);
    when(attachmentRepository.findByNegotiationId("abcd")).thenReturn(attachments);
    when(negotiationService.isAuthorizedForNegotiation(any())).thenReturn(true);
    List<AttachmentMetadataDTO> attachmentsMetadata = service.findByNegotiation("abcd");
    Assertions.assertEquals(attachmentsMetadata.size(), 1);
    Assertions.assertEquals(
        attachmentsMetadata.get(0).getContentType(), MediaType.APPLICATION_OCTET_STREAM_VALUE);
    Assertions.assertEquals(
        attachmentsMetadata.get(0).getName(), publicNegotiationAttachment.getName());
    Assertions.assertEquals(
        attachmentsMetadata.get(0).getSize(), publicNegotiationAttachment.getSize());
  }

  @Test
  @WithMockNegotiatorUser(
      id = BIOBANKER_2_ID,
      authName = BIOBANKER_2_AUTH_NAME,
      authSubject = BIOBANKER_2_AUTH_SUBJECT,
      authEmail = BIOBANKER_2_AUTH_EMAIL,
      authorities = {"ROLE_REPRESENTATIVE", "ROLE_REPRESENTATIVE_resource:2"})
  public void test_FindByNegotiation_EmptyList_WhenTheUser_IsNotAuthorizedForNegotiation() {
    List<Attachment> attachments =
        List.of(publicNegotiationAttachment, privateNegotiationAttachment);
    when(attachmentRepository.findByNegotiationId("abcd")).thenReturn(attachments);
    when(negotiationService.isAuthorizedForNegotiation(any())).thenReturn(false);
    List<AttachmentMetadataDTO> attachmentsMetadata = service.findByNegotiation("abcd");
    Assertions.assertEquals(attachmentsMetadata.size(), 0);
  }

  @Test
  public void test_FindByNegotiation_EmptyList() {
    when(attachmentRepository.findByNegotiationId("abcd")).thenReturn(Collections.emptyList());
    List<AttachmentMetadataDTO> attachments = service.findByNegotiation("abcd");
    Assertions.assertEquals(attachments.size(), 0);
  }

  @Test
  @WithMockNegotiatorUser(
      id = RESEARCHER_ID,
      authName = RESEARCHER_AUTH_NAME,
      authSubject = RESEARCHER_AUTH_SUBJECT,
      authEmail = RESEARCHER_AUTH_EMAIL,
      authorities = {"ROLE_RESEARCHER"})
  public void test_findByIdAndNegotiation_whenPublic_OK_AsResearcher() {
    when(attachmentRepository.findByIdAndNegotiationId("attachment-id", "negotiation-id"))
        .thenReturn(Optional.of(publicNegotiationAttachment));
    when(negotiationService.isAuthorizedForNegotiation(any())).thenReturn(true);
    AttachmentMetadataDTO attachments =
        service.findByIdAndNegotiation("attachment-id", "negotiation-id");

    Assertions.assertEquals(attachments.getContentType(), MediaType.APPLICATION_OCTET_STREAM_VALUE);
    Assertions.assertEquals(attachments.getName(), publicNegotiationAttachment.getName());
    Assertions.assertEquals(attachments.getSize(), publicNegotiationAttachment.getSize());
  }

  @Test
  @WithMockNegotiatorUser(
      id = ADMIN_ID,
      authName = ADMIN_AUTH_NAME,
      authSubject = ADMIN_AUTH_SUBJECT,
      authEmail = ADMIN_AUTH_EMAIL,
      authorities = {"ROLE_ADMIN"})
  public void test_findByIdAndNegotiation_whenPublic_OK_AsAdmin() {
    when(attachmentRepository.findByIdAndNegotiationId("attachment-id", "negotiation-id"))
        .thenReturn(Optional.of(publicNegotiationAttachment));

    AttachmentMetadataDTO attachments =
        service.findByIdAndNegotiation("attachment-id", "negotiation-id");

    Assertions.assertEquals(attachments.getContentType(), MediaType.APPLICATION_OCTET_STREAM_VALUE);
    Assertions.assertEquals(attachments.getName(), publicNegotiationAttachment.getName());
    Assertions.assertEquals(attachments.getSize(), publicNegotiationAttachment.getSize());
  }

  @Test
  @WithMockNegotiatorUser(
      id = BIOBANKER_1_ID,
      authName = BIOBANKER_1_AUTH_NAME,
      authSubject = BIOBANKER_1_AUTH_SUBJECT,
      authEmail = BIOBANKER_1_AUTH_EMAIL,
      authorities = {"ROLE_REPRESENTATIVE", "ROLE_REPRESENTATIVE_resource:1"})
  public void test_findByIdAndNegotiation_whenPublic_OK_AsBiobanker() {
    when(attachmentRepository.findByIdAndNegotiationId("attachment-id", "negotiation-id"))
        .thenReturn(Optional.of(publicNegotiationAttachment));
    when(negotiationService.isAuthorizedForNegotiation(any())).thenReturn(true);

    AttachmentMetadataDTO attachments =
        service.findByIdAndNegotiation("attachment-id", "negotiation-id");
    Assertions.assertEquals(attachments.getContentType(), MediaType.APPLICATION_OCTET_STREAM_VALUE);
    Assertions.assertEquals(attachments.getName(), publicNegotiationAttachment.getName());
    Assertions.assertEquals(attachments.getSize(), publicNegotiationAttachment.getSize());
  }

  @Test
  @WithMockNegotiatorUser(
      id = RESEARCHER_ID,
      authName = RESEARCHER_AUTH_NAME,
      authSubject = RESEARCHER_AUTH_SUBJECT,
      authEmail = RESEARCHER_AUTH_EMAIL,
      authorities = {"ROLE_RESEARCHER"})
  public void test_findByIdAndNegotiation_whenPrivate_OK_AsResearcher() {
    when(attachmentRepository.findByIdAndNegotiationId("attachment-id", "negotiation-id"))
        .thenReturn(Optional.of(publicNegotiationAttachment));
    when(negotiationService.isAuthorizedForNegotiation(any())).thenReturn(true);
    AttachmentMetadataDTO attachments =
        service.findByIdAndNegotiation("attachment-id", "negotiation-id");

    Assertions.assertEquals(attachments.getContentType(), MediaType.APPLICATION_OCTET_STREAM_VALUE);
    Assertions.assertEquals(attachments.getName(), publicNegotiationAttachment.getName());
    Assertions.assertEquals(attachments.getSize(), publicNegotiationAttachment.getSize());
  }

  @Test
  @WithMockNegotiatorUser(
      id = ADMIN_ID,
      authName = ADMIN_AUTH_NAME,
      authSubject = ADMIN_AUTH_SUBJECT,
      authEmail = ADMIN_AUTH_EMAIL,
      authorities = {"ROLE_ADMIN"})
  public void test_findByIdAndNegotiation_whenPrivate_OK_AsAdmin() {
    when(attachmentRepository.findByIdAndNegotiationId("attachment-id", "negotiation-id"))
        .thenReturn(Optional.of(publicNegotiationAttachment));

    AttachmentMetadataDTO attachments =
        service.findByIdAndNegotiation("attachment-id", "negotiation-id");

    Assertions.assertEquals(attachments.getContentType(), MediaType.APPLICATION_OCTET_STREAM_VALUE);
    Assertions.assertEquals(attachments.getName(), publicNegotiationAttachment.getName());
    Assertions.assertEquals(attachments.getSize(), publicNegotiationAttachment.getSize());
  }

  @Test
  @WithMockNegotiatorUser(
      id = BIOBANKER_1_ID,
      authName = BIOBANKER_1_AUTH_NAME,
      authSubject = BIOBANKER_1_AUTH_SUBJECT,
      authEmail = BIOBANKER_1_AUTH_EMAIL,
      authorities = {"ROLE_REPRESENTATIVE", "ROLE_REPRESENTATIVE_resource:1"})
  public void test_findByIdAndNegotiation_whenPrivate_Forbiddend_AsBiobanker() {
    when(negotiationService.isAuthorizedForNegotiation(any())).thenReturn(false);
    when(attachmentRepository.findByIdAndNegotiationId("attachment-id", "negotiation-id"))
        .thenReturn(Optional.of(privateNegotiationAttachment));
    Assertions.assertThrows(
        ForbiddenRequestException.class,
        () -> service.findByIdAndNegotiation("attachment-id", "negotiation-id"));
  }

  @Test
  public void test_findByIdAndNegotiationId_NotFound() {
    when(attachmentRepository.findByIdAndNegotiationId("attachment-id", "negotiation-id"))
        .thenReturn(Optional.empty());

    Assertions.assertThrows(
        EntityNotFoundException.class,
        () -> service.findByIdAndNegotiation("attachment-id", "negotiation-id"));
  }

  @Test
  @WithMockNegotiatorUser(
      id = BIOBANKER_2_ID,
      authName = BIOBANKER_2_AUTH_NAME,
      authSubject = BIOBANKER_2_AUTH_SUBJECT,
      authEmail = BIOBANKER_2_AUTH_EMAIL,
      authorities = {"ROLE_REPRESENTATIVE", "ROLE_REPRESENTATIVE_resource:2"})
  public void test_FindByIdAndNegotiation_Forbidden_WhenTheUserIsNotAuthorizedForNegotiation() {
    when(attachmentRepository.findByIdAndNegotiationId("attachment-id", "negotiation-id"))
        .thenReturn(Optional.of(publicNegotiationAttachment));
    when(negotiationService.isAuthorizedForNegotiation(any())).thenReturn(false);
    Assertions.assertThrows(
        ForbiddenRequestException.class,
        () -> service.findByIdAndNegotiation("attachment-id", "negotiation-id"));
  }

  /**
   * Test correct download of an attachment when the creator of the attachment is authenticated user
   */
  @Test
  @WithMockNegotiatorUser(
      id = RESEARCHER_ID,
      authName = RESEARCHER_AUTH_NAME,
      authSubject = RESEARCHER_AUTH_SUBJECT,
      authEmail = RESEARCHER_AUTH_EMAIL,
      authorities = {"ROLE_RESEARCHER"})
  public void test_FindById_Ok_WhenTheAuthenticatedUser_IsCreator() {
    when(attachmentRepository.findById("attachment-id")).thenReturn(Optional.of(privateAttachment));

    AttachmentDTO attachmentDTO = service.findById("attachment-id");
    Assertions.assertEquals(attachmentDTO.getName(), privateAttachment.getName());
    Assertions.assertEquals(
        attachmentDTO.getContentType(), MediaType.APPLICATION_OCTET_STREAM_VALUE);
    Assertions.assertEquals(attachmentDTO.getSize(), privateAttachment.getSize());
    Assertions.assertEquals(
        Arrays.toString(attachmentDTO.getPayload()),
        Arrays.toString(privateAttachment.getPayload()));
  }

  /**
   * Test correct download of an attachment when the creator of the attachment is authenticated user
   */
  @Test
  @WithMockNegotiatorUser(
      id = ADMIN_ID,
      authName = ADMIN_AUTH_NAME,
      authSubject = ADMIN_AUTH_SUBJECT,
      authEmail = ADMIN_AUTH_EMAIL,
      authorities = {"ROLE_ADMIN"})
  public void test_FindById_Ok_WhenAdmin() {
    when(attachmentRepository.findById("attachment-id")).thenReturn(Optional.of(privateAttachment));

    AttachmentDTO attachmentDTO = service.findById("attachment-id");
    Assertions.assertEquals(attachmentDTO.getName(), privateAttachment.getName());
    Assertions.assertEquals(
        attachmentDTO.getContentType(), MediaType.APPLICATION_OCTET_STREAM_VALUE);
    Assertions.assertEquals(attachmentDTO.getSize(), privateAttachment.getSize());
    Assertions.assertEquals(
        Arrays.toString(attachmentDTO.getPayload()),
        Arrays.toString(privateAttachment.getPayload()));
  }

  @Test
  @WithMockNegotiatorUser(
      id = BIOBANKER_1_ID,
      authName = BIOBANKER_1_AUTH_NAME,
      authSubject = BIOBANKER_1_AUTH_SUBJECT,
      authEmail = BIOBANKER_1_AUTH_EMAIL,
      authorities = {"ROLE_REPRESENTATIVE"})
  public void test_FindById_Forbidden_WhenTheAuthenticatedUser_IsNotCreator() {
    when(attachmentRepository.findById("attachment-id")).thenReturn(Optional.of(privateAttachment));
    Assertions.assertThrows(
        ForbiddenRequestException.class, () -> service.findById("attachment-id"));
  }

  @Test
  @WithMockNegotiatorUser(
      id = BIOBANKER_2_ID,
      authName = BIOBANKER_2_AUTH_NAME,
      authSubject = BIOBANKER_2_AUTH_SUBJECT,
      authEmail = BIOBANKER_2_AUTH_EMAIL,
      authorities = {"ROLE_REPRESENTATIVE", "ROLE_REPRESENTATIVE_resource:2"})
  public void test_FindById_Forbidden_WhenTheAuthenticatedUser_IsNotAuthorizedForNegotiation() {
    when(attachmentRepository.findById("attachment-id")).thenReturn(Optional.of(privateAttachment));
    Assertions.assertThrows(
        ForbiddenRequestException.class, () -> service.findById("attachment-id"));
  }

  @Test
  @WithMockNegotiatorUser(
      id = BIOBANKER_1_ID,
      authName = BIOBANKER_1_AUTH_NAME,
      authSubject = BIOBANKER_1_AUTH_SUBJECT,
      authEmail = BIOBANKER_1_AUTH_EMAIL,
      authorities = {"ROLE_REPRESENTATIVE"})
  public void test_FindById_NotFound() {
    when(attachmentRepository.findById("attachment-id")).thenReturn(Optional.empty());
    Assertions.assertThrows(EntityNotFoundException.class, () -> service.findById("attachment-id"));
  }

  @Test
  @WithMockNegotiatorUser(
      id = RESEARCHER_ID,
      authName = RESEARCHER_AUTH_NAME,
      authSubject = RESEARCHER_AUTH_SUBJECT,
      authEmail = RESEARCHER_AUTH_EMAIL,
      authorities = {"ROLE_RESEARCHER"})
  public void test_FindMetadataById_Ok_WhenTheAuthenticatedUser_IsCreator() {
    when(attachmentRepository.findMetadataById("attachment-id"))
        .thenReturn(Optional.of(privateAttachment));

    AttachmentMetadataDTO attachmentDTO = service.findMetadataById("attachment-id");
    Assertions.assertEquals(attachmentDTO.getName(), privateAttachment.getName());
    Assertions.assertEquals(
        attachmentDTO.getContentType(), MediaType.APPLICATION_OCTET_STREAM_VALUE);
    Assertions.assertEquals(attachmentDTO.getSize(), privateAttachment.getSize());
  }

  /**
   * Test correct download of an attachment when the creator of the attachment is authenticated user
   */
  @Test
  @WithMockNegotiatorUser(
      id = ADMIN_ID,
      authName = ADMIN_AUTH_NAME,
      authSubject = ADMIN_AUTH_SUBJECT,
      authEmail = ADMIN_AUTH_EMAIL,
      authorities = {"ROLE_ADMIN"})
  public void test_FindMetadataById_Ok_WhenAdmin() {
    when(attachmentRepository.findMetadataById("attachment-id"))
        .thenReturn(Optional.of(privateAttachment));
    AttachmentMetadataDTO attachmentDTO = service.findMetadataById("attachment-id");
    Assertions.assertEquals(attachmentDTO.getName(), privateAttachment.getName());
    Assertions.assertEquals(
        attachmentDTO.getContentType(), MediaType.APPLICATION_OCTET_STREAM_VALUE);
    Assertions.assertEquals(attachmentDTO.getSize(), privateAttachment.getSize());
  }

  @Test
  @WithMockNegotiatorUser(
      id = BIOBANKER_1_ID,
      authName = BIOBANKER_1_AUTH_NAME,
      authSubject = BIOBANKER_1_AUTH_SUBJECT,
      authEmail = BIOBANKER_1_AUTH_EMAIL,
      authorities = {"ROLE_REPRESENTATIVE"})
  public void test_FindMetadataById_Forbidden_WhenTheAuthenticatedUser_IsNotCreator() {
    when(attachmentRepository.findMetadataById("attachment-id"))
        .thenReturn(Optional.of(privateAttachment));
    Assertions.assertThrows(
        ForbiddenRequestException.class, () -> service.findMetadataById("attachment-id"));
  }

  @Test
  @WithMockNegotiatorUser(
      id = BIOBANKER_1_ID,
      authName = BIOBANKER_1_AUTH_NAME,
      authSubject = BIOBANKER_1_AUTH_SUBJECT,
      authEmail = BIOBANKER_1_AUTH_EMAIL,
      authorities = {"ROLE_REPRESENTATIVE"})
  public void test_FindMetadataById_NotFound() {
    when(attachmentRepository.findMetadataById("attachment-id")).thenReturn(Optional.empty());
    Assertions.assertThrows(
        EntityNotFoundException.class, () -> service.findMetadataById("attachment-id"));
  }
}
