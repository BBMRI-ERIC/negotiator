package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.configuration.auth.NegotiatorUserDetailsService;
import eu.bbmri.eric.csit.service.negotiator.database.model.Attachment;
import eu.bbmri.eric.csit.service.negotiator.database.model.Negotiation;
import eu.bbmri.eric.csit.service.negotiator.database.model.Request;
import eu.bbmri.eric.csit.service.negotiator.database.model.Resource;
import eu.bbmri.eric.csit.service.negotiator.database.repository.AttachmentRepository;
import eu.bbmri.eric.csit.service.negotiator.database.repository.NegotiationRepository;
import eu.bbmri.eric.csit.service.negotiator.dto.attachments.AttachmentDTO;
import eu.bbmri.eric.csit.service.negotiator.dto.attachments.AttachmentMetadataDTO;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service(value = "DefaultAttachmentService")
public class DBAttachmentService implements AttachmentService {

  @Autowired private final AttachmentRepository attachmentRepository;
  @Autowired private final ModelMapper modelMapper;
  @Autowired private final NegotiationRepository negotiationRepository;

  @Autowired
  public DBAttachmentService(
      AttachmentRepository attachmentRepository,
      NegotiationRepository negotiationRepository,
      ModelMapper modelMapper) {
    this.attachmentRepository = attachmentRepository;
    this.negotiationRepository = negotiationRepository;
    this.modelMapper = modelMapper;
  }

  @Override
  public AttachmentMetadataDTO createForNegotiation(String negotiationId, MultipartFile file) {
    Attachment attachment;
    try {
      Negotiation negotiation =
          negotiationRepository
              .findById(negotiationId)
              .orElseThrow(() -> new EntityNotFoundException(negotiationId));

      attachment =
          Attachment.builder()
              .name(file.getOriginalFilename())
              .payload(file.getBytes())
              .contentType(file.getContentType())
              .size(file.getSize())
              .negotiation(negotiation)
              .build();

      Attachment saved = attachmentRepository.save(attachment);
      return modelMapper.map(saved, AttachmentMetadataDTO.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
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
  public AttachmentMetadataDTO findMetadataById(String id) {
    Attachment attachment =
        attachmentRepository
            .findMetadataById(id)
            .orElseThrow(() -> new EntityNotFoundException(id));
    return modelMapper.map(attachment, AttachmentMetadataDTO.class);
  }

  @Override
  public AttachmentDTO findById(String id) {
    Attachment attachment =
        attachmentRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(id));
    if (!isAuthorizedForAttachment(attachment)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
    return modelMapper.map(attachment, AttachmentDTO.class);
  }

  @Override
  public List<AttachmentMetadataDTO> findByNegotiation(String id) {
    List<Attachment> attachments = attachmentRepository.findByNegotiationId(id);
    return attachments.stream()
        .map((attachment) -> modelMapper.map(attachment, AttachmentMetadataDTO.class))
        .toList();
  }

  @Override
  public AttachmentMetadataDTO findByIdAndNegotiation(String id, String negotiationId) {
    Attachment attachment =
        attachmentRepository
            .findByIdAndNegotiationId(id, negotiationId)
            .orElseThrow(() -> new EntityNotFoundException(id));
    return modelMapper.map(attachment, AttachmentMetadataDTO.class);
  }

  private boolean isCreator(Attachment attachment) {
    return Objects.equals(
        attachment.getCreatedBy().getId(),
        NegotiatorUserDetailsService.getCurrentlyAuthenticatedUserInternalId());
  }

  private boolean isNegotiationCreator(Negotiation negotiation) {
    return Objects.equals(
        negotiation.getCreatedBy().getId(),
        NegotiatorUserDetailsService.getCurrentlyAuthenticatedUserInternalId());
  }

  private List<String> getResourceIdsFromUserAuthorities() {
    List<String> resourceIds = new ArrayList<>();
    for (GrantedAuthority grantedAuthority :
        SecurityContextHolder.getContext().getAuthentication().getAuthorities()) {
      // Edit for different groups/resource types
      if (grantedAuthority.getAuthority().contains("collection")) {
        resourceIds.add(grantedAuthority.getAuthority());
      }
    }
    return Collections.unmodifiableList(resourceIds);
  }

  private boolean isRepresentative(Negotiation negotiation) {
    for (Request request : negotiation.getRequests()) {
      for (Resource resource : request.getResources()) {
        if (getResourceIdsFromUserAuthorities().contains(resource.getSourceId())) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean isAdmin() {
    return NegotiatorUserDetailsService.isCurrentlyAuthenticatedUserAdmin();
  }

  private boolean isAuthorizedForAttachment(Attachment attachment) {
    Negotiation negotiation = attachment.getNegotiation();
    if (negotiation == null) {
      return isCreator(attachment) || isAdmin();
    } else {
      return isCreator(attachment)
          || isNegotiationCreator(negotiation)
          || isRepresentative(negotiation)
          || isAdmin();
    }
  }
}
