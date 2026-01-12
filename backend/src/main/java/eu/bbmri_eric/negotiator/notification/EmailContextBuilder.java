package eu.bbmri_eric.negotiator.notification;

import eu.bbmri_eric.negotiator.template.TemplateService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EmailContextBuilder {
  public static final String TEMPLATE_NAME = "EMAIL";
  private final TemplateService templateService;
  private final String frontendUrl;
  private final String emailYoursSincerelyText;
  private final String emailHelpdeskHref;
  private final String logoURL;

  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("d.M.yyyy, HH:mm");

  public EmailContextBuilder(
      TemplateService templateService,
      @Value("${negotiator.frontend-url}") String frontendUrl,
      @Value("${negotiator.emailYoursSincerelyText}") String emailYoursSincerelyText,
      @Value("${negotiator.emailHelpdeskHref}") String emailHelpdeskHref,
      @Value("${negotiator.emailLogo}") String logoURL) {
    this.templateService = templateService;
    this.frontendUrl = frontendUrl;
    this.emailYoursSincerelyText = emailYoursSincerelyText;
    this.emailHelpdeskHref = emailHelpdeskHref;
    this.logoURL = logoURL;
  }

  @NonNull
  public String buildEmailContent(
      @NonNull String recipientName,
      @NonNull String message,
      String negotiationId,
      String negotiationTitle,
      LocalDateTime negotiationCreationDate) {

    Map<String, Object> variables = new HashMap<>();
    // DO NOT remove any variables without a good reason.
    // Any deployments without updated templates would miss them.
    // If you add or modify them mention it in the documentation.
    String emailButtonLink = frontendUrl;
    String emailButtonText = "Negotiator Login";
    variables.put("recipient", recipientName);
    variables.put("message", message);

    if (negotiationId != null && negotiationTitle != null && negotiationCreationDate != null) {
      emailButtonLink = frontendUrl + "/negotiations/" + negotiationId;
      emailButtonText = "View Details";
      variables.put("negotiation", negotiationId);
      variables.put("titleForNegotiation", negotiationTitle);
      variables.put("date", negotiationCreationDate.format(DATE_TIME_FORMATTER));
    }
    variables.put("frontendUrl", frontendUrl);
    variables.put("emailYoursSincerelyText", emailYoursSincerelyText);
    variables.put("emailHelpdeskHref", emailHelpdeskHref);
    variables.put("logoUrl", logoURL);
    variables.put("emailButtonLink", emailButtonLink);
    variables.put("emailButtonText", emailButtonText);
    return templateService.processTemplate(variables, TEMPLATE_NAME);
  }
}
