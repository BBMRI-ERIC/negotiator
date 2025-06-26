package eu.bbmri_eric.negotiator.info_submission;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.negotiator.common.AuthenticatedUserContext;
import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.common.exceptions.ForbiddenRequestException;
import eu.bbmri_eric.negotiator.common.exceptions.WrongRequestException;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.governance.resource.ResourceRepository;
import eu.bbmri_eric.negotiator.info_requirement.InformationRequirement;
import eu.bbmri_eric.negotiator.info_requirement.InformationRequirementRepository;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.user.Person;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import jakarta.transaction.Transactional;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.NonNull;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
    if (informationSubmissionRepository.existsByResource_SourceIdAndNegotiation_IdAndRequirement_Id(
        submission.getResource().getSourceId(), negotiationId, informationRequirementId)) {
      throw new WrongRequestException(
          "The required information for this resource was already provided");
    }
    return saveInformationSubmission(negotiationId, submission);
  }

  @Override
  public SubmittedInformationDTO updateSubmission(
      InformationSubmissionDTO informationSubmissionDTO, Long submissionId) {
    verifyAuthorization(informationSubmissionDTO);
    InformationSubmission submission =
        informationSubmissionRepository
            .findById(submissionId)
            .orElseThrow(() -> new EntityNotFoundException(submissionId));
    if (!submission.isEditable() && !AuthenticatedUserContext.isCurrentlyAuthenticatedUserAdmin()) {
      throw new WrongRequestException(
          "The information has been formally submitted and cannot be updated.");
    }
    if (Objects.nonNull(informationSubmissionDTO.getPayload())) {
      submission.setPayload(informationSubmissionDTO.getPayload().toString());
    }
    submission.setEditable(informationSubmissionDTO.isEditable());
    return saveInformationSubmission(submission.getNegotiation().getId(), submission);
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
        submission, AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId())) {
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

  @Override
  public MultipartFile createSummary(Long requirementId, String negotiationId) {
    if (negotiationRepository.existsByIdAndCreatedBy_Id(
            negotiationId, AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId())
        || AuthenticatedUserContext.isCurrentlyAuthenticatedUserAdmin()) {
      InformationRequirement requirement =
          informationRequirementRepository
              .findById(requirementId)
              .orElseThrow(() -> new EntityNotFoundException(requirementId));
      if (requirement.isViewableOnlyByAdmin()
          && !AuthenticatedUserContext.isCurrentlyAuthenticatedUserAdmin()) {
        throw new ForbiddenRequestException("You are not authorized to perform this action");
      }
      List<InformationSubmission> allSubmissions =
          informationSubmissionRepository.findAllByRequirement_IdAndNegotiation_Id(
              requirementId, negotiationId);
      String name = requirement.getRequiredAccessForm().getName();
      return generateCSVFile(allSubmissions, "%s-summary.csv".formatted(name));
    }
    throw new ForbiddenRequestException("You are not authorized to perform this action");
  }

  private SubmittedInformationDTO saveInformationSubmission(
      String negotiationId, InformationSubmission submission) {
    submission = informationSubmissionRepository.saveAndFlush(submission);
    applicationEventPublisher.publishEvent(new InformationSubmissionEvent(this, negotiationId));
    return modelMapper.map(submission, SubmittedInformationDTO.class);
  }

  private MultipartFile generateCSVFile(List<InformationSubmission> submissions, String fileName) {
    ObjectMapper objectMapper = new ObjectMapper();
    Set<String> jsonKeys = generatedHeadersFromResponses(submissions, objectMapper);
    List<String> headers = setHeaders(jsonKeys);
    if (submissions.isEmpty()) {
      headers = new ArrayList<>();
    }
    ByteArrayOutputStream byteArrayOutputStream =
        createCSVAsByteArray(submissions, headers, objectMapper, jsonKeys);
    return new MockMultipartFile(
        fileName, fileName, "text/csv", byteArrayOutputStream.toByteArray());
  }

  private static @NonNull List<String> setHeaders(Set<String> jsonKeys) {
    List<String> headers = new ArrayList<>();
    headers.add("resourceId");
    headers.addAll(jsonKeys);
    return headers;
  }

  private static @NonNull ByteArrayOutputStream createCSVAsByteArray(
      List<InformationSubmission> submissions,
      List<String> headers,
      ObjectMapper objectMapper,
      Set<String> jsonKeys) {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    if (submissions.isEmpty()) return byteArrayOutputStream;
    try (CSVPrinter printer =
        new CSVPrinter(
            new OutputStreamWriter(byteArrayOutputStream),
            CSVFormat.DEFAULT.withHeader(headers.toArray(new String[0])))) {
      for (InformationSubmission submission : submissions) {
        buildRow(objectMapper, jsonKeys, submission, printer);
      }
    } catch (Exception e) {
      log.error("Could not generate CSV file", e);
      throw new InternalError("Could not generate the CSV file. Please try again later");
    }
    return byteArrayOutputStream;
  }

  private static void buildRow(
      ObjectMapper objectMapper,
      Set<String> jsonKeys,
      InformationSubmission submission,
      CSVPrinter printer)
      throws IOException {
    List<String> row = new ArrayList<>();
    row.add(submission.getResource().getSourceId());
    JsonNode payload = objectMapper.readTree(submission.getPayload());
    Map<String, String> flattenedPayload = flattenJson(payload);
    for (String key : jsonKeys) {
      String value = flattenedPayload.getOrDefault(key, "");
      row.add(value);
    }
    printer.printRecord(row);
  }

  private static @NonNull Set<String> generatedHeadersFromResponses(
      List<InformationSubmission> submissions, ObjectMapper objectMapper) {
    Set<String> jsonKeys = new LinkedHashSet<>();
    for (InformationSubmission submission : submissions) {
      JsonNode payload = null;
      try {
        payload = objectMapper.readTree(submission.getPayload());
      } catch (JsonProcessingException e) {
        log.error("Could not generate JSON payload", e);
        throw new RuntimeException(e);
      }
      Map<String, String> flattenedPayload = flattenJson(payload);
      jsonKeys.addAll(flattenedPayload.keySet());
    }
    return jsonKeys;
  }

  private static Map<String, String> flattenJson(JsonNode node) {
    Map<String, String> flattenedMap = new LinkedHashMap<>();
    flattenJsonHelper("", node, flattenedMap);
    return flattenedMap;
  }

  private static void flattenJsonHelper(
      String prefix, JsonNode node, Map<String, String> flattenedMap) {
    if (node.isObject()) {
      node.fieldNames()
          .forEachRemaining(
              field -> {
                String newPrefix = prefix.isEmpty() ? field : prefix + "." + field;
                flattenJsonHelper(newPrefix, node.get(field), flattenedMap);
              });
    } else if (node.isValueNode()) {
      flattenedMap.put(prefix, node.asText());
    }
  }

  private void verifyAuthorization(InformationSubmissionDTO informationSubmissionDTO) {
    if (!isAuthorizedToWrite(
        AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId(),
        informationSubmissionDTO.getResourceId())) {
      throw new ForbiddenRequestException("You are not authorized to perform this action");
    }
  }

  private boolean isAuthorizedToWrite(Long personId, Long resourceId) {
    Optional<Person> personOpt = personRepository.findById(personId);
    if (personOpt.isPresent()) {
      Person person = personOpt.get();
      // If the user is an admin, they can write to any resource
      if (AuthenticatedUserContext.isCurrentlyAuthenticatedUserAdmin()) {
        return true;
      }
      // Check if the person is a representative of the resource
      return person.getResources().stream()
          .anyMatch(resource -> resource.getId().equals(resourceId));
    }
    return false;
  }

  private boolean isAuthorizedToRead(InformationSubmission submission, Long personId) {
    boolean isRepresentative = isAuthorizedToWrite(personId, submission.getResource().getId());
    if (isOnlyForAdmin(submission)
        && (AuthenticatedUserContext.isCurrentlyAuthenticatedUserAdmin() || isRepresentative)) {
      return true;
    }
    return isRepresentative
        || negotiationRepository.existsByIdAndCreatedBy_Id(
            submission.getNegotiation().getId(), personId);
  }

  private boolean isOnlyForAdmin(InformationSubmission submission) {
    return (Objects.nonNull(submission.getRequirement())
        && submission.getRequirement().isViewableOnlyByAdmin());
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
        requirement,
        resource,
        negotiation,
        informationSubmissionDTO.getPayload().toString(),
        informationSubmissionDTO.isEditable());
  }
}
