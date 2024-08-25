package eu.bbmri_eric.negotiator.user;

import java.security.Principal;

public interface NegotiatorPrincipal extends Principal {

  Person getPerson();
}
