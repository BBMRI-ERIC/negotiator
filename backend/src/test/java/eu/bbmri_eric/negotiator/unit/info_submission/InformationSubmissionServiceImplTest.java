package eu.bbmri_eric.negotiator.unit.info_submission;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import eu.bbmri_eric.negotiator.common.AuthenticatedUserContext;
import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.common.exceptions.ForbiddenRequestException;
import eu.bbmri_eric.negotiator.governance.resource.ResourceRepository;
import eu.bbmri_eric.negotiator.info_requirement.InformationRequirement;
import eu.bbmri_eric.negotiator.info_requirement.InformationRequirementRepository;
import eu.bbmri_eric.negotiator.info_submission.InformationSubmissionRepository;
import eu.bbmri_eric.negotiator.info_submission.InformationSubmissionServiceImpl;
import eu.bbmri_eric.negotiator.info_submission.pdf.InformationSubmissionToPdfConverter;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class InformationSubmissionServiceImplTest {

  @Mock private InformationSubmissionRepository informationSubmissionRepository;
  @Mock private InformationRequirementRepository informationRequirementRepository;
  @Mock private ResourceRepository resourceRepository;
  @Mock private NegotiationRepository negotiationRepository;
  @Mock private ModelMapper modelMapper;
  @Mock private ApplicationEventPublisher applicationEventPublisher;
  @Mock private PersonRepository personRepository;
  @Mock private InformationSubmissionToPdfConverter InformationSubmissionToPdfConverter;

  private InformationSubmissionServiceImpl service;

  @BeforeEach
  void setUp() {
    service =
        new InformationSubmissionServiceImpl(
            informationSubmissionRepository,
            informationRequirementRepository,
            resourceRepository,
            negotiationRepository,
            modelMapper,
            applicationEventPublisher,
            personRepository,
            InformationSubmissionToPdfConverter);
  }

  @Test
  void createPdfSummary_whenUserIsCreator_returnsSuccessfully() throws IOException {
    Long requirementId = 1L;
    String negotiationId = "neg-001";
    byte[] expectedPdfBytes = new byte[] {1, 2, 3, 4};

    InformationRequirement requirement = mock(InformationRequirement.class);
    eu.bbmri_eric.negotiator.form.AccessForm accessForm =
        mock(eu.bbmri_eric.negotiator.form.AccessForm.class);

    when(requirement.getRequiredAccessForm()).thenReturn(accessForm);
    when(accessForm.getName()).thenReturn("Test Requirement");
    when(requirement.isViewableOnlyByAdmin()).thenReturn(false);
    when(informationRequirementRepository.findById(requirementId))
        .thenReturn(Optional.of(requirement));
    when(negotiationRepository.existsByIdAndCreatedBy_Id(eq(negotiationId), anyLong()))
        .thenReturn(true);

    MultipartFile mockCsvFile =
        new MockMultipartFile("test.csv", "test.csv", "text/csv", "data".getBytes());
    InformationSubmissionServiceImpl spyService = spy(service);
    doReturn(mockCsvFile).when(spyService).createSummary(requirementId, negotiationId);
    when(InformationSubmissionToPdfConverter.convertCsvToPdf(mockCsvFile, negotiationId, "Test Requirement"))
        .thenReturn(expectedPdfBytes);

    try (MockedStatic<AuthenticatedUserContext> mockedContext =
        mockStatic(AuthenticatedUserContext.class)) {
      mockedContext
          .when(AuthenticatedUserContext::getCurrentlyAuthenticatedUserInternalId)
          .thenReturn(1L);
      mockedContext
          .when(AuthenticatedUserContext::isCurrentlyAuthenticatedUserAdmin)
          .thenReturn(false);

      byte[] result = spyService.createPdfSummary(requirementId, negotiationId);

      assertNotNull(result);
      assertArrayEquals(expectedPdfBytes, result);
      verify(InformationSubmissionToPdfConverter).convertCsvToPdf(mockCsvFile, negotiationId, "Test Requirement");
    }
  }

  @Test
  void createPdfSummary_whenUserIsAdmin_returnsSuccessfully() throws IOException {
    Long requirementId = 1L;
    String negotiationId = "neg-001";
    byte[] expectedPdfBytes = new byte[] {1, 2, 3, 4};

    InformationRequirement requirement = mock(InformationRequirement.class);
    eu.bbmri_eric.negotiator.form.AccessForm accessForm =
        mock(eu.bbmri_eric.negotiator.form.AccessForm.class);

    when(requirement.getRequiredAccessForm()).thenReturn(accessForm);
    when(accessForm.getName()).thenReturn("Test Requirement");
    when(requirement.isViewableOnlyByAdmin()).thenReturn(false);
    when(informationRequirementRepository.findById(requirementId))
        .thenReturn(Optional.of(requirement));
    when(negotiationRepository.existsByIdAndCreatedBy_Id(eq(negotiationId), anyLong()))
        .thenReturn(false);

    MultipartFile mockCsvFile =
        new MockMultipartFile("test.csv", "test.csv", "text/csv", "data".getBytes());
    InformationSubmissionServiceImpl spyService = spy(service);
    doReturn(mockCsvFile).when(spyService).createSummary(requirementId, negotiationId);
    when(InformationSubmissionToPdfConverter.convertCsvToPdf(mockCsvFile, negotiationId, "Test Requirement"))
        .thenReturn(expectedPdfBytes);

    try (MockedStatic<AuthenticatedUserContext> mockedContext =
        mockStatic(AuthenticatedUserContext.class)) {
      mockedContext
          .when(AuthenticatedUserContext::getCurrentlyAuthenticatedUserInternalId)
          .thenReturn(1L);
      mockedContext
          .when(AuthenticatedUserContext::isCurrentlyAuthenticatedUserAdmin)
          .thenReturn(true);

      byte[] result = spyService.createPdfSummary(requirementId, negotiationId);

      assertNotNull(result);
      assertArrayEquals(expectedPdfBytes, result);
    }
  }

  @Test
  void createPdfSummary_whenRequirementNotFound_throwsEntityNotFoundException() {
    Long requirementId = 999L;
    String negotiationId = "neg-001";

    when(informationRequirementRepository.findById(requirementId)).thenReturn(Optional.empty());
    when(negotiationRepository.existsByIdAndCreatedBy_Id(eq(negotiationId), anyLong()))
        .thenReturn(true);

    try (MockedStatic<AuthenticatedUserContext> mockedContext =
        mockStatic(AuthenticatedUserContext.class)) {
      mockedContext
          .when(AuthenticatedUserContext::getCurrentlyAuthenticatedUserInternalId)
          .thenReturn(1L);
      mockedContext
          .when(AuthenticatedUserContext::isCurrentlyAuthenticatedUserAdmin)
          .thenReturn(false);

      assertThrows(
          EntityNotFoundException.class,
          () -> service.createPdfSummary(requirementId, negotiationId));
    }
  }

  @Test
  void createPdfSummary_whenUserNotAuthorized_throwsForbiddenRequestException() {
    Long requirementId = 1L;
    String negotiationId = "neg-001";

    when(negotiationRepository.existsByIdAndCreatedBy_Id(eq(negotiationId), anyLong()))
        .thenReturn(false);

    try (MockedStatic<AuthenticatedUserContext> mockedContext =
        mockStatic(AuthenticatedUserContext.class)) {
      mockedContext
          .when(AuthenticatedUserContext::getCurrentlyAuthenticatedUserInternalId)
          .thenReturn(1L);
      mockedContext
          .when(AuthenticatedUserContext::isCurrentlyAuthenticatedUserAdmin)
          .thenReturn(false);

      assertThrows(
          ForbiddenRequestException.class,
          () -> service.createPdfSummary(requirementId, negotiationId));
    }
  }

  @Test
  void createPdfSummary_whenRequirementIsAdminOnlyAndUserNotAdmin_throwsForbiddenRequestException()
      throws IOException {
    Long requirementId = 1L;
    String negotiationId = "neg-001";

    InformationRequirement requirement = mock(InformationRequirement.class);
    when(requirement.isViewableOnlyByAdmin()).thenReturn(true);
    when(informationRequirementRepository.findById(requirementId))
        .thenReturn(Optional.of(requirement));
    when(negotiationRepository.existsByIdAndCreatedBy_Id(eq(negotiationId), anyLong()))
        .thenReturn(true);

    try (MockedStatic<AuthenticatedUserContext> mockedContext =
        mockStatic(AuthenticatedUserContext.class)) {
      mockedContext
          .when(AuthenticatedUserContext::getCurrentlyAuthenticatedUserInternalId)
          .thenReturn(1L);
      mockedContext
          .when(AuthenticatedUserContext::isCurrentlyAuthenticatedUserAdmin)
          .thenReturn(false);

      assertThrows(
          ForbiddenRequestException.class,
          () -> service.createPdfSummary(requirementId, negotiationId));
    }
  }

  @Test
  void createPdfSummary_whenCsvConversionFails_throwsInternalError() throws IOException {
    Long requirementId = 1L;
    String negotiationId = "neg-001";

    InformationRequirement requirement = mock(InformationRequirement.class);
    eu.bbmri_eric.negotiator.form.AccessForm accessForm =
        mock(eu.bbmri_eric.negotiator.form.AccessForm.class);

    when(requirement.getRequiredAccessForm()).thenReturn(accessForm);
    when(accessForm.getName()).thenReturn("Test Requirement");
    when(requirement.isViewableOnlyByAdmin()).thenReturn(false);
    when(informationRequirementRepository.findById(requirementId))
        .thenReturn(Optional.of(requirement));
    when(negotiationRepository.existsByIdAndCreatedBy_Id(eq(negotiationId), anyLong()))
        .thenReturn(true);

    MultipartFile mockCsvFile =
        new MockMultipartFile("test.csv", "test.csv", "text/csv", "data".getBytes());
    InformationSubmissionServiceImpl spyService = spy(service);
    doReturn(mockCsvFile).when(spyService).createSummary(requirementId, negotiationId);
    when(InformationSubmissionToPdfConverter.convertCsvToPdf(mockCsvFile, negotiationId, "Test Requirement"))
        .thenThrow(new IOException("Conversion failed"));

    try (MockedStatic<AuthenticatedUserContext> mockedContext =
        mockStatic(AuthenticatedUserContext.class)) {
      mockedContext
          .when(AuthenticatedUserContext::getCurrentlyAuthenticatedUserInternalId)
          .thenReturn(1L);
      mockedContext
          .when(AuthenticatedUserContext::isCurrentlyAuthenticatedUserAdmin)
          .thenReturn(false);

      assertThrows(
          InternalError.class, () -> spyService.createPdfSummary(requirementId, negotiationId));
    }
  }

  @Test
  void createAllPdfSummaries_whenUserIsCreator_returnsAllPdfs() throws IOException {
    String negotiationId = "neg-001";

    InformationRequirement requirement1 = mock(InformationRequirement.class);
    InformationRequirement requirement2 = mock(InformationRequirement.class);
    eu.bbmri_eric.negotiator.form.AccessForm accessForm1 =
        mock(eu.bbmri_eric.negotiator.form.AccessForm.class);
    eu.bbmri_eric.negotiator.form.AccessForm accessForm2 =
        mock(eu.bbmri_eric.negotiator.form.AccessForm.class);

    when(requirement1.getId()).thenReturn(1L);
    when(requirement1.getRequiredAccessForm()).thenReturn(accessForm1);
    when(accessForm1.getName()).thenReturn("Requirement 1");
    when(requirement1.isViewableOnlyByAdmin()).thenReturn(false);

    when(requirement2.getId()).thenReturn(2L);
    when(requirement2.getRequiredAccessForm()).thenReturn(accessForm2);
    when(accessForm2.getName()).thenReturn("Requirement 2");
    when(requirement2.isViewableOnlyByAdmin()).thenReturn(false);

    when(negotiationRepository.existsByIdAndCreatedBy_Id(eq(negotiationId), anyLong()))
        .thenReturn(true);
    when(informationRequirementRepository.findAll())
        .thenReturn(Arrays.asList(requirement1, requirement2));

    MultipartFile mockCsvFile1 =
        new MockMultipartFile("test1.csv", "test1.csv", "text/csv", "data1".getBytes());
    MultipartFile mockCsvFile2 =
        new MockMultipartFile("test2.csv", "test2.csv", "text/csv", "data2".getBytes());

    byte[] pdfBytes1 = new byte[] {1, 2, 3};
    byte[] pdfBytes2 = new byte[] {4, 5, 6};

    InformationSubmissionServiceImpl spyService = spy(service);
    doReturn(mockCsvFile1).when(spyService).createSummary(1L, negotiationId);
    doReturn(mockCsvFile2).when(spyService).createSummary(2L, negotiationId);

    when(InformationSubmissionToPdfConverter.convertCsvToPdf(mockCsvFile1, negotiationId, "Requirement 1"))
        .thenReturn(pdfBytes1);
    when(InformationSubmissionToPdfConverter.convertCsvToPdf(mockCsvFile2, negotiationId, "Requirement 2"))
        .thenReturn(pdfBytes2);

    try (MockedStatic<AuthenticatedUserContext> mockedContext =
        mockStatic(AuthenticatedUserContext.class)) {
      mockedContext
          .when(AuthenticatedUserContext::getCurrentlyAuthenticatedUserInternalId)
          .thenReturn(1L);
      mockedContext
          .when(AuthenticatedUserContext::isCurrentlyAuthenticatedUserAdmin)
          .thenReturn(false);

      List<byte[]> result = spyService.createAllPdfSummaries(negotiationId);

      assertNotNull(result);
      assertEquals(2, result.size());
      assertArrayEquals(pdfBytes1, result.get(0));
      assertArrayEquals(pdfBytes2, result.get(1));
    }
  }

  @Test
  void createAllPdfSummaries_whenUserIsAdmin_includesAdminOnlyRequirements() throws IOException {
    String negotiationId = "neg-001";

    InformationRequirement requirement1 = mock(InformationRequirement.class);
    InformationRequirement requirement2 = mock(InformationRequirement.class);
    eu.bbmri_eric.negotiator.form.AccessForm accessForm1 =
        mock(eu.bbmri_eric.negotiator.form.AccessForm.class);
    eu.bbmri_eric.negotiator.form.AccessForm accessForm2 =
        mock(eu.bbmri_eric.negotiator.form.AccessForm.class);

    when(requirement1.getId()).thenReturn(1L);
    when(requirement1.getRequiredAccessForm()).thenReturn(accessForm1);
    when(accessForm1.getName()).thenReturn("Public Requirement");
    when(requirement1.isViewableOnlyByAdmin()).thenReturn(false);

    when(requirement2.getId()).thenReturn(2L);
    when(requirement2.getRequiredAccessForm()).thenReturn(accessForm2);
    when(accessForm2.getName()).thenReturn("Admin Only Requirement");
    when(requirement2.isViewableOnlyByAdmin()).thenReturn(true);

    when(negotiationRepository.existsByIdAndCreatedBy_Id(eq(negotiationId), anyLong()))
        .thenReturn(false);
    when(informationRequirementRepository.findAll())
        .thenReturn(Arrays.asList(requirement1, requirement2));

    MultipartFile mockCsvFile1 =
        new MockMultipartFile("test1.csv", "test1.csv", "text/csv", "data1".getBytes());
    MultipartFile mockCsvFile2 =
        new MockMultipartFile("test2.csv", "test2.csv", "text/csv", "data2".getBytes());

    byte[] pdfBytes1 = new byte[] {1, 2, 3};
    byte[] pdfBytes2 = new byte[] {4, 5, 6};

    InformationSubmissionServiceImpl spyService = spy(service);
    doReturn(mockCsvFile1).when(spyService).createSummary(1L, negotiationId);
    doReturn(mockCsvFile2).when(spyService).createSummary(2L, negotiationId);

    when(InformationSubmissionToPdfConverter.convertCsvToPdf(mockCsvFile1, negotiationId, "Public Requirement"))
        .thenReturn(pdfBytes1);
    when(InformationSubmissionToPdfConverter.convertCsvToPdf(mockCsvFile2, negotiationId, "Admin Only Requirement"))
        .thenReturn(pdfBytes2);

    try (MockedStatic<AuthenticatedUserContext> mockedContext =
        mockStatic(AuthenticatedUserContext.class)) {
      mockedContext
          .when(AuthenticatedUserContext::getCurrentlyAuthenticatedUserInternalId)
          .thenReturn(1L);
      mockedContext
          .when(AuthenticatedUserContext::isCurrentlyAuthenticatedUserAdmin)
          .thenReturn(true);

      List<byte[]> result = spyService.createAllPdfSummaries(negotiationId);

      assertNotNull(result);
      assertEquals(2, result.size());
      verify(spyService).createSummary(1L, negotiationId);
      verify(spyService).createSummary(2L, negotiationId);
    }
  }

  @Test
  void createAllPdfSummaries_whenUserIsNotAdmin_skipsAdminOnlyRequirements() throws IOException {
    String negotiationId = "neg-001";

    InformationRequirement requirement1 = mock(InformationRequirement.class);
    InformationRequirement requirement2 = mock(InformationRequirement.class);
    eu.bbmri_eric.negotiator.form.AccessForm accessForm1 =
        mock(eu.bbmri_eric.negotiator.form.AccessForm.class);

    when(requirement1.getId()).thenReturn(1L);
    when(requirement1.getRequiredAccessForm()).thenReturn(accessForm1);
    when(accessForm1.getName()).thenReturn("Public Requirement");
    when(requirement1.isViewableOnlyByAdmin()).thenReturn(false);

    when(requirement2.isViewableOnlyByAdmin()).thenReturn(true);

    when(negotiationRepository.existsByIdAndCreatedBy_Id(eq(negotiationId), anyLong()))
        .thenReturn(true);
    when(informationRequirementRepository.findAll())
        .thenReturn(Arrays.asList(requirement1, requirement2));

    MultipartFile mockCsvFile1 =
        new MockMultipartFile("test1.csv", "test1.csv", "text/csv", "data1".getBytes());

    byte[] pdfBytes1 = new byte[] {1, 2, 3};

    InformationSubmissionServiceImpl spyService = spy(service);
    doReturn(mockCsvFile1).when(spyService).createSummary(1L, negotiationId);

    when(InformationSubmissionToPdfConverter.convertCsvToPdf(mockCsvFile1, negotiationId, "Public Requirement"))
        .thenReturn(pdfBytes1);

    try (MockedStatic<AuthenticatedUserContext> mockedContext =
        mockStatic(AuthenticatedUserContext.class)) {
      mockedContext
          .when(AuthenticatedUserContext::getCurrentlyAuthenticatedUserInternalId)
          .thenReturn(1L);
      mockedContext
          .when(AuthenticatedUserContext::isCurrentlyAuthenticatedUserAdmin)
          .thenReturn(false);

      List<byte[]> result = spyService.createAllPdfSummaries(negotiationId);

      assertNotNull(result);
      assertEquals(1, result.size());
      assertArrayEquals(pdfBytes1, result.get(0));
      verify(spyService).createSummary(1L, negotiationId);
      verify(spyService, never()).createSummary(2L, negotiationId);
    }
  }

  @Test
  void createAllPdfSummaries_whenUserNotAuthorized_throwsForbiddenRequestException() {
    String negotiationId = "neg-001";

    when(negotiationRepository.existsByIdAndCreatedBy_Id(eq(negotiationId), anyLong()))
        .thenReturn(false);

    try (MockedStatic<AuthenticatedUserContext> mockedContext =
        mockStatic(AuthenticatedUserContext.class)) {
      mockedContext
          .when(AuthenticatedUserContext::getCurrentlyAuthenticatedUserInternalId)
          .thenReturn(1L);
      mockedContext
          .when(AuthenticatedUserContext::isCurrentlyAuthenticatedUserAdmin)
          .thenReturn(false);

      assertThrows(
          ForbiddenRequestException.class, () -> service.createAllPdfSummaries(negotiationId));
    }
  }

  @Test
  void createAllPdfSummaries_whenNoRequirements_returnsEmptyList() {
    String negotiationId = "neg-001";

    when(negotiationRepository.existsByIdAndCreatedBy_Id(eq(negotiationId), anyLong()))
        .thenReturn(true);
    when(informationRequirementRepository.findAll()).thenReturn(Collections.emptyList());

    try (MockedStatic<AuthenticatedUserContext> mockedContext =
        mockStatic(AuthenticatedUserContext.class)) {
      mockedContext
          .when(AuthenticatedUserContext::getCurrentlyAuthenticatedUserInternalId)
          .thenReturn(1L);
      mockedContext
          .when(AuthenticatedUserContext::isCurrentlyAuthenticatedUserAdmin)
          .thenReturn(false);

      List<byte[]> result = service.createAllPdfSummaries(negotiationId);

      assertNotNull(result);
      assertTrue(result.isEmpty());
    }
  }

  @Test
  void createAllPdfSummaries_whenCsvIsEmpty_skipsRequirement() throws IOException {
    String negotiationId = "neg-001";

    InformationRequirement requirement1 = mock(InformationRequirement.class);
    InformationRequirement requirement2 = mock(InformationRequirement.class);
    eu.bbmri_eric.negotiator.form.AccessForm accessForm2 =
        mock(eu.bbmri_eric.negotiator.form.AccessForm.class);

    when(requirement1.getId()).thenReturn(1L);
    when(requirement1.isViewableOnlyByAdmin()).thenReturn(false);

    when(requirement2.getId()).thenReturn(2L);
    when(requirement2.getRequiredAccessForm()).thenReturn(accessForm2);
    when(accessForm2.getName()).thenReturn("Valid Requirement");
    when(requirement2.isViewableOnlyByAdmin()).thenReturn(false);

    when(negotiationRepository.existsByIdAndCreatedBy_Id(eq(negotiationId), anyLong()))
        .thenReturn(true);
    when(informationRequirementRepository.findAll())
        .thenReturn(Arrays.asList(requirement1, requirement2));

    MultipartFile mockCsvFile1 =
        new MockMultipartFile("test1.csv", "test1.csv", "text/csv", new byte[0]);
    MultipartFile mockCsvFile2 =
        new MockMultipartFile("test2.csv", "test2.csv", "text/csv", "data2".getBytes());

    byte[] pdfBytes2 = new byte[] {4, 5, 6};

    InformationSubmissionServiceImpl spyService = spy(service);
    doReturn(mockCsvFile1).when(spyService).createSummary(1L, negotiationId);
    doReturn(mockCsvFile2).when(spyService).createSummary(2L, negotiationId);

    when(InformationSubmissionToPdfConverter.convertCsvToPdf(mockCsvFile2, negotiationId, "Valid Requirement"))
        .thenReturn(pdfBytes2);

    try (MockedStatic<AuthenticatedUserContext> mockedContext =
        mockStatic(AuthenticatedUserContext.class)) {
      mockedContext
          .when(AuthenticatedUserContext::getCurrentlyAuthenticatedUserInternalId)
          .thenReturn(1L);
      mockedContext
          .when(AuthenticatedUserContext::isCurrentlyAuthenticatedUserAdmin)
          .thenReturn(false);

      List<byte[]> result = spyService.createAllPdfSummaries(negotiationId);

      assertNotNull(result);
      assertEquals(1, result.size());
      assertArrayEquals(pdfBytes2, result.get(0));
    }
  }

  @Test
  void createAllPdfSummaries_whenPdfConversionFails_continuesWithOtherRequirements()
      throws IOException {
    String negotiationId = "neg-001";

    InformationRequirement requirement1 = mock(InformationRequirement.class);
    InformationRequirement requirement2 = mock(InformationRequirement.class);
    eu.bbmri_eric.negotiator.form.AccessForm accessForm1 =
        mock(eu.bbmri_eric.negotiator.form.AccessForm.class);
    eu.bbmri_eric.negotiator.form.AccessForm accessForm2 =
        mock(eu.bbmri_eric.negotiator.form.AccessForm.class);

    when(requirement1.getId()).thenReturn(1L);
    when(requirement1.getRequiredAccessForm()).thenReturn(accessForm1);
    when(accessForm1.getName()).thenReturn("Failing Requirement");
    when(requirement1.isViewableOnlyByAdmin()).thenReturn(false);

    when(requirement2.getId()).thenReturn(2L);
    when(requirement2.getRequiredAccessForm()).thenReturn(accessForm2);
    when(accessForm2.getName()).thenReturn("Success Requirement");
    when(requirement2.isViewableOnlyByAdmin()).thenReturn(false);

    when(negotiationRepository.existsByIdAndCreatedBy_Id(eq(negotiationId), anyLong()))
        .thenReturn(true);
    when(informationRequirementRepository.findAll())
        .thenReturn(Arrays.asList(requirement1, requirement2));

    MultipartFile mockCsvFile1 =
        new MockMultipartFile("test1.csv", "test1.csv", "text/csv", "data1".getBytes());
    MultipartFile mockCsvFile2 =
        new MockMultipartFile("test2.csv", "test2.csv", "text/csv", "data2".getBytes());

    byte[] pdfBytes2 = new byte[] {4, 5, 6};

    InformationSubmissionServiceImpl spyService = spy(service);
    doReturn(mockCsvFile1).when(spyService).createSummary(1L, negotiationId);
    doReturn(mockCsvFile2).when(spyService).createSummary(2L, negotiationId);

    when(InformationSubmissionToPdfConverter.convertCsvToPdf(mockCsvFile1, negotiationId, "Failing Requirement"))
        .thenThrow(new IOException("PDF conversion failed"));
    when(InformationSubmissionToPdfConverter.convertCsvToPdf(mockCsvFile2, negotiationId, "Success Requirement"))
        .thenReturn(pdfBytes2);

    try (MockedStatic<AuthenticatedUserContext> mockedContext =
        mockStatic(AuthenticatedUserContext.class)) {
      mockedContext
          .when(AuthenticatedUserContext::getCurrentlyAuthenticatedUserInternalId)
          .thenReturn(1L);
      mockedContext
          .when(AuthenticatedUserContext::isCurrentlyAuthenticatedUserAdmin)
          .thenReturn(false);

      List<byte[]> result = spyService.createAllPdfSummaries(negotiationId);

      assertNotNull(result);
      assertEquals(1, result.size());
      assertArrayEquals(pdfBytes2, result.get(0));
    }
  }

  @Test
  void createAllPdfSummaries_whenCsvCreationFails_continuesWithOtherRequirements()
      throws IOException {
    String negotiationId = "neg-001";

    InformationRequirement requirement1 = mock(InformationRequirement.class);
    InformationRequirement requirement2 = mock(InformationRequirement.class);
    eu.bbmri_eric.negotiator.form.AccessForm accessForm2 =
        mock(eu.bbmri_eric.negotiator.form.AccessForm.class);

    when(requirement1.getId()).thenReturn(1L);
    when(requirement1.isViewableOnlyByAdmin()).thenReturn(false);

    when(requirement2.getId()).thenReturn(2L);
    when(requirement2.getRequiredAccessForm()).thenReturn(accessForm2);
    when(accessForm2.getName()).thenReturn("Success Requirement");
    when(requirement2.isViewableOnlyByAdmin()).thenReturn(false);

    when(negotiationRepository.existsByIdAndCreatedBy_Id(eq(negotiationId), anyLong()))
        .thenReturn(true);
    when(informationRequirementRepository.findAll())
        .thenReturn(Arrays.asList(requirement1, requirement2));

    MultipartFile mockCsvFile2 =
        new MockMultipartFile("test2.csv", "test2.csv", "text/csv", "data2".getBytes());
    byte[] pdfBytes2 = new byte[] {4, 5, 6};

    InformationSubmissionServiceImpl spyService = spy(service);
    doThrow(new RuntimeException("CSV creation failed"))
        .when(spyService)
        .createSummary(1L, negotiationId);
    doReturn(mockCsvFile2).when(spyService).createSummary(2L, negotiationId);

    when(InformationSubmissionToPdfConverter.convertCsvToPdf(mockCsvFile2, negotiationId, "Success Requirement"))
        .thenReturn(pdfBytes2);

    try (MockedStatic<AuthenticatedUserContext> mockedContext =
        mockStatic(AuthenticatedUserContext.class)) {
      mockedContext
          .when(AuthenticatedUserContext::getCurrentlyAuthenticatedUserInternalId)
          .thenReturn(1L);
      mockedContext
          .when(AuthenticatedUserContext::isCurrentlyAuthenticatedUserAdmin)
          .thenReturn(false);

      List<byte[]> result = spyService.createAllPdfSummaries(negotiationId);

      assertNotNull(result);
      assertEquals(1, result.size());
      assertArrayEquals(pdfBytes2, result.get(0));
    }
  }
}
