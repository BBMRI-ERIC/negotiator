package eu.bbmri_eric.negotiator.lifecycle.resource;

import eu.bbmri_eric.negotiator.info_requirement.InformationRequirementRepository;
import eu.bbmri_eric.negotiator.info_submission.InformationSubmissionRepository;
import eu.bbmri_eric.negotiator.lifecycle.statemachine.Precondition;
import eu.bbmri_eric.negotiator.lifecycle.statemachine.TransitionPreconditionException;
import eu.bbmri_eric.negotiator.negotiation.NegotiationResourceEvent;
import org.springframework.stereotype.Component;

/** Precondition that requires an information submission when the event demands one. */
@Component("infoRequirementMet")
public class InfoRequirementMetPrecondition implements Precondition<ResourceTransitionContext> {

  private final InformationRequirementRepository requirementRepository;
  private final InformationSubmissionRepository informationSubmissionRepository;

  public InfoRequirementMetPrecondition(
      InformationRequirementRepository requirementRepository,
      InformationSubmissionRepository informationSubmissionRepository) {
    this.requirementRepository = requirementRepository;
    this.informationSubmissionRepository = informationSubmissionRepository;
  }

  @Override
  public void check(ResourceTransitionContext context, String event) {
    NegotiationResourceEvent eventEnum = NegotiationResourceEvent.valueOf(event);
    if (requirementRepository.existsByForEvent(eventEnum)
        && !informationSubmissionRepository.existsByResource_SourceIdAndNegotiation_Id(
            context.resourceId(), context.negotiationId())) {
      throw new TransitionPreconditionException(
          "The requirement for this operation was not met. Please make sure you have submitted the required form and try again.");
    }
  }
}
