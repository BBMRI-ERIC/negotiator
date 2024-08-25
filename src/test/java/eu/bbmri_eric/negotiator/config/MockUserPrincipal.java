package eu.bbmri_eric.negotiator.config;

import eu.bbmri_eric.negotiator.common.NegotiatorPrincipal;
import eu.bbmri_eric.negotiator.user.Person;
import java.util.Collection;
import java.util.HashSet;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class MockUserPrincipal implements UserDetails, NegotiatorPrincipal {

  private final Person person;

  private final HashSet<GrantedAuthority> authorities;

  public MockUserPrincipal(Person person) {
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
    return person.getName();
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

  @Override
  public String getName() {
    return person.getName();
  }
}
