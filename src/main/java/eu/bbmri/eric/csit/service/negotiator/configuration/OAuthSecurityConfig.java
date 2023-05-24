package eu.bbmri.eric.csit.service.negotiator.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("test")
public class OAuthSecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .authorizeHttpRequests().antMatchers("/v3/negotiations/**", "/v3/projects/**").authenticated()
                .and()
                .authorizeHttpRequests().antMatchers(HttpMethod.GET, "/v3/access-criteria/**").authenticated()
                .and()
                .authorizeHttpRequests().antMatchers(HttpMethod.POST, "/directory/create_query").permitAll()
                .and()
                .httpBasic()
                .and()
                        .oauth2ResourceServer(OAuth2ResourceServerConfigurer::jwt);
        return http.build();
    }
}
