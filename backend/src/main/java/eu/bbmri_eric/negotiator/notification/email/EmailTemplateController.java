package eu.bbmri_eric.negotiator.notification.email;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v3")
@CommonsLog
@Tag(name = "Email Templates", description = "Retrieve and update email templates")
@SecurityRequirement(name = "security_auth")
public class EmailTemplateController {

    @Autowired
    EmailTemplateService emailTemplateService;

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

    @PostMapping(
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

    @PostMapping(
            value = "/email-templates/{templateName}/reset",
            produces = MediaType.APPLICATION_XHTML_XML_VALUE)
    @Operation(
            summary = "Reset a notification email-template",
            description = "Reset a notification email-template to the default content")
    @ResponseStatus(HttpStatus.OK)
    public String resetNotificationTemplate(@PathVariable String templateName) {
        return emailTemplateService.resetNotificationTemplate(templateName);
    }

}
