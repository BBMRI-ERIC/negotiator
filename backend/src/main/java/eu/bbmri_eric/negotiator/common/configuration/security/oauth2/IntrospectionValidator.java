package eu.bbmri_eric.negotiator.common.configuration.security.oauth2;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

@CommonsLog
public class IntrospectionValidator implements OAuth2TokenValidator<Jwt> {
  OAuth2Error error = new OAuth2Error("401", "Introspection failed", null);
  private final String introspectionUri;
  private final String clientId;
  private final String clientSecret;
  private static final Map<String, Jwt> jwtCache = new ConcurrentHashMap<>();

  public IntrospectionValidator(String introspectionUri, String clientId, String clientSecret) {
    Objects.requireNonNull(
        introspectionUri, "Introspection URI must not be null but can be empty!");
    this.introspectionUri = introspectionUri;
    this.clientId = clientId;
    this.clientSecret = clientSecret;
  }

  @Override
  public OAuth2TokenValidatorResult validate(Jwt token) {
    if (introspectionUri.isEmpty()) {
      return OAuth2TokenValidatorResult.success();
    } else {
      return introspect(token);
    }
  }

  private OAuth2TokenValidatorResult introspect(Jwt token) {
    String subject = token.getSubject();
    if (Objects.isNull(subject)) {
      subject = token.getClaimAsString("client_id");
    }
    if (jwtCache.containsKey(subject) || isRequestSuccessful(sendHttpRequest(token))) {
      log.debug("Introspection for subject %s was successful!".formatted(subject));
      return OAuth2TokenValidatorResult.success();
    } else {
      log.warn("Introspection for subject %s failed!".formatted(subject));
      return OAuth2TokenValidatorResult.failure(error);
    }
  }

  private boolean isRequestSuccessful(HttpResponse<String> response) {
    if (Objects.isNull(response)) return false;
    return response.statusCode() == 200 && response.body().contains("\"active\":true");
  }

  private HttpResponse<String> sendHttpRequest(Jwt token) {
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = buildHttpRequest(token);
    HttpResponse<String> response;
    try {
      response = client.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (IOException | InterruptedException e) {
      log.error("Could not send request to introspection endpoint!");
      return null;
    }
    return response;
  }

  private HttpRequest buildHttpRequest(Jwt token) {
    return HttpRequest.newBuilder()
        .uri(URI.create(introspectionUri))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .header(
            "Authorization",
            "Basic "
                + Base64.getEncoder()
                    .encodeToString((("%s:%s").formatted(clientId, clientSecret)).getBytes()))
        .POST(HttpRequest.BodyPublishers.ofString("token=" + token.getTokenValue()))
        .build();
  }

  static void cleanCache() {
    log.debug("Clearing JWT cache.");
    jwtCache.clear();
  }
}
