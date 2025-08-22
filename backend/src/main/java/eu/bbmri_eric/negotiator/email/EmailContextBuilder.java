package eu.bbmri_eric.negotiator.email;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Component
final class EmailContextBuilder {

  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("d.M.yyyy, HH:mm");

  private final TemplateEngine templateEngine;
  private final String frontendUrl;
  private final String emailYoursSincerelyText;
  private final String emailHelpdeskHref;
  private final String logoURL;

  EmailContextBuilder(
      TemplateEngine templateEngine,
      @Value("${negotiator.frontend-url}") String frontendUrl,
      @Value("${negotiator.emailYoursSincerelyText}") String emailYoursSincerelyText,
      @Value("${negotiator.emailHelpdeskHref}") String emailHelpdeskHref,
      @Value("${negotiator.emailLogo}") String logoURL) {
    this.templateEngine = templateEngine;
    this.frontendUrl = frontendUrl;
    this.emailYoursSincerelyText = emailYoursSincerelyText;
    this.emailHelpdeskHref = emailHelpdeskHref;
    this.logoURL = logoURL;
  }

  @NonNull
  String buildEmailContent(
      @NonNull String templateName,
      @NonNull String recipientName,
      @NonNull String message,
      String negotiationId,
      String negotiationTitle,
      LocalDateTime negotiationCreationDate) {

    var context = new Context();

    context.setVariable("recipient", recipientName);
    context.setVariable("message", message);

    if (negotiationId != null && negotiationTitle != null && negotiationCreationDate != null) {
      context.setVariable("negotiation", negotiationId);
      context.setVariable("titleForNegotiation", negotiationTitle);
      context.setVariable("date", negotiationCreationDate.format(DATE_TIME_FORMATTER));
    }
    context.setVariable("frontendUrl", frontendUrl);
    context.setVariable("emailYoursSincerelyText", emailYoursSincerelyText);
    context.setVariable("emailHelpdeskHref", emailHelpdeskHref);
    context.setVariable("logoUrl", logoURL);

    return templateEngine.process(templateName, context);
  }
}
