package eu.bbmri_eric.negotiator.common.exceptions;

import lombok.Getter;

/** A lifecycle Event was refused because the Lifecycle cannot take it right now. */
@Getter
public class LifecycleConflictException extends RuntimeException {
  private final LifecycleProblemType type;

  public LifecycleConflictException(LifecycleProblemType type, String detail) {
    super(detail);
    this.type = type;
  }
}
