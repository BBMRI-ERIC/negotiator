package eu.bbmri_eric.negotiator.service;

import eu.bbmri_eric.negotiator.database.model.InformationRequirement;
import eu.bbmri_eric.negotiator.database.model.InformationSubmission;
import eu.bbmri_eric.negotiator.database.model.Negotiation;
import eu.bbmri_eric.negotiator.database.model.Resource;
import eu.bbmri_eric.negotiator.database.repository.InformationRequirementRepository;
import eu.bbmri_eric.negotiator.database.repository.InformationSubmissionRepository;
import eu.bbmri_eric.negotiator.database.repository.NegotiationRepository;
import eu.bbmri_eric.negotiator.database.repository.ResourceRepository;
import eu.bbmri_eric.negotiator.dto.InformationSubmissionDTO;
import eu.bbmri_eric.negotiator.dto.SubmittedInformationDTO;
import eu.bbmri_eric.negotiator.events.InformationSubmissionEvent;
import eu.bbmri_eric.negotiator.exceptions.EntityNotFoundException;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.NonNull;
import lombok.extern.apachecommons.CommonsLog;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@Transactional
@CommonsLog
public class InformationSubmissionServiceImpl implements InformationSubmissionService {

  private final InformationSubmissionRepository informationSubmissionRepository;
  private final InformationRequirementRepository informationRequirementRepository;
  private final ResourceRepository resourceRepository;
  private final NegotiationRepository negotiationRepository;
  private final ModelMapper modelMapper;
  private final ApplicationEventPublisher applicationEventPublisher;

  public InformationSubmissionServiceImpl(
      InformationSubmissionRepository informationSubmissionRepository,
      InformationRequirementRepository informationRequirementRepository,
      ResourceRepository resourceRepository,
      NegotiationRepository negotiationRepository,
      ModelMapper modelMapper,
      ApplicationEventPublisher applicationEventPublisher) {
    this.informationSubmissionRepository = informationSubmissionRepository;
    this.informationRequirementRepository = informationRequirementRepository;
    this.resourceRepository = resourceRepository;
    this.negotiationRepository = negotiationRepository;
    this.modelMapper = modelMapper;
    this.applicationEventPublisher = applicationEventPublisher;
  }

  @Override
  public SubmittedInformationDTO submit(
      InformationSubmissionDTO informationSubmissionDTO,
      Long informationRequirementId,
      String negotiationId) {
    InformationSubmission submission =
        buildSubmissionEntity(informationSubmissionDTO, informationRequirementId, negotiationId);
    submission = informationSubmissionRepository.saveAndFlush(submission);
    applicationEventPublisher.publishEvent(new InformationSubmissionEvent(this, negotiationId));
    return modelMapper.map(submission, SubmittedInformationDTO.class);
  }

  private @NonNull InformationSubmission buildSubmissionEntity(
      InformationSubmissionDTO informationSubmissionDTO,
      Long informationRequirementId,
      String negotiationId) {
    InformationRequirement requirement =
        informationRequirementRepository
            .findById(informationRequirementId)
            .orElseThrow(() -> new EntityNotFoundException(informationRequirementId));
    Resource resource =
        resourceRepository
            .findById(informationSubmissionDTO.getResourceId())
            .orElseThrow(
                () -> new EntityNotFoundException(informationSubmissionDTO.getResourceId()));
    Negotiation negotiation =
        negotiationRepository
            .findById(negotiationId)
            .orElseThrow(() -> new EntityNotFoundException(negotiationId));
    return new InformationSubmission(
        requirement, resource, negotiation, informationSubmissionDTO.getPayload().toString());
  }

  @Override
  public SubmittedInformationDTO findById(Long id) {
    return modelMapper.map(
        informationSubmissionRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException(id)),
        SubmittedInformationDTO.class);
  }

  @Override
  public List<SubmittedInformationDTO> findAllForNegotiation(String negotiationId) {
    return informationSubmissionRepository.findAllByNegotiation_Id(negotiationId).stream()
        .map(
            informationSubmission ->
                modelMapper.map(informationSubmission, SubmittedInformationDTO.class))
        .toList();
  }
}
