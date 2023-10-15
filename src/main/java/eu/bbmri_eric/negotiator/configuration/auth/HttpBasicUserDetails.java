package eu.bbmri_eric.negotiator.configuration.auth;

import eu.bbmri_eric.negotiator.database.model.Person;
import java.util.Collection;
import java.util.HashSet;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class HttpBasicUserDetails implements UserDetails, NegotiatorUserDetails {

  private final Person person;

  private final HashSet<GrantedAuthority> authorities;

  public HttpBasicUserDetails(Person person) {
    this.person = person;
    authorities = new HashSet<>();
    person
        .getAuthorities()
        .forEach(
            authority -> authorities.add(new SimpleGrantedAuthority(authority.getAuthority())));
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public String getPassword() {
    return person.getPassword();
  }

  @Override
  public String getUsername() {
    return person.getAuthName();
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  public Person getPerson() {
    return person;
  }
}
