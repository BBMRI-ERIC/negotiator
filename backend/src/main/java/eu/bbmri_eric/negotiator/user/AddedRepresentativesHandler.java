package eu.bbmri_eric.negotiator.user;

import eu.bbmri_eric.negotiator.email.EmailService;
import eu.bbmri_eric.negotiator.notification.EmailContextBuilder;
import java.util.List;
import java.util.Map;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@CommonsLog
public class AddedRepresentativesHandler {

  private final EmailService emailService;
  private final EmailContextBuilder emailContextBuilder;

  public AddedRepresentativesHandler(
      EmailService emailService, EmailContextBuilder emailContextBuilder) {
    this.emailService = emailService;
    this.emailContextBuilder = emailContextBuilder;
  }

  @Async
  public void notifyAddedRepresentatives(Map<Long, List<AddedRepresentativeEvent>> events) {
    events.forEach(this::sendAddedRepresentativeEmail);
  }

  private void sendAddedRepresentativeEmail(
      Long representativeId, List<AddedRepresentativeEvent> events) {
    String emailTitle = "You have been added as a representative for one or more resources";
    StringBuilder emailText =
        new StringBuilder("You have been added as a representative for the following resources:\n");
    for (AddedRepresentativeEvent event : events) {
      emailText
          .append("- Resource ID: ")
          .append(event.getResourceId())
          .append(" (Source: ")
          .append(event.getSourceId())
          .append(")\n");
    }
    emailText.append(
        "\nPlease log in to the BBMRI Negotiator to review all the ongoing negotiations involving these resources.");

    String emailContent =
        emailContextBuilder.buildEmailContent(
            events.getFirst().getRepresentativeName(),
            emailText.toString(),
            null,
            emailTitle,
            null);

    log.info(
        "******Sending email to representative "
            + representativeId
            + " for "
            + events.size()
            + " added resources.");
    emailService.sendEmail(events.getFirst().getRepresentativeEmail(), emailTitle, emailContent);
  }
}
