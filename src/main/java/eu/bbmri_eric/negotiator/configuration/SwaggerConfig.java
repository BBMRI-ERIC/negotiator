package eu.bbmri_eric.negotiator.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.Scopes;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
  @Value("${springdoc.oAuthFlow.authorizationUrl}")
  private String authorizationUrl;

  @Value("${springdoc.oAuthFlow.tokenUrl}")
  private String tokenUrl;

  @Value("#{'${springdoc.swagger-ui.oauth.scopes}'.split(' ')}")
  private List<String> scopes;

  @Bean
  public OpenAPI customOpenAPI() {
    Scopes oauthScopes = new Scopes();
    for (String scope : scopes) {
      oauthScopes.addString(scope, "no description");
    }
    return new OpenAPI()
        .info(
            new Info()
                .license(
                    new License()
                        .name("GNU AFFERO GENERAL PUBLIC LICENSE")
                        .url("https://www.gnu.org/licenses/agpl-3.0.en.html"))
                .title("Negotiator API")
                .termsOfService(
                    "https://web.bbmri-eric.eu/Policies/BBMRI-ERIC-AUP-IT-Services-1_3.pdf")
                .description(
                    "Documentation and examples for the Negotiator REST API. Some endpoints require the client to supply an OAuth2 bearer token along with the HTTP request,"
                        + " for obtaining one please use the 'Authorize' button bellow. In case you do not have an account with the connected OpenID Connect provider please contact us.")
                .contact(
                    new Contact()
                        .email("negotiator@helpdesk.bbmri-eric.eu")
                        .name("BBMRI-ERIC CS-IT")
                        .url("https://www.bbmri-eric.eu/bbmri-eric/common-service-it/"))
                .version("3.0.0"))
        .components(
            new Components()
                .addSecuritySchemes(
                    "security_auth",
                    new io.swagger.v3.oas.models.security.SecurityScheme()
                        .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.OAUTH2)
                        .flows(
                            new io.swagger.v3.oas.models.security.OAuthFlows()
                                .authorizationCode(
                                    new io.swagger.v3.oas.models.security.OAuthFlow()
                                        .authorizationUrl(authorizationUrl)
                                        .tokenUrl(tokenUrl)
                                        .scopes(oauthScopes)))));
  }
}
