package eu.bbmri_eric.negotiator.notification;

import eu.bbmri_eric.negotiator.configuration.security.auth.NegotiatorUserDetailsService;
import eu.bbmri_eric.negotiator.dto.NotificationDTO;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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
        NegotiatorUserDetailsService.getCurrentlyAuthenticatedUserInternalId());
  }
}
