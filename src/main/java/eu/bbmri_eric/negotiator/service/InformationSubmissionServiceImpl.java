package eu.bbmri_eric.negotiator.service;

import eu.bbmri_eric.negotiator.configuration.security.auth.NegotiatorUserDetailsService;
import eu.bbmri_eric.negotiator.database.model.InformationRequirement;
import eu.bbmri_eric.negotiator.database.model.InformationSubmission;
import eu.bbmri_eric.negotiator.database.model.Negotiation;
import eu.bbmri_eric.negotiator.database.model.Person;
import eu.bbmri_eric.negotiator.database.model.Resource;
import eu.bbmri_eric.negotiator.database.repository.InformationRequirementRepository;
import eu.bbmri_eric.negotiator.database.repository.InformationSubmissionRepository;
import eu.bbmri_eric.negotiator.database.repository.NegotiationRepository;
import eu.bbmri_eric.negotiator.database.repository.PersonRepository;
import eu.bbmri_eric.negotiator.database.repository.ResourceRepository;
import eu.bbmri_eric.negotiator.dto.InformationSubmissionDTO;
import eu.bbmri_eric.negotiator.dto.SubmittedInformationDTO;
import eu.bbmri_eric.negotiator.events.InformationSubmissionEvent;
import eu.bbmri_eric.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.exceptions.ForbiddenRequestException;
import eu.bbmri_eric.negotiator.exceptions.WrongRequestException;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
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
  private final PersonRepository personRepository;

  public InformationSubmissionServiceImpl(
      InformationSubmissionRepository informationSubmissionRepository,
      InformationRequirementRepository informationRequirementRepository,
      ResourceRepository resourceRepository,
      NegotiationRepository negotiationRepository,
      ModelMapper modelMapper,
      ApplicationEventPublisher applicationEventPublisher,
      PersonRepository personRepository) {
    this.informationSubmissionRepository = informationSubmissionRepository;
    this.informationRequirementRepository = informationRequirementRepository;
    this.resourceRepository = resourceRepository;
    this.negotiationRepository = negotiationRepository;
    this.modelMapper = modelMapper;
    this.applicationEventPublisher = applicationEventPublisher;
    this.personRepository = personRepository;
  }

  @Override
  public SubmittedInformationDTO submit(
      InformationSubmissionDTO informationSubmissionDTO,
      Long informationRequirementId,
      String negotiationId) {
    verifyAuthorization(informationSubmissionDTO);
    InformationSubmission submission =
        buildSubmissionEntity(informationSubmissionDTO, informationRequirementId, negotiationId);
    return saveInformationSubmission(informationRequirementId, negotiationId, submission);
  }

  @Override
  public SubmittedInformationDTO findById(Long id) {
    InformationSubmission submission =
        informationSubmissionRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException(id));
    verifyReadAuthorization(submission);
    return modelMapper.map(submission, SubmittedInformationDTO.class);
  }

  private void verifyReadAuthorization(InformationSubmission submission) {
    if (!isAuthorizedToRead(
        submission.getNegotiation().getId(),
        NegotiatorUserDetailsService.getCurrentlyAuthenticatedUserInternalId(),
        submission.getResource().getId())) {
      throw new ForbiddenRequestException("You are not authorized to perform this action");
    }
  }

  @Override
  public List<SubmittedInformationDTO> findAllForNegotiation(String negotiationId) {
    return informationSubmissionRepository.findAllByNegotiation_Id(negotiationId).stream()
        .map(
            informationSubmission ->
                modelMapper.map(informationSubmission, SubmittedInformationDTO.class))
        .toList();
  }

  private SubmittedInformationDTO saveInformationSubmission(
      Long informationRequirementId, String negotiationId, InformationSubmission submission) {
    if (informationSubmissionRepository.existsByResource_SourceIdAndNegotiation_IdAndRequirement_Id(
        submission.getResource().getSourceId(), negotiationId, informationRequirementId)) {
      throw new WrongRequestException(
          "The required information for this resource was already provided");
    }
    submission = informationSubmissionRepository.saveAndFlush(submission);
    applicationEventPublisher.publishEvent(new InformationSubmissionEvent(this, negotiationId));
    return modelMapper.map(submission, SubmittedInformationDTO.class);
  }

  private void verifyAuthorization(InformationSubmissionDTO informationSubmissionDTO) {
    if (!isAuthorizedToWrite(
        NegotiatorUserDetailsService.getCurrentlyAuthenticatedUserInternalId(),
        informationSubmissionDTO.getResourceId())) {
      throw new ForbiddenRequestException("You are not authorized to perform this action");
    }
  }

  private boolean isAuthorizedToWrite(Long personId, Long resourceId) {
    Optional<Person> personOpt = personRepository.findById(personId);
    if (personOpt.isPresent()) {
      Person person = personOpt.get();
      return person.getResources().stream()
          .anyMatch(resource -> resource.getId().equals(resourceId));
    }
    return false;
  }

  private boolean isAuthorizedToRead(String negotiationId, Long personId, Long resourceId) {
    return isAuthorizedToWrite(personId, resourceId)
        || negotiationRepository.existsByIdAndCreatedBy_Id(negotiationId, personId);
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
}
