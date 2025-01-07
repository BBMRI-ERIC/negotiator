package eu.bbmri_eric.negotiator.common;

import eu.bbmri_eric.negotiator.user.Person;
import java.security.Principal;

/**
 * A Principal object emulating currently Authenticated entity, e.g., Physical person or a service
 * account.
 */
public interface NegotiatorPrincipal extends Principal {

  Person getPerson();
}
