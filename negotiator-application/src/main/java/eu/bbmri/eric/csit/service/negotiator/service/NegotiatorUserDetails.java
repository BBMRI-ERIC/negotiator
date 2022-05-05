package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.model.DataSource;
import eu.bbmri.eric.csit.service.negotiator.model.User;
import java.util.Collection;
import java.util.HashSet;
import javax.transaction.Transactional;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class NegotiatorUserDetails implements UserDetails {

  private final User user;

  private final HashSet<GrantedAuthority> authorities;

  public NegotiatorUserDetails(User user) {
    this.user = user;
    authorities = new HashSet<>();
    user.getAuthorities()
        .forEach(
            authority -> authorities.add(new SimpleGrantedAuthority(authority.getAuthority())));
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public String getPassword() {
    return user.getPassword();
  }

  @Override
  public String getUsername() {
    return user.getUsername();
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
    return user.isEnabled();
  }

  public DataSource getDataSource() {
    return user.getDataSource();
  }
}
