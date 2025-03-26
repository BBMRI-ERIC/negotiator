package eu.bbmri_eric.negotiator.notification;

import eu.bbmri_eric.negotiator.common.AuthenticatedUserContext;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v3")
@CommonsLog
@Tag(name = "Notifications", description = "Retrieve and update user notifications")
@SecurityRequirement(name = "security_auth")
public class NotificationController {

  @Autowired UserNotificationService userNotificationService;

  @GetMapping(value = "/notifications", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  public List<NotificationDTO> getNotificationsForUser() {
    return userNotificationService.getNotificationsForUser(
        AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId());
  }

  @GetMapping(value = "/notifications/template/{templateName}", produces = MediaType.APPLICATION_XHTML_XML_VALUE)
  @ResponseStatus(HttpStatus.OK)
  public String getNotificationTemplate(@PathVariable String templateName) {
    return userNotificationService.getNotificationTemplate(templateName);
  }

  @PostMapping(value = "/notifications/template/{templateName}", produces = MediaType.APPLICATION_XHTML_XML_VALUE)
  @ResponseStatus(HttpStatus.OK)
  public String updateNotificationTemplate(@PathVariable String templateName, @RequestBody String template) {
    return userNotificationService.updateNotificationTemplate(templateName, template);
  }

  @PostMapping(value = "/notifications/template/{templateName}/reset", produces = MediaType.APPLICATION_XHTML_XML_VALUE)
  @ResponseStatus(HttpStatus.OK)
  public String updateNotificationTemplate(@PathVariable String templateName) {
    return userNotificationService.resetNotificationTemplate(templateName);
  }
}
