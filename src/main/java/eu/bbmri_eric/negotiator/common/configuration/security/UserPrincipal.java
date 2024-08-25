package eu.bbmri_eric.negotiator.common.configuration.security;

import eu.bbmri_eric.negotiator.user.NegotiatorPrincipal;
import eu.bbmri_eric.negotiator.user.Person;
import lombok.Getter;

@Getter
public class UserPrincipal implements NegotiatorPrincipal {

  private final Person person;

  public UserPrincipal(Person person) {
    this.person = person;
  }

  @Override
  public String getName() {
    return person.getName();
  }
}
