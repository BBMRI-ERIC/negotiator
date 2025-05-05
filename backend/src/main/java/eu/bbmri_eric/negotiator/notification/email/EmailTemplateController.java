package eu.bbmri_eric.negotiator.notification.email;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/v3")
@CommonsLog
@Tag(name = "Email Templates", description = "Retrieve and update email templates")
@SecurityRequirement(name = "security_auth")
public class EmailTemplateController {

  @Autowired EmailTemplateService emailTemplateService;

  @GetMapping("/email-templates")
  @Operation(
      summary = "Get all notification email-templates",
      description = "Get all notification email-templates")
  @ResponseStatus(HttpStatus.OK)
  public List<String> getAllNotificationTemplates() {
    return emailTemplateService.getAllNotificationTemplates();
  }

  @GetMapping(
      value = "/email-templates/{templateName}",
      produces = MediaType.APPLICATION_XHTML_XML_VALUE)
  @Operation(
      summary = "Get a notification email-template",
      description = "Get a notification email-template by name")
  @ResponseStatus(HttpStatus.OK)
  public String getNotificationTemplate(@PathVariable String templateName) {
    return emailTemplateService.getNotificationTemplate(templateName);
  }

  @PutMapping(
      value = "/email-templates/{templateName}",
      produces = MediaType.APPLICATION_XHTML_XML_VALUE)
  @Operation(
      summary = "Update a notification email-template",
      description = "Update the content of a notification email-template by name")
  @ResponseStatus(HttpStatus.OK)
  public String updateNotificationTemplate(
      @PathVariable String templateName, @RequestBody String template) {
    return emailTemplateService.updateNotificationTemplate(templateName, template);
  }

  @PatchMapping(
      value = "/email-templates/{templateName}",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_XHTML_XML_VALUE)
  @Operation(
      summary = "Reset a notification email-template",
      description = "Reset a notification email-template to the default content")
  @ResponseStatus(HttpStatus.OK)
  public String resetNotificationTemplate(
      @PathVariable String templateName, @RequestBody Map<String, Object> body) {
    if (body.size() != 1 || !Boolean.TRUE.equals(body.get("reset"))) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Only { \"reset\": true } is supported for this endpoint");
    }
    return emailTemplateService.resetNotificationTemplate(templateName);
  }
}
