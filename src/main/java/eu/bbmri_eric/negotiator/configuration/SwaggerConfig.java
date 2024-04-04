package eu.bbmri_eric.negotiator.configuration;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.OAuthFlow;
import io.swagger.v3.oas.annotations.security.OAuthFlows;
import io.swagger.v3.oas.annotations.security.OAuthScope;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecurityScheme(
    name = "security_auth",
    type = SecuritySchemeType.OAUTH2,
    description = "OAuth2 security configuration",
    flows =
        @OAuthFlows(
            authorizationCode =
                @OAuthFlow(
                    authorizationUrl = "${springdoc.oAuthFlow.authorizationUrl}",
                    tokenUrl = "${springdoc.oAuthFlow.tokenUrl}",
                    scopes = {
                      @OAuthScope(name = "openid", description = "IdentityPortal.API"),
                      @OAuthScope(name = "profile", description = "IdentityPortal.API"),
                      @OAuthScope(name = "email", description = "IdentityPortal.API"),
                      @OAuthScope(name = "permissions", description = "IdentityPortal.API"),
                      @OAuthScope(name = "some-app-scope-1", description = "IdentityPortal.API")
                    })))
public class SwaggerConfig {
  @Bean
  public OpenAPI customOpenAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .license(new License().name("GNU AFFERO GENERAL PUBLIC LICENSE"))
                .title("Negotiator API")
                .description("Documentation and examples for the REST API.")
                .version("3.0.0"))
        .addSecurityItem(new SecurityRequirement().addList("security_auth"));
  }
}
