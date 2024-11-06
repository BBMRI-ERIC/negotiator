package eu.bbmri_eric.negotiator.common.configuration.security.oauth2;

import static org.springframework.security.oauth2.core.OAuth2TokenIntrospectionClaimNames.AUD;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.proc.DefaultJOSEObjectTypeVerifier;
import java.util.List;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
public class JwtDecoderConfig {
  @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
  private String jwtIssuer;

  @Value("${spring.security.oauth2.resourceserver.opaquetoken.introspection-uri}")
  private String introspectionEndpoint;

  @Value("${spring.security.oauth2.resourceserver.opaquetoken.client-id}")
  private String clientId;

  @Value("${spring.security.oauth2.resourceserver.opaquetoken.client-secret}")
  private String clientSecret;

  @Value("${spring.security.oauth2.resourceserver.jwt.audiences}")
  private String audience;

  @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
  private String jwksUrl;

  @Value("${spring.security.oauth2.resourceserver.jwt.type}")
  private String jwtType;

  @Bean
  public JwtDecoder jwtDecoder() {
    NimbusJwtDecoder decoder = setupJWTDecoder();
    addTokenValidators(decoder);
    return decoder;
  }

  @NonNull
  private NimbusJwtDecoder setupJWTDecoder() {
    return NimbusJwtDecoder.withJwkSetUri(this.jwksUrl)
        .jwtProcessorCustomizer(
            customizer -> {
              customizer.setJWSTypeVerifier(
                  new DefaultJOSEObjectTypeVerifier<>(new JOSEObjectType(jwtType)));
            })
        .build();
  }

  private void addTokenValidators(NimbusJwtDecoder decoder) {
    decoder.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(
            audienceValidator(),
            new JwtTimestampValidator()));
  }

  //  @Bean
  //  public OAuth2TokenValidator<Jwt> introspectionValidator() {
  //    return new IntrospectionValidator(introspectionEndpoint, clientId, clientSecret);
  //  }

  private OAuth2TokenValidator<Jwt> audienceValidator() {
    return new JwtClaimValidator<List<String>>(AUD, aud -> aud.contains(audience));
  }
}
