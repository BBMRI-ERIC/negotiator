package eu.bbmri_eric.negotiator.notification.internal;

import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.email.EmailService;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.governance.resource.UnreachableResourcesEvent;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class UnreachableResourcesHandler
    implements NotificationStrategy<UnreachableResourcesEvent> {
  private static final String TITLE = "New Negotiation Request in the BBMRI-ERIC Negotiator";
  private final EmailService emailService;
  private final NegotiationRepository negotiationRepository;

  public UnreachableResourcesHandler(
      EmailService emailService, NegotiationRepository negotiationRepository) {
    this.emailService = emailService;
    this.negotiationRepository = negotiationRepository;
  }

  @Override
  public Class<UnreachableResourcesEvent> getSupportedEventType() {
    return UnreachableResourcesEvent.class;
  }

  @Override
  @Transactional
  public void notify(UnreachableResourcesEvent event) {
    Negotiation negotiation =
        negotiationRepository
            .findById(event.getNegotiationId())
            .orElseThrow(() -> new EntityNotFoundException(event.getNegotiationId()));
    var resourcesByContactEmail =
        negotiation.getResources().stream()
            .filter(resource -> resource.getRepresentatives().isEmpty())
            .filter(
                resource ->
                    resource.getContactEmail() != null && !resource.getContactEmail().isEmpty())
            .collect(Collectors.groupingBy(Resource::getContactEmail));
    resourcesByContactEmail.forEach(
        (contactEmail, resources) -> {
          String message = buildGroupedNotificationMessage(negotiation, resources);
          emailService.sendEmail(contactEmail, TITLE, message);
        });
  }

  private String buildGroupedNotificationMessage(
      Negotiation negotiation, List<Resource> resources) {
    var resourcesList =
        resources.stream()
            .map(Resource::getName)
            .map(name -> String.format("<li><strong>%s</strong></li>", name))
            .collect(Collectors.joining());

    var resourceCount = resources.size();
    var resourceText = resourceCount == 1 ? "resource" : "resources";

    return """
            <p>Hello,</p>

            <p>We wanted to let you know that a researcher has submitted a negotiation request for %d %s under your organization. However, we noticed that no representatives are currently assigned to handle requests for these resources.</p>

            <div style="background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 15px 0;">
                <h4 style="margin-top: 0; color: #495057;">📋 Request Details</h4>
                <p style="margin-bottom: 5px;"><strong>Project Title:</strong> %s</p>
                <p style="margin-bottom: 5px;"><strong>Request ID:</strong> %s</p>
                <p style="margin-bottom: 10px;"><strong>Researcher Contact:</strong> %s (<a href="mailto:%s">%s</a>)</p>
                <p style="margin-bottom: 5px;"><strong>Resources without representatives:</strong></p>
                <ul style="margin-top: 5px;">%s</ul>
            </div>

            <p><strong>What you can do:</strong></p>
            <ul>
                <li>Contact the researcher directly to discuss their request</li>
                <li>Assign representatives for your resources in the BBMRI-ERIC Negotiator system</li>
                <li>Forward this request to the appropriate person in your organization</li>
            </ul>

            <p>Thank you for your attention to this request.</p>

            <p style="color: #6c757d; font-size: 14px;">BBMRI-ERIC Negotiator Team</p>
            """
        .formatted(
            resourceCount,
            resourceText,
            negotiation.getTitle(),
            negotiation.getId(),
            negotiation.getCreatedBy().getName(),
            negotiation.getCreatedBy().getEmail(),
            negotiation.getCreatedBy().getEmail(),
            resourcesList);
  }
}
