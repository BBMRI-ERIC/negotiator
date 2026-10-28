package eu.bbmri_eric.negotiator.common.exceptions;

import org.springframework.statemachine.StateMachineException;

/**
 * Thrown when a resource lifecycle event is blocked because a required information-requirement form
 * has not been submitted yet.
 */
public class InformationRequirementNotMetException extends StateMachineException {

  public static final String PROBLEM_TYPE_URI =
      "https://negotiator.bbmri-eric.eu/errors/information-requirement-not-met";

  public InformationRequirementNotMetException() {
    super(
        "The requirement for this operation was not met. "
            + "Please make sure you have submitted the required form and try again.");
  }
}

//public class InformationRequirementNotMetException extends StateMachineException {
//
//  public static final String ERROR_CODE = "INFORMATION_REQUIREMENT_NOT_MET";
//
//  public InformationRequirementNotMetException() {
//    super(
//            "The requirement for this operation was not met. "
//                    + "Please make sure you have submitted the required form and try again.");
//  }
//}
