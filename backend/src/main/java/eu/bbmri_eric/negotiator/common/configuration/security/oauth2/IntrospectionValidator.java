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
  private final OAuth2Error error = new OAuth2Error("401", "Introspection failed", null);
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
    }
    return introspect(token);
  }

  private OAuth2TokenValidatorResult introspect(Jwt token) {
    String subject = token.getSubject();
    if (subject == null) {
      subject = token.getClaimAsString("client_id");
    }
    if (subject == null) {
      log.error("Token does not contain a subject or client_id claim.");
      return OAuth2TokenValidatorResult.failure(error);
    }
    try {
      if (jwtCache.containsKey(subject)) {
        log.debug("Token for subject %s found in cache.".formatted(subject));
        return OAuth2TokenValidatorResult.success();
      }
      HttpResponse<String> response = sendHttpRequest(token);
      if (isRequestSuccessful(response)) {
        jwtCache.put(subject, token);
        log.debug("Token for subject %s validated and stored in cache.".formatted(subject));
        return OAuth2TokenValidatorResult.success();
      } else {
        log.warn("Token for subject %s failed introspection.".formatted(subject));
        return OAuth2TokenValidatorResult.failure(error);
      }
    } catch (Exception e) {
      log.error(
          "Error during introspection for subject %s: %s".formatted(subject, e.getMessage()), e);
      return OAuth2TokenValidatorResult.failure(error);
    }
  }

  private boolean isRequestSuccessful(HttpResponse<String> response) {
    return response != null
        && response.statusCode() == 200
        && response.body().contains("\"active\":true");
  }

  private HttpResponse<String> sendHttpRequest(Jwt token) {
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = buildHttpRequest(token);
    try {
      return client.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt(); // Preserve the interrupt status
      log.error("Thread was interrupted while sending HTTP request to introspection endpoint!", e);
      return null;
    } catch (IOException e) {
      log.error("Could not send request to introspection endpoint!", e);
      return null;
    }
  }

  private HttpRequest buildHttpRequest(Jwt token) {
    return HttpRequest.newBuilder()
        .uri(URI.create(introspectionUri))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .header(
            "Authorization",
            "Basic "
                + Base64.getEncoder()
                    .encodeToString(("%s:%s".formatted(clientId, clientSecret)).getBytes()))
        .POST(HttpRequest.BodyPublishers.ofString("token=" + token.getTokenValue()))
        .build();
  }

  public static void cleanCache() {
    log.debug("Clearing JWT cache.");
    jwtCache.clear();
  }

  // Add a public method to inspect cache size
  public static int getCacheSize() {
    return jwtCache.size();
  }
}
