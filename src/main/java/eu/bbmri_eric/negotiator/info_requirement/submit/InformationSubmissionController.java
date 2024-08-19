package eu.bbmri_eric.negotiator.info_requirement.submit;

import eu.bbmri_eric.negotiator.info_requirement.InformationRequirementService;
import eu.bbmri_eric.negotiator.negotiation.NegotiationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ServerErrorException;

@RestController
@RequestMapping(value = InformationSubmissionController.BASE_URL)
@Tag(
    name = "Submit required information",
    description = "Submit required information on behalf of a resource in a Negotiation.")
@SecurityRequirement(name = "security_auth")
public class InformationSubmissionController {
  private final InformationRequirementService requirementService;
  private final NegotiationService negotiationService;
  private final InformationSubmissionService submissionService;
  public static final String BASE_URL = "/v3";

  public InformationSubmissionController(
      InformationRequirementService requirementService,
      NegotiationService negotiationService,
      InformationSubmissionService submissionService) {
    this.requirementService = requirementService;
    this.negotiationService = negotiationService;
    this.submissionService = submissionService;
  }

  @ResponseStatus(HttpStatus.OK)
  @GetMapping(
      value = "/negotiations/{negotiationId}/info-requirements/{requirementId}",
      produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  public ResponseEntity<byte[]> getSummaryInformation(
      @PathVariable String negotiationId, @PathVariable Long requirementId) {
    MultipartFile file = submissionService.createSummary(requirementId, negotiationId);
    try {
      return ResponseEntity.ok()
          .header("Content-Disposition", "attachment; filename=\"%s\"".formatted(file.getName()))
          .contentType(MediaType.valueOf("text/csv"))
          .body(file.getBytes());
    } catch (IOException e) {
      throw new ServerErrorException("Failed to create summary information", e);
    }
  }

  @ResponseStatus(HttpStatus.OK)
  @PostMapping(
      value = "/negotiations/{negotiationId}/info-requirements/{requirementId}",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaTypes.HAL_JSON_VALUE)
  public EntityModel<SubmittedInformationDTO> submitInformation(
      @PathVariable String negotiationId,
      @PathVariable Long requirementId,
      @RequestBody InformationSubmissionDTO dto) {
    return EntityModel.of(submissionService.submit(dto, requirementId, negotiationId));
  }

  @ResponseStatus(HttpStatus.OK)
  @GetMapping(value = "/info-submissions/{id}", produces = MediaTypes.HAL_JSON_VALUE)
  public EntityModel<SubmittedInformationDTO> getInfoSubmission(@PathVariable Long id) {
    return EntityModel.of(submissionService.findById(id));
  }
}
