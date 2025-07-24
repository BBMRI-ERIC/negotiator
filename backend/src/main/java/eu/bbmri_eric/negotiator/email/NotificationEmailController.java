package eu.bbmri_eric.negotiator.email;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.apachecommons.CommonsLog;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v3")
@CommonsLog
@Tag(name = "Notification Emails", description = "Endpoint for managing notification emails")
@SecurityRequirement(name = "security_auth")
class NotificationEmailController {

  private final NotificationEmailService notificationEmailService;
  private final NotificationEmailModelAssembler modelAssembler;

  NotificationEmailController(
      NotificationEmailService notificationEmailService,
      NotificationEmailModelAssembler modelAssembler) {
    this.notificationEmailService = notificationEmailService;
    this.modelAssembler = modelAssembler;
  }

  @GetMapping(value = "/emails", produces = MediaTypes.HAL_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  @Operation(
      summary = "Get all notification emails",
      description =
          "Retrieve all notification emails with optional filtering by email address and sent date range. "
              + "Supports pagination and sorting.")
  PagedModel<EntityModel<NotificationEmailDTO>> getAllNotificationEmails(
      @Valid @ParameterObject NotificationEmailFilterDTO filters) {
    Page<NotificationEmailDTO> emailPage = notificationEmailService.findAllWithFilters(filters);
    return modelAssembler.toPagedModel(emailPage, filters);
  }

  @GetMapping(value = "/emails/{id}", produces = MediaTypes.HAL_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  @Operation(
      summary = "Get notification email by ID",
      description = "Retrieve a specific notification email by its ID.")
  EntityModel<NotificationEmailDTO> getNotificationEmail(
      @Parameter(description = "Notification email ID", example = "1") @PathVariable Long id) {

    NotificationEmailDTO dto = notificationEmailService.findById(id);
    return modelAssembler.toModel(dto);
  }
}
