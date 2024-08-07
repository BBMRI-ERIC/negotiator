package eu.bbmri_eric.negotiator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
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

  @Override
  public MultipartFile createSummary(Long requirementId, String negotiationId) {
    if (negotiationRepository.existsByIdAndCreatedBy_Id(
            negotiationId, NegotiatorUserDetailsService.getCurrentlyAuthenticatedUserInternalId())
        || NegotiatorUserDetailsService.isCurrentlyAuthenticatedUserAdmin()) {
      List<InformationSubmission> allSubmissions =
          informationSubmissionRepository.findAllByRequirement_IdAndNegotiation_Id(
              requirementId, negotiationId);
      String name =
          informationRequirementRepository
              .findById(requirementId)
              .orElseThrow(() -> new EntityNotFoundException(requirementId))
              .getRequiredAccessForm()
              .getName();
      return generateCSVFile(allSubmissions, "%s-summary.csv".formatted(name));
    }
    throw new ForbiddenRequestException("You are not authorized to perform this action");
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
    Set<String> jsonKeys = new TreeSet<>();
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
