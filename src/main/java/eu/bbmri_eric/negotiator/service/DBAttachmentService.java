package eu.bbmri_eric.negotiator.service;

import eu.bbmri_eric.negotiator.configuration.security.auth.NegotiatorUserDetailsService;
import eu.bbmri_eric.negotiator.database.model.Attachment;
import eu.bbmri_eric.negotiator.database.model.Negotiation;
import eu.bbmri_eric.negotiator.database.model.Organization;
import eu.bbmri_eric.negotiator.database.model.views.AttachmentViewDTO;
import eu.bbmri_eric.negotiator.database.model.views.MetadataAttachmentViewDTO;
import eu.bbmri_eric.negotiator.database.repository.AttachmentRepository;
import eu.bbmri_eric.negotiator.database.repository.NegotiationRepository;
import eu.bbmri_eric.negotiator.database.repository.OrganizationRepository;
import eu.bbmri_eric.negotiator.database.repository.PersonRepository;
import eu.bbmri_eric.negotiator.dto.attachments.AttachmentDTO;
import eu.bbmri_eric.negotiator.dto.attachments.AttachmentMetadataDTO;
import eu.bbmri_eric.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.exceptions.EntityNotStorableException;
import eu.bbmri_eric.negotiator.exceptions.ForbiddenRequestException;
import eu.bbmri_eric.negotiator.exceptions.WrongRequestException;
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

  @Autowired private AttachmentRepository attachmentRepository;
  @Autowired private ModelMapper modelMapper;
  @Autowired private NegotiationRepository negotiationRepository;
  @Autowired private OrganizationRepository organizationRepository;
  @Autowired private PersonService personService;
  @Autowired private NegotiationService negotiationService;
  @Autowired private PersonRepository personRepository;

  @Override
  @Transactional
  public AttachmentMetadataDTO createForNegotiation(
      String negotiationId, @Nullable String organizationExternalId, MultipartFile file)
      throws WrongRequestException {
    if (organizationExternalId != null
        && !negotiationService.isOrganizationPartOfNegotiation(
            negotiationId, organizationExternalId)) {
      throw new WrongRequestException(
          "The organization specified is not involved in the negotiation");
    }

    Long userId = NegotiatorUserDetailsService.getCurrentlyAuthenticatedUserInternalId();
    checkAuthorization(userId, negotiationId, organizationExternalId);

    Negotiation negotiation = fetchNegotiation(negotiationId);
    Organization organization = fetchAddressedOrganization(organizationExternalId);
    return saveAttachment(file, negotiation, organization);
  }

  @Override
  @Transactional
  public AttachmentMetadataDTO create(MultipartFile file) {
    return saveAttachment(file, null, null);
  }

  @Override
  @Transactional
  public AttachmentDTO findById(String id) {
    AttachmentViewDTO attachment =
        attachmentRepository.findAllById(id).orElseThrow(() -> new EntityNotFoundException(id));
    if (!isAuthorizedForAttachment(attachment)) {
      throw new ForbiddenRequestException();
    }
    return modelMapper.map(attachment, AttachmentDTO.class);
  }

  @Override
  @Transactional
  public List<AttachmentMetadataDTO> findByNegotiation(String negotiationId) {
    if (!negotiationService.exists(negotiationId)) {
      throw new EntityNotFoundException(
          "Negotiation with id %s does not exist".formatted(negotiationId));
    }
    List<MetadataAttachmentViewDTO> attachments =
        attachmentRepository.findByNegotiationId(negotiationId);
    return attachments.stream()
        .filter(this::isAuthorizedForAttachment)
        .map((attachment) -> modelMapper.map(attachment, AttachmentMetadataDTO.class))
        .toList();
  }

  @Override
  @Transactional
  public AttachmentMetadataDTO findByIdAndNegotiationId(String id, String negotiationId) {
    MetadataAttachmentViewDTO attachment =
        attachmentRepository
            .findMetadataByIdAndNegotiationId(id, negotiationId)
            .orElseThrow(() -> new EntityNotFoundException(id));
    if (!this.isAuthorizedForAttachment(attachment)) {
      throw new ForbiddenRequestException();
    }
    return modelMapper.map(attachment, AttachmentMetadataDTO.class);
  }

  private Negotiation fetchNegotiation(String negotiationId) {
    return negotiationRepository
        .findById(negotiationId)
        .orElseThrow(() -> new EntityNotFoundException(negotiationId));
  }

  @Nullable
  private Organization fetchAddressedOrganization(@Nullable String externalId) {
    Organization organization = null;
    if (externalId != null) {
      organization =
          organizationRepository
              .findByExternalId(externalId)
              .orElseThrow(() -> new EntityNotFoundException(externalId));
    }
    return organization;
  }

  private AttachmentMetadataDTO saveAttachment(
      MultipartFile file, Negotiation negotiation, Organization organization) {
    Attachment attachment;
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
      throw new EntityNotStorableException("The attachment could not be stored.");
    }
  }

  private void checkAuthorization(Long userId, String negotiationId, String organizationExternalId)
      throws ForbiddenRequestException {
    // if the recipient organization is specified and the sender is not the negotiation creator,
    // checks whether the user is representative of the organization
    if (organizationExternalId != null
        && !negotiationService.isNegotiationCreator(negotiationId)
        && !isRepresentativeOfOrganization(organizationExternalId)) {
      throw new ForbiddenRequestException(
          "User %s is not authorized to upload attachments to this organization for this negotiation"
              .formatted(userId));
    }

    if (!negotiationService.isAuthorizedForNegotiation(negotiationId)) {
      throw new ForbiddenRequestException(
          "User %s is not authorized to upload attachments for this negotiation."
              .formatted(userId));
    }
  }

  private boolean isRepresentativeOfOrganization(Long organizationId) {
    return personService.isRepresentativeOfAnyResourceOfOrganization(
        NegotiatorUserDetailsService.getCurrentlyAuthenticatedUserInternalId(), organizationId);
  }

  private boolean isRepresentativeOfOrganization(String organizationExternalId) {
    return personService.isRepresentativeOfAnyResourceOfOrganization(
        NegotiatorUserDetailsService.getCurrentlyAuthenticatedUserInternalId(),
        organizationExternalId);
  }

  private boolean isAdmin() {
    return NegotiatorUserDetailsService.isCurrentlyAuthenticatedUserAdmin();
  }

  private boolean isAuthorizedForAttachment(MetadataAttachmentViewDTO attachment) {
    // The administrator of the negotiator is authorized to all attachements
    if (isAdmin()) return true;

    String negotiationId = attachment.getNegotiationId();
    if (negotiationId == null) {
      // If the attachment is not associated to a Negotiation yet, it can be accessed only by the
      // creator of the attachment
      return isCurrentAuthenticatedUserAttachmentCreator(attachment);
    } else {
      // otherwise the user has to be authorized for the negotiation and
      // the attachment must be either:
      // 1. public (in the negotiation)
      // 2. created by the currently authenticated user
      // 3. addressed to the organization represented by the authenticated user
      return negotiationService.isAuthorizedForNegotiation(negotiationId)
          && (isAttachmentPublic(attachment)
              || negotiationService.isNegotiationCreator(negotiationId)
              || isCurrentAuthenticatedUserAttachmentCreator(attachment)
              || (attachment.getOrganizationId() != null
                  && isRepresentativeOfOrganization(attachment.getOrganizationId())));
    }
  }

  boolean isCurrentAuthenticatedUserAttachmentCreator(MetadataAttachmentViewDTO attachment) {
    return attachment
        .getCreatedById()
        .equals(NegotiatorUserDetailsService.getCurrentlyAuthenticatedUserInternalId());
  }

  boolean isAttachmentPublic(MetadataAttachmentViewDTO attachment) {
    return attachment.getOrganizationId() == null;
  }
}
