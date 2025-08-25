package eu.bbmri_eric.negotiator.template;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/v3")
@CommonsLog
@Tag(name = "Email Templates", description = "Retrieve and update templates")
@SecurityRequirement(name = "security_auth")
public class TemplateController {

  TemplateService templateService;

  public TemplateController(TemplateService templateService) {
    this.templateService = templateService;
  }

  @GetMapping("/templates")
  @Operation(
      summary = "Get all notification templates",
      description = "Get all notification templates")
  @ResponseStatus(HttpStatus.OK)
  public List<String> getAllNotificationTemplates() {
    return templateService.getAll();
  }

  @GetMapping(value = "/templates/{templateName}", produces = MediaType.APPLICATION_XHTML_XML_VALUE)
  @Operation(
      summary = "Get a notification template",
      description = "Get a notification template by name")
  @ResponseStatus(HttpStatus.OK)
  public String getNotificationTemplate(@PathVariable String templateName) {
    return templateService.getByName(templateName);
  }

  @PutMapping(value = "/templates/{templateName}", produces = MediaType.APPLICATION_XHTML_XML_VALUE)
  @Operation(
      summary = "Update a notification template",
      description = "Update the content of a notification template by name")
  @ResponseStatus(HttpStatus.OK)
  public String updateNotificationTemplate(
      @PathVariable String templateName, @RequestBody String template) {
    return templateService.updateTemplate(templateName, template);
  }

  @PostMapping(
      value = "/templates/{templateName}/operations",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_XHTML_XML_VALUE)
  @Operation(
      summary = "Reset a notification template",
      description = "Reset a notification template to the default content")
  @ResponseStatus(HttpStatus.OK)
  public String resetNotificationTemplate(
      @PathVariable String templateName, @RequestBody TemplateOperationRequest request) {

    if (request.getOperation() == TemplateOperationRequest.Operation.RESET) {
      return templateService.resetTemplate(templateName);
    }
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid operation");
  }
}
