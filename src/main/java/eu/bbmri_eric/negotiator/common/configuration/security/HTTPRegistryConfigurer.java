package eu.bbmri_eric.negotiator.common.configuration.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;

/** Configuration class for securing available HTTP endpoints. */
@Configuration
public class HTTPRegistryConfigurer {

  private final MvcRequestMatcher.Builder mvc;

  @Value("${management.endpoint.prometheus.ip}")
  private String prometheusWhitelistedIp;

  public HTTPRegistryConfigurer(MvcRequestMatcher.Builder mvcRequestMatcherBuilder) {
    mvc = mvcRequestMatcherBuilder;
  }

  public void configure(
      AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry
          auth) {
    auth.requestMatchers(mvc.pattern(HttpMethod.GET, "/v3/negotiations"))
        .hasRole("ADMIN")
        .requestMatchers(mvc.pattern(HttpMethod.GET, "/v3/negotiation-lifecycle/**"))
        .permitAll()
            .requestMatchers(mvc.pattern(HttpMethod.GET, "/v3/resource-lifecycle/**"))
            .permitAll()
        .requestMatchers(mvc.pattern("/v3/negotiations/*/attachments/*"))
        .authenticated()
        .requestMatchers(mvc.pattern("/v3/attachments/**"))
        .authenticated()
        .requestMatchers(mvc.pattern("/v3/negotiations/**"))
        .authenticated()
        .requestMatchers(mvc.pattern("/v3/access-criteria"))
        .permitAll()
        .requestMatchers(mvc.pattern(HttpMethod.GET, "/v3/access-forms/**"))
        .permitAll()
        .requestMatchers(mvc.pattern(HttpMethod.POST, "/v3/requests"))
        .permitAll()
            .requestMatchers(mvc.pattern(HttpMethod.PUT, "/v3/requests/**"))
            .authenticated()
            .requestMatchers(mvc.pattern(HttpMethod.GET, "/v3/requests/**"))
            .authenticated()
            .requestMatchers(mvc.pattern(HttpMethod.POST, "/directory/create_query"))
            .permitAll()
            .requestMatchers(mvc.pattern(HttpMethod.PUT, "/directory/create_query"))
            .authenticated()
        .requestMatchers(mvc.pattern(HttpMethod.POST, "/v3/discovery-services/**"))
        .hasRole("ADMIN")
        .requestMatchers(mvc.pattern(HttpMethod.PUT, "/v3/discovery-services/**"))
        .hasRole("ADMIN")
            .requestMatchers(mvc.pattern(HttpMethod.GET, "/v3/discovery-services/**"))
            .authenticated()
            .requestMatchers(mvc.pattern("/v3/notifications"))
            .authenticated()
        .requestMatchers(mvc.pattern("/v3/users/**"))
        .authenticated()
        .requestMatchers(mvc.pattern("/v3/userinfo"))
        .authenticated()
        .requestMatchers(mvc.pattern("/v3/users/roles"))
        .authenticated()
            .requestMatchers(mvc.pattern("/v3/users/*/networks"))
            .authenticated()
        .requestMatchers(mvc.pattern("/v3/users/resources"))
        .authenticated()
        .requestMatchers(mvc.pattern("/v3/users/*/resources"))
        .authenticated()
        .requestMatchers(mvc.pattern(HttpMethod.POST, "/v3/users/*/resources"))
        .hasAnyRole("AUTHORIZATION_MANAGER", "ADMIN")
        .requestMatchers(mvc.pattern(HttpMethod.PUT, "/v3/users/*/resources"))
        .hasAnyRole("AUTHORIZATION_MANAGER", "ADMIN")
        .requestMatchers(mvc.pattern(HttpMethod.DELETE, "/v3/users/*/resources/**"))
        .hasAnyRole("AUTHORIZATION_MANAGER", "ADMIN")
        .requestMatchers(mvc.pattern(HttpMethod.GET, "/v3/resources/**"))
        .authenticated()
        .requestMatchers(mvc.pattern("/v3/users/*/negotiations"))
        .authenticated()
        .requestMatchers(mvc.pattern(HttpMethod.POST, "/v3/sections"))
        .hasRole("ADMIN")
        .requestMatchers(mvc.pattern(HttpMethod.PUT, "/v3/sections/**"))
        .hasRole("ADMIN")
            .requestMatchers(mvc.pattern(HttpMethod.GET, "/v3/sections/**"))
            .authenticated()
        .requestMatchers(mvc.pattern(HttpMethod.POST, "/v3/elements"))
        .hasRole("ADMIN")
        .requestMatchers(mvc.pattern(HttpMethod.PUT, "/v3/elements/**"))
        .hasRole("ADMIN")
            .requestMatchers(mvc.pattern(HttpMethod.GET, "/v3/elements/**"))
            .authenticated()
        .requestMatchers(mvc.pattern(HttpMethod.POST, "/v3/access-forms/**"))
        .hasRole("ADMIN")
        .requestMatchers(mvc.pattern(HttpMethod.PUT, "/v3/access-forms/**"))
        .hasRole("ADMIN")
        .requestMatchers(mvc.pattern(HttpMethod.DELETE, "/v3/access-forms/**"))
        .hasRole("ADMIN")
            .requestMatchers(mvc.pattern(HttpMethod.GET, "/v3/access-forms/**"))
            .authenticated()
            .requestMatchers(mvc.pattern("/v3/value-sets/**"))
            .authenticated()
        .requestMatchers(mvc.pattern(HttpMethod.POST, "/v3/networks/**"))
        .hasRole("ADMIN")
        .requestMatchers(mvc.pattern(HttpMethod.DELETE, "/v3/networks/**"))
        .hasRole("ADMIN")
        .requestMatchers(mvc.pattern(HttpMethod.PUT, "/v3/networks/**"))
        .hasRole("ADMIN")
            .requestMatchers(mvc.pattern(HttpMethod.GET, "/v3/networks/**"))
            .authenticated()
        .requestMatchers(mvc.pattern(HttpMethod.GET, "/v3/info-requirements/**"))
        .authenticated()
        .requestMatchers(mvc.pattern(HttpMethod.POST, "/v3/info-requirements"))
        .hasRole("ADMIN")
        .requestMatchers(mvc.pattern(HttpMethod.PUT, "/v3/info-requirements/**"))
        .hasRole("ADMIN")
        .requestMatchers(mvc.pattern(HttpMethod.DELETE, "/v3/info-requirements/**"))
        .hasRole("ADMIN")
        .requestMatchers(mvc.pattern("/v3/info-submissions/**"))
        .authenticated()
        .requestMatchers(mvc.pattern("/actuator/prometheus"))
        // Needs to be IPv6 address
        .access(
            new WebExpressionAuthorizationManager(
                "hasIpAddress('%s')".formatted(prometheusWhitelistedIp)))
        .requestMatchers(mvc.pattern("/actuator/info"))
        .permitAll()
        .requestMatchers(mvc.pattern("/actuator/health"))
        .permitAll()
        .requestMatchers(mvc.pattern("/swagger-ui/**"))
        .permitAll()
        .requestMatchers(mvc.pattern("/v3/api-docs/**"))
        .permitAll()
        .anyRequest()
        .denyAll();
  }
}
