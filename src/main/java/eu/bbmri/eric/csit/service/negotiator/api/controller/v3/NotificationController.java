package eu.bbmri.eric.csit.service.negotiator.api.controller.v3;

import eu.bbmri.eric.csit.service.negotiator.configuration.security.auth.NegotiatorUserDetailsService;
import eu.bbmri.eric.csit.service.negotiator.dto.NotificationDTO;
import eu.bbmri.eric.csit.service.negotiator.service.UserNotificationService;
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
public class NotificationController {

  @Autowired UserNotificationService userNotificationService;

  @GetMapping(value = "/notifications", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  public List<NotificationDTO> getNotificationsForUser() {
    return userNotificationService.getNotificationsForUser(
        NegotiatorUserDetailsService.getCurrentlyAuthenticatedUserInternalId());
  }
}
