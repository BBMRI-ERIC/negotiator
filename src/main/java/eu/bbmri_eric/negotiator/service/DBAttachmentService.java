package eu.bbmri_eric.negotiator.service;

import eu.bbmri_eric.negotiator.configuration.security.auth.NegotiatorUserDetailsService;
import eu.bbmri_eric.negotiator.database.model.Attachment;
import eu.bbmri_eric.negotiator.database.model.Negotiation;
import eu.bbmri_eric.negotiator.database.model.Organization;
import eu.bbmri_eric.negotiator.database.model.Resource;
import eu.bbmri_eric.negotiator.database.repository.AttachmentRepository;
import eu.bbmri_eric.negotiator.database.repository.NegotiationRepository;
import eu.bbmri_eric.negotiator.database.repository.OrganizationRepository;
import eu.bbmri_eric.negotiator.dto.attachments.AttachmentDTO;
import eu.bbmri_eric.negotiator.dto.attachments.AttachmentMetadataDTO;
import eu.bbmri_eric.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.exceptions.ForbiddenRequestException;
import java.io.IOException;
import java.util.List;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service(value = "DefaultAttachmentService")
public class DBAttachmentService implements AttachmentService {

  @Autowired private final AttachmentRepository attachmentRepository;
  @Autowired private final ModelMapper modelMapper;
  @Autowired private final NegotiationRepository negotiationRepository;
  @Autowired private OrganizationRepository organizationRepository;
  @Autowired private PersonService personService;
  @Autowired private NegotiationService negotiationService;

  @Autowired
  public DBAttachmentService(
      AttachmentRepository attachmentRepository,
      NegotiationRepository negotiationRepository,
      PersonService personService,
      NegotiationService negotiationService,
      ModelMapper modelMapper) {
    this.attachmentRepository = attachmentRepository;
    this.negotiationRepository = negotiationRepository;
    this.modelMapper = modelMapper;
    this.personService = personService;
    this.negotiationService = negotiationService;
  }

  @Override
  @Transactional
  public AttachmentMetadataDTO createForNegotiation(
      String negotiationId, @Nullable String organizationId, MultipartFile file) {

    Attachment attachment;
    Negotiation negotiation =
        negotiationRepository
            .findById(negotiationId)
            .orElseThrow(() -> new EntityNotFoundException(negotiationId));

    Organization organization;
    if (organizationId != null) {
      organization =
          organizationRepository
              .findByExternalId(organizationId)
              .orElseThrow(() -> new EntityNotFoundException(organizationId));

    } else {
      organization = null;
    }

    try {
      attachment =
          Attachment.builder()
              .name(file.getOriginalFilename())
              .payload(file.getBytes())
              .contentType(file.getContentType())
              .size(file.getSize())
              .negotiation(negotiation)
              .organization(organization)
              .build();

      Attachment saved = attachmentRepository.save(attachment);
      return modelMapper.map(saved, AttachmentMetadataDTO.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  @Transactional
  public AttachmentMetadataDTO create(MultipartFile file) {
    Attachment attachment;
    try {
      attachment =
          Attachment.builder()
              .name(file.getOriginalFilename())
              .payload(file.getBytes())
              .contentType(file.getContentType())
              .size(file.getSize())
              .build();

      Attachment saved = attachmentRepository.save(attachment);
      return modelMapper.map(saved, AttachmentMetadataDTO.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  @Transactional
  public AttachmentMetadataDTO findMetadataById(String id) {
    Attachment attachment =
        attachmentRepository
            .findMetadataById(id)
            .orElseThrow(() -> new EntityNotFoundException(id));
    if (!isAuthorizedForAttachment(attachment)) {
      throw new ForbiddenRequestException();
    }
    return modelMapper.map(attachment, AttachmentMetadataDTO.class);
  }

  @Override
  @Transactional
  public AttachmentDTO findById(String id) {
    Attachment attachment =
        attachmentRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(id));
    if (!isAuthorizedForAttachment(attachment)) {
      throw new ForbiddenRequestException();
    }
    return modelMapper.map(attachment, AttachmentDTO.class);
  }

  @Override
  @Transactional
  public List<AttachmentMetadataDTO> findByNegotiation(String id) {
    List<Attachment> attachments = attachmentRepository.findByNegotiationId(id);
    return attachments.stream()
        .filter(this::isAuthorizedForAttachment)
        .map((attachment) -> modelMapper.map(attachment, AttachmentMetadataDTO.class))
        .toList();
  }

  @Override
  @Transactional
  public AttachmentMetadataDTO findByIdAndNegotiation(String id, String negotiationId) {
    Attachment attachment =
        attachmentRepository
            .findByIdAndNegotiationId(id, negotiationId)
            .orElseThrow(() -> new EntityNotFoundException(id));
    if (!this.isAuthorizedForAttachment(attachment)) {
      throw new ForbiddenRequestException();
    }
    return modelMapper.map(attachment, AttachmentMetadataDTO.class);
  }

  private boolean isRepresentative(Organization organization) {
    return personService.isRepresentativeOfAnyResource(
        NegotiatorUserDetailsService.getCurrentlyAuthenticatedUserInternalId(),
        organization.getResources().stream().map(Resource::getSourceId).toList());
  }

  private boolean isAdmin() {
    return NegotiatorUserDetailsService.isCurrentlyAuthenticatedUserAdmin();
  }

  private boolean isAuthorizedForAttachment(Attachment attachment) {
    // The administrator of the negotiator is authorized to all attachements
    if (isAdmin()) return true;

    Negotiation negotiation = attachment.getNegotiation();
    if (negotiation == null) {
      // If the attachment is not associated to a Negotiation yet, it can be accessed only by the
      // creator of the attachment
      return attachment.isCreator(
          NegotiatorUserDetailsService.getCurrentlyAuthenticatedUserInternalId());
    } else {
      // otherwise the user has to be authorized for the negotiation and
      // the attachment must be either:
      // 1. public (in the negotiation)
      // 2. created by the currently authenticated user
      // 3. addressed to the organization represented by the authenticated user
      return negotiationService.isAuthorizedForNegotiation(negotiation)
          && (attachment.isPublic()
              || attachment.isCreator(
                  NegotiatorUserDetailsService.getCurrentlyAuthenticatedUserInternalId())
              || (attachment.getOrganization() != null
                  && isRepresentative(attachment.getOrganization())));
    }
  }
}
