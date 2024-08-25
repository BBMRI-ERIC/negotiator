package eu.bbmri_eric.negotiator.common.configuration.security;

import eu.bbmri_eric.negotiator.user.Person;
import java.security.Principal;

public interface NegotiatorPrincipal extends Principal {

  Person getPerson();
}
