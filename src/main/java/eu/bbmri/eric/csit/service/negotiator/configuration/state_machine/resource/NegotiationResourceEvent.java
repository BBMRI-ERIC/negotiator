package eu.bbmri.eric.csit.service.negotiator.configuration.state_machine.resource;

public enum NegotiationResourceEvent {
  CONTACT,
  MARK_AS_UNREACHABLE,
  RETURN_FOR_RESUBMISSION,
  MARK_AS_CHECKING_AVAILABILITY,
  MARK_AS_AVAILABLE,

  MARK_AS_UNAVAILABLE,
  MARK_AS_CURRENTLY_UNAVAILABLE_BUT_WILLING_TO_COLLECT,
  STEP_AWAY,
  INDICATE_ACCESS_CONDITIONS,
  ACCEPT_ACCESS_CONDITIONS,
  DECLINE_ACCESS_CONDITIONS,
  GRANT_ACCESS_TO_RESOURCE
}
