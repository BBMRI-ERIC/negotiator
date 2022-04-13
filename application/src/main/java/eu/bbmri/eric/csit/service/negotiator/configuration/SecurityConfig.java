package eu.bbmri.eric.csit.service.negotiator.configuration;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    //    http.authorizeRequests(
    //            authz ->
    //                authz
    //                    .antMatchers(HttpMethod.GET, "/data-sources/**")
    //                    .permitAll()
    //                    .anyRequest()
    //                    .authenticated())
    //        .oauth2ResourceServer(OAuth2ResourceServerConfigurer::jwt);
    http.cors().and().csrf().disable();
  }

  @Bean
  public ModelMapper modelMapper() {
    return new ModelMapper();
  }
}
