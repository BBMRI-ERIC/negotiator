package eu.bbmri_eric.negotiator.api.controller.v3;

import eu.bbmri_eric.negotiator.dto.InformationRequirementDTO;
import eu.bbmri_eric.negotiator.dto.InformationSubmissionDTO;
import eu.bbmri_eric.negotiator.dto.SubmittedInformationDTO;
import eu.bbmri_eric.negotiator.dto.negotiation.NegotiationDTO;
import eu.bbmri_eric.negotiator.service.InformationRequirementService;
import eu.bbmri_eric.negotiator.service.NegotiationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
    value = InformationSubmissionController.BASE_URL,
    produces = MediaTypes.HAL_JSON_VALUE)
@Tag(
    name = "Submit required information",
    description = "Submit required information on behalf of a resource in a Negotiation.")
@SecurityRequirement(name = "security_auth")
public class InformationSubmissionController {
  private final InformationRequirementService requirementService;
  private final NegotiationService negotiationService;
  public static final String BASE_URL = "/v3";

  public InformationSubmissionController(
      InformationRequirementService requirementService, NegotiationService negotiationService) {
    this.requirementService = requirementService;
    this.negotiationService = negotiationService;
  }

  @ResponseStatus(HttpStatus.OK)
  @GetMapping("/negotiations/{negotiationId}/info-requirements/{requirementId}")
  public String getSummaryInformation(
      @PathVariable String negotiationId, @PathVariable Long requirementId) {
    NegotiationDTO negotiationDTO = negotiationService.findById(negotiationId, false);
    InformationRequirementDTO requirementDTO =
        requirementService.getInformationRequirement(requirementId);
    return "{}";
  }

  @ResponseStatus(HttpStatus.OK)
  @PostMapping("/negotiations/{negotiationId}/info-requirements/{requirementId}")
  public EntityModel<SubmittedInformationDTO> submitInformation(
      @PathVariable String negotiationId,
      @PathVariable Long requirementId,
      @RequestBody InformationSubmissionDTO dto) {
    return EntityModel.of(new SubmittedInformationDTO(1L, dto.getResourceId(), dto.getPayload()));
  }
}
