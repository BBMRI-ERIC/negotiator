package eu.bbmri_eric.negotiator.configuration.security.auth;

import eu.bbmri_eric.negotiator.database.model.Person;

public interface NegotiatorUserDetails {

  Person getPerson();
}
