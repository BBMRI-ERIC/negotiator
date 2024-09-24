package eu.bbmri_eric.negotiator.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.bbmri_eric.negotiator.attachment.Attachment;
import eu.bbmri_eric.negotiator.attachment.AttachmentRepository;
import eu.bbmri_eric.negotiator.attachment.dto.AttachmentMetadataDTO;
import eu.bbmri_eric.negotiator.common.exceptions.EntityNotStorableException;
import eu.bbmri_eric.negotiator.discovery.DiscoveryService;
import eu.bbmri_eric.negotiator.governance.organization.Organization;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.integration.api.v3.TestUtils;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.NegotiationServiceImpl;
import eu.bbmri_eric.negotiator.negotiation.dto.NegotiationCreateDTO;
import eu.bbmri_eric.negotiator.negotiation.dto.NegotiationDTO;
import eu.bbmri_eric.negotiator.negotiation.request.Request;
import eu.bbmri_eric.negotiator.negotiation.request.RequestRepository;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.notification.UserNotificationService;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import eu.bbmri_eric.negotiator.util.WithMockNegotiatorUser;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.hibernate.exception.DataException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration
public class NegotiationServiceTest {
  @Mock AttachmentRepository attachmentRepository;
  @Mock NegotiationRepository negotiationRepository;
  @Mock PersonRepository personRepository;
  @Mock ApplicationEventPublisher eventPublisher;

  @Mock RequestRepository requestRepository;
  @Mock ModelMapper modelMapper;
  @InjectMocks NegotiationServiceImpl negotiationService;
  @Mock UserNotificationService userNotificationService;
  private AutoCloseable closeable;

  private static Negotiation buildNegotiation() {
    Set<Resource> resources =
        Set.of(
            Resource.builder()
                .sourceId("collection:1")
                .discoveryService(new DiscoveryService())
                .organization(
                    Organization.builder().externalId("biobank:1").name("TestBiobank").build())
                .build());

    return Negotiation.builder()
        .resources(resources)
        .currentState(NegotiationState.SUBMITTED)
        .humanReadable("#1 Material Type: DNA")
        .build();
  }

  @BeforeEach
  void before() {
    closeable = MockitoAnnotations.openMocks(this);
  }

  @AfterEach
  void after() throws Exception {
    closeable.close();
  }

  @Test
  public void test_Exist_IsFalse_WhenNegotiationIsNotFound() {
    when(negotiationRepository.existsById(any())).thenReturn(false);
    assertFalse(negotiationService.exists("unknown"));
  }

  @Test
  public void test_Exist_IsTrue_WhenNegotiationIsFound() {
    when(negotiationRepository.existsById(any())).thenReturn(true);
    assertTrue(negotiationService.exists("123"));
  }

  @Test
  @WithMockNegotiatorUser(
      id = 2L,
      authName = "researcher",
      authSubject = "researcher@aai.eu",
      authEmail = "researcher@aai.eu",
      authorities = {"ROLE_RESEARCHER"})
  public void test_isNegotiatorCreator_IsFalse_WhenPersonRepositoryIsNegotiatiorCreator_IsFalse() {
    when(personRepository.isNegotiationCreator(any(), any())).thenReturn(false);
    assertFalse(negotiationService.isNegotiationCreator("123"));
  }

  @Test
  @WithMockNegotiatorUser(
      id = 2L,
      authName = "researcher",
      authSubject = "researcher@aai.eu",
      authEmail = "researcher@aai.eu",
      authorities = {"ROLE_RESEARCHER"})
  public void test_isNegotiatorCreator_IsTrue_WhenPersonRepositoryIsNegotiatiorCreator_IsTrue() {
    when(personRepository.isNegotiationCreator(any(), any())).thenReturn(true);
    assertTrue(negotiationService.isNegotiationCreator("123"));
  }

  @Test
  void testCreateNegotiation_ok() throws IOException {
    NegotiationCreateDTO negotiationCreateDTO = TestUtils.createNegotiation(Set.of("requestID"));

    Negotiation negotiation = Negotiation.builder().build();
    Request request = new Request();
    request.setResources(Set.of(new Resource()));
    negotiation.setResources(request.getResources());
    when(requestRepository.findAllById(Set.of("requestID"))).thenReturn(List.of(request));
    when(modelMapper.map(negotiationCreateDTO, Negotiation.class)).thenReturn(negotiation);
    NegotiationDTO savedDTO = new NegotiationDTO();
    savedDTO.setId("saved");
    when(negotiationRepository.save(negotiation)).thenReturn(negotiation);
    when(modelMapper.map(negotiation, NegotiationDTO.class)).thenReturn(savedDTO);
    NegotiationDTO negotiationDTO = negotiationService.create(negotiationCreateDTO, 100L);
    assertEquals("saved", negotiationDTO.getId());
    verify(userNotificationService, times(1)).notifyAdmins(negotiation);
  }

  @Test
  void testCreateNegotiation_ok_with_attachments() throws IOException {
    NegotiationCreateDTO negotiationCreateDTO = TestUtils.createNegotiation(Set.of("requestID"));
    AttachmentMetadataDTO attachmentMetadataDTO =
        AttachmentMetadataDTO.builder().id("attachment-1").build();
    negotiationCreateDTO.setAttachments(Set.of(attachmentMetadataDTO));

    Negotiation negotiation = Negotiation.builder().build();
    Request request = new Request();
    request.setResources(Set.of(new Resource()));
    negotiation.setResources(request.getResources());
    Attachment attachment = Attachment.builder().id("attachment-1").name("Attachment-1").build();

    when(attachmentRepository.findAllById(List.of("attachment-1"))).thenReturn(List.of(attachment));
    when(requestRepository.findAllById(Set.of("requestID"))).thenReturn(List.of(request));
    when(modelMapper.map(negotiationCreateDTO, Negotiation.class)).thenReturn(negotiation);

    NegotiationDTO savedDTO = new NegotiationDTO();
    savedDTO.setId("saved");
    when(negotiationRepository.save(any())).thenReturn(negotiation);
    when(modelMapper.map(negotiation, NegotiationDTO.class)).thenReturn(savedDTO);
    NegotiationDTO negotiationDTO = negotiationService.create(negotiationCreateDTO, 100L);
    assertEquals("saved", negotiationDTO.getId());
    verify(userNotificationService, times(1)).notifyAdmins(negotiation);
  }

  @Test
  void testCreateNegotiation_fails_when_DataException() throws IOException {
    NegotiationCreateDTO negotiationCreateDTO = TestUtils.createNegotiation(Set.of("requestID"));
    Negotiation negotiation = new Negotiation();
    Request request = new Request();
    request.setResources(Set.of(new Resource()));
    negotiation.setResources(request.getResources());
    when(requestRepository.findAllById(Set.of("requestID"))).thenReturn(List.of(request));
    when(modelMapper.map(negotiationCreateDTO, Negotiation.class)).thenReturn(negotiation);
    when(negotiationRepository.save(any())).thenThrow(DataException.class);
    assertThrows(
        EntityNotStorableException.class,
        () -> negotiationService.create(negotiationCreateDTO, 100L));
    verify(userNotificationService, times(0)).notifyAdmins(negotiation);
  }

  @Test
  void testCreateNegotiation_fails_when_DataIntegrityViolationException() throws IOException {
    NegotiationCreateDTO negotiationCreateDTO = TestUtils.createNegotiation(Set.of("requestID"));
    Negotiation negotiation = new Negotiation();
    Request request = new Request();
    request.setResources(Set.of(new Resource()));
    negotiation.setResources(request.getResources());
    when(requestRepository.findAllById(Set.of("requestID"))).thenReturn(List.of(request));
    when(modelMapper.map(negotiationCreateDTO, Negotiation.class)).thenReturn(negotiation);
    when(negotiationRepository.save(negotiation)).thenThrow(DataIntegrityViolationException.class);
    assertThrows(
        EntityNotStorableException.class,
        () -> negotiationService.create(negotiationCreateDTO, 100L));
    verify(userNotificationService, times(0)).notifyAdmins(negotiation);
  }

  @Test
  void findAllWithCurrentState_stateIsSubmitted_Ok() {
    Negotiation negotiation = buildNegotiation();
    NegotiationDTO negotiationDTO =
        NegotiationDTO.builder()
            .id(negotiation.getId())
            .status(negotiation.getCurrentState().name())
            .build();
    when(modelMapper.map(negotiation, NegotiationDTO.class)).thenReturn(negotiationDTO);
    when(negotiationRepository.findAll(any(Specification.class))).thenReturn(List.of(negotiation));
    assertEquals(1, negotiationService.findAllWithCurrentState(NegotiationState.SUBMITTED).size());
    assertEquals(
        NegotiationState.SUBMITTED.name(),
        negotiationService.findAllWithCurrentState(NegotiationState.SUBMITTED).get(0).getStatus());
  }

  @Test
  public void updatePrivatePostStatus_to_true() {
    Negotiation negotiation = buildNegotiation();
    String negotiationId = "biobank:1:collection:1";
    negotiation.setId(negotiationId);
    when(negotiationRepository.findById(any())).thenReturn(Optional.of(negotiation));
    negotiation.setPrivatePostsEnabled(false);
    negotiationService.setPrivatePostsEnabled(negotiationId, true);
    assertTrue(negotiation.isPrivatePostsEnabled());
  }

  @Test
  public void updatePrivatePostStatus_to_false() {
    Negotiation negotiation = buildNegotiation();
    String negotiationId = "biobank:1:collection:1";
    negotiation.setId(negotiationId);
    when(negotiationRepository.findById(any())).thenReturn(Optional.of(negotiation));
    negotiation.setPrivatePostsEnabled(true);
    negotiationService.setPrivatePostsEnabled(negotiationId, false);
    assertFalse(negotiation.isPrivatePostsEnabled());
  }

  @Test
  public void updatePublicPostStatus_to_false() {
    Negotiation negotiation = buildNegotiation();
    String negotiationId = "biobank:1:collection:1";
    negotiation.setId(negotiationId);
    when(negotiationRepository.findById(any())).thenReturn(Optional.of(negotiation));
    negotiation.setPublicPostsEnabled(true);
    negotiationService.setPublicPostsEnabled(negotiationId, false);
    assertFalse(negotiation.isPublicPostsEnabled());
  }

  @Test
  public void updateAllPostStatus_to_false() {
    Negotiation negotiation = buildNegotiation();
    String negotiationId = "biobank:1:collection:1";
    negotiation.setId(negotiationId);
    when(negotiationRepository.findById(any())).thenReturn(Optional.of(negotiation));
    negotiation.setPublicPostsEnabled(true);
    negotiation.setPrivatePostsEnabled(true);
    negotiationService.setPublicPostsEnabled(negotiationId, false);
    negotiationService.setPrivatePostsEnabled(negotiationId, false);
    assertFalse(negotiation.isPublicPostsEnabled());
    assertFalse(negotiation.isPrivatePostsEnabled());
  }
}
