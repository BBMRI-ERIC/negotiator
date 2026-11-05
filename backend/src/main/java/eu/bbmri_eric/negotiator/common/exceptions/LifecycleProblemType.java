package eu.bbmri_eric.negotiator.common.exceptions;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** The categories of a refused lifecycle Event that a client can act on differently. */
@Getter
@RequiredArgsConstructor
public enum LifecycleProblemType {
  NO_TRANSITION("no-transition", "No Transition for this Event"),
  UNMET_REQUIREMENT("unmet-requirement", "Information Requirement not met"),
  DOMAIN_STATE_CONFLICT("domain-state-conflict", "A Guard blocks this Transition");

  /** Path segment under which the problem type is identified. */
  public static final String TYPE_PATH = "/v3/problems/lifecycle/";

  private final String slug;
  private final String title;
}
