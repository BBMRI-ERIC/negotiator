package eu.bbmri_eric.negotiator.unit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import eu.bbmri_eric.negotiator.common.configuration.security.oauth2.IntrospectionValidator;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public class IntrospectionValidatorTest {

  private static final String TEST_CLIENT_ID = "client-id";
  private static final String TEST_CLIENT_SECRET = "client-secret";

  @RegisterExtension
  static WireMockExtension wireMockServer = WireMockExtension.newInstance().build();

  @BeforeEach
  void setup() {
    // Clear the cache before each test.
    IntrospectionValidator.cleanCache();
    // Reset WireMock mappings between tests.
    wireMockServer.resetAll();
  }

  /**
   * Helper method to create a fake Jwt. The Jwt builder now expects a Consumer<Map<String, Object>>
   * so we add all claims via a lambda.
   */
  private Jwt createFakeJwt(
      String subject, String tokenValue, Map<String, Object> additionalClaims) {
    Map<String, Object> claims;
    if (subject != null) {
      claims = new HashMap<>(additionalClaims);
      claims.put("sub", subject);
    } else {
      claims = additionalClaims;
    }
    return Jwt.withTokenValue(tokenValue)
        .header("alg", "none")
        .claims(map -> map.putAll(claims))
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(60))
        .build();
  }

  @Test
  void validate_successfulIntrospection_returnsSuccessAndCachesToken() {
    int port = wireMockServer.getRuntimeInfo().getHttpPort();
    String introspectionUri = "http://localhost:" + port + "/introspect";

    // Stub a successful introspection response.
    wireMockServer.stubFor(
        post(urlEqualTo("/introspect"))
            .withHeader("Content-Type", equalTo("application/x-www-form-urlencoded"))
            .willReturn(aResponse().withStatus(200).withBody("{\"active\":true}")));

    IntrospectionValidator validator =
        new IntrospectionValidator(introspectionUri, TEST_CLIENT_ID, TEST_CLIENT_SECRET);
    Jwt jwt = createFakeJwt("subjectWireMock", "tokenWireMock", Map.of("sub", "subjectWireMock"));

    // Cache should be empty initially.
    assertEquals(0, IntrospectionValidator.getCacheSize(), "Cache should be empty initially");

    // Validate token; this should trigger an HTTP call and cache the token.
    OAuth2TokenValidatorResult result = validator.validate(jwt);
    assertTrue(result.getErrors().isEmpty(), "Expected success on first introspection");
    assertEquals(
        1, IntrospectionValidator.getCacheSize(), "Expected cache size 1 after caching token");

    // Validate again; token should be retrieved from the cache.
    OAuth2TokenValidatorResult resultCached = validator.validate(jwt);
    assertTrue(resultCached.getErrors().isEmpty(), "Expected success from cached token");

    // Verify that only one HTTP call was made.
    wireMockServer.verify(1, postRequestedFor(urlEqualTo("/introspect")));
  }

  @Test
  void validate_inactiveResponse_returnsFailure() {
    int port = wireMockServer.getRuntimeInfo().getHttpPort();
    String introspectionUri = "http://localhost:" + port + "/introspect";

    // Stub an introspection response indicating the token is inactive.
    wireMockServer.stubFor(
        post(urlEqualTo("/introspect"))
            .willReturn(aResponse().withStatus(200).withBody("{\"active\":false}")));

    IntrospectionValidator validator =
        new IntrospectionValidator(introspectionUri, TEST_CLIENT_ID, TEST_CLIENT_SECRET);
    Jwt jwt = createFakeJwt("subjectInactive", "tokenInactive", Map.of("sub", "subjectInactive"));
    OAuth2TokenValidatorResult result = validator.validate(jwt);

    String errorCode = result.getErrors().iterator().next().getErrorCode();
    assertEquals("401", errorCode, "Expected failure when token is not active");
  }

  @Test
  void validate_whenIntrospectionUriIsEmpty_returnsSuccess() {
    // If introspectionUri is empty, the validator returns success immediately.
    IntrospectionValidator validator =
        new IntrospectionValidator("", TEST_CLIENT_ID, TEST_CLIENT_SECRET);
    Jwt jwt = createFakeJwt("subjectEmpty", "tokenEmpty", Map.of("sub", "subjectEmpty"));
    OAuth2TokenValidatorResult result = validator.validate(jwt);
    assertTrue(result.getErrors().isEmpty(), "Expected success when introspection URI is empty");
  }

  // Additional tests focused on cache behavior:

  @Test
  void validate_inactiveToken_notCached() {
    int port = wireMockServer.getRuntimeInfo().getHttpPort();
    String introspectionUri = "http://localhost:" + port + "/introspect";

    // Stub a response that returns inactive.
    wireMockServer.stubFor(
        post(urlEqualTo("/introspect"))
            .willReturn(aResponse().withStatus(200).withBody("{\"active\":false}")));

    IntrospectionValidator validator =
        new IntrospectionValidator(introspectionUri, TEST_CLIENT_ID, TEST_CLIENT_SECRET);
    Jwt jwt =
        createFakeJwt(
            "subjectInactiveNotCached",
            "tokenInactiveNotCached",
            Map.of("sub", "subjectInactiveNotCached"));
    OAuth2TokenValidatorResult result = validator.validate(jwt);
    String errorCode = result.getErrors().iterator().next().getErrorCode();
    assertEquals("401", errorCode, "Expected failure when token is inactive");
    assertEquals(0, IntrospectionValidator.getCacheSize(), "Inactive token should not be cached");
  }

  @Test
  void validate_multipleTokens_areCachedIndependently() {
    int port = wireMockServer.getRuntimeInfo().getHttpPort();
    String introspectionUri = "http://localhost:" + port + "/introspect";

    // Stub a response that returns active for all tokens.
    wireMockServer.stubFor(
        post(urlEqualTo("/introspect"))
            .willReturn(aResponse().withStatus(200).withBody("{\"active\":true}")));

    IntrospectionValidator validator =
        new IntrospectionValidator(introspectionUri, TEST_CLIENT_ID, TEST_CLIENT_SECRET);
    Jwt jwt1 = createFakeJwt("subject1", "token1", Map.of("sub", "subject1"));
    Jwt jwt2 = createFakeJwt("subject2", "token2", Map.of("sub", "subject2"));

    // Validate two different tokens.
    OAuth2TokenValidatorResult result1 = validator.validate(jwt1);
    OAuth2TokenValidatorResult result2 = validator.validate(jwt2);

    assertTrue(result1.getErrors().isEmpty(), "Expected success for token1");
    assertTrue(result2.getErrors().isEmpty(), "Expected success for token2");
    assertEquals(
        2, IntrospectionValidator.getCacheSize(), "Expected cache size 2 after caching two tokens");

    // Validate them again; no additional HTTP calls should be made.
    validator.validate(jwt1);
    validator.validate(jwt2);

    // Verify that only two HTTP calls were made.
    wireMockServer.verify(2, postRequestedFor(urlEqualTo("/introspect")));
  }

  @Test
  void validate_cleanCache_clearsCachedTokens() {
    int port = wireMockServer.getRuntimeInfo().getHttpPort();
    String introspectionUri = "http://localhost:" + port + "/introspect";

    // Stub a response that returns active.
    wireMockServer.stubFor(
        post(urlEqualTo("/introspect"))
            .willReturn(aResponse().withStatus(200).withBody("{\"active\":true}")));

    IntrospectionValidator validator =
        new IntrospectionValidator(introspectionUri, TEST_CLIENT_ID, TEST_CLIENT_SECRET);
    Jwt jwt = createFakeJwt("subjectClean", "tokenClean", Map.of("sub", "subjectClean"));

    // Validate token to cache it.
    validator.validate(jwt);
    assertEquals(
        1, IntrospectionValidator.getCacheSize(), "Expected cache size 1 after caching token");

    // Clear the cache.
    IntrospectionValidator.cleanCache();
    assertEquals(
        0, IntrospectionValidator.getCacheSize(), "Expected cache to be empty after clearing");

    // Validate token again; should trigger new HTTP call.
    validator.validate(jwt);
    assertEquals(
        1, IntrospectionValidator.getCacheSize(), "Expected cache size 1 after re-caching token");
  }
}
