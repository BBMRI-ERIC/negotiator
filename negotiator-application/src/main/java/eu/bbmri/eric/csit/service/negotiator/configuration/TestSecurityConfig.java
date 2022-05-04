package eu.bbmri.eric.csit.service.negotiator.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableWebSecurity
@Profile({"test"})
public class TestSecurityConfig extends WebSecurityConfigurerAdapter {

  @Autowired private PasswordEncoder passwordEncoder;

  @Override
  public void configure(AuthenticationManagerBuilder auth) throws Exception {
    auth.inMemoryAuthentication()
        .withUser("researcher")
        .password(passwordEncoder.encode("researcher"))
        .authorities("RESEARCHER")
        .and()
        .withUser("admin")
        .password(passwordEncoder.encode("admin"))
        .authorities("ADMIN")
        .and()
        .withUser("directory")
        .password(passwordEncoder.encode("directory"))
        .authorities("EXT_SERV")
        .and()
        .withUser("perun")
        .password(passwordEncoder.encode("perun"))
        .authorities("PERUN_USER");
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http.cors()
        .and()
        .csrf()
        .disable()
        .authorizeRequests()
        .antMatchers(HttpMethod.POST, "/v3/data-sources/**")
        .hasAuthority("ADMIN")
        .antMatchers(HttpMethod.PUT, "/v3/data-sources/**")
        .hasAuthority("ADMIN")
        .and()
        .authorizeRequests()
        .antMatchers("/v3/projects/**", "/v3/requests/**")
        .hasAuthority("RESEARCHER")
        .and()
        .authorizeRequests()
        .antMatchers(HttpMethod.POST, "/v3/queries/**")
        .hasAnyRole("ADMIN", "EXT_SERV")
        .and()
        .authorizeRequests()
        .anyRequest()
        .permitAll()
        .and()
        .httpBasic();
  }
}
