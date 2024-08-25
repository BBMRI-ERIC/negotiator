package eu.bbmri_eric.negotiator.common;

import eu.bbmri_eric.negotiator.user.Person;
import java.security.Principal;

public interface NegotiatorPrincipal extends Principal {

  Person getPerson();
}
