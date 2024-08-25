package eu.bbmri_eric.negotiator.user;


public class JwtUserDetails implements NegotiatorUserDetails {

  private final Person person;

  public JwtUserDetails(Person person) {
    this.person = person;
  }

  public Person getPerson() {
    return person;
  }
}
