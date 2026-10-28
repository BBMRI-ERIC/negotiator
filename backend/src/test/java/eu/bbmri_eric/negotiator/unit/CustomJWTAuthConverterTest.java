package eu.bbmri_eric.negotiator.unit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import eu.bbmri_eric.negotiator.common.UserPrincipal;
import eu.bbmri_eric.negotiator.common.configuration.security.oauth2.CustomJWTAuthConverter;
import eu.bbmri_eric.negotiator.user.Person;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

public class CustomJWTAuthConverterTest {

  private static final String TEST_AUTHZ_CLAIM = "roles";
  private static final String TEST_AUTHZ_ADMIN_VALUE = "admin";
  private static final String USER_INFO_ENDPOINT_PATH = "/userinfo";

  @RegisterExtension
  static WireMockExtension wireMockServer =
      WireMockExtension.newInstance()
          .options(WireMockConfiguration.options().dynamicPort())
          .build();

  @Mock private PersonRepository personRepository;

  private CustomJWTAuthConverter converterWithUserInfo;
  private CustomJWTAuthConverter converterWithoutUserInfo;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    CustomJWTAuthConverter.cleanCache();

    String userInfoEndpointUrl = wireMockServer.baseUrl() + USER_INFO_ENDPOINT_PATH;
    converterWithUserInfo =
        new CustomJWTAuthConverter(
            personRepository, userInfoEndpointUrl, TEST_AUTHZ_CLAIM, TEST_AUTHZ_ADMIN_VALUE);
    converterWithoutUserInfo =
        new CustomJWTAuthConverter(
            personRepository, null, TEST_AUTHZ_CLAIM, TEST_AUTHZ_ADMIN_VALUE);
  }

  private Jwt createFakeJwt(Map<String, Object> claims, String tokenValue) {
    return Jwt.withTokenValue(tokenValue)
        .header("alg", "none")
        .claims(map -> map.putAll(claims))
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(60))
        .build();
  }

  @Test
  void testConvertMachineToken_createsNewClient() {
    Map<String, Object> claims = new HashMap<>();
    claims.put("client_id", "machineClient");
    claims.put("scope", "read write");
    Jwt jwt = createFakeJwt(claims, "machineToken");

    when(personRepository.findBySubjectId("machineClient")).thenReturn(Optional.empty());
    Person newClient =
        Person.builder()
            .subjectId("machineClient")
            .name("machineClient")
            .email("no_email")
            .isServiceAccount(true)
            .build();
    when(personRepository.save(any(Person.class))).thenReturn(newClient);

    var authToken = converterWithoutUserInfo.convert(jwt);
    UserPrincipal principal = (UserPrincipal) authToken.getPrincipal();
    assertEquals("machineClient", principal.getPerson().getSubjectId());
    assertTrue(principal.getPerson().isServiceAccount());
  }

  @Test
  void testConvertMachineToken_withExistingClient() {
    Map<String, Object> claims = new HashMap<>();
    claims.put("client_id", "existingClient");
    claims.put("scope", "read write");
    Jwt jwt = createFakeJwt(claims, "machineToken");

    Person existingClient =
        Person.builder()
            .subjectId("existingClient")
            .name("existingClient")
            .email("no_email")
            .isServiceAccount(true)
            .build();
    when(personRepository.findBySubjectId("existingClient"))
        .thenReturn(Optional.of(existingClient));

    var authToken = converterWithoutUserInfo.convert(jwt);
    UserPrincipal principal = (UserPrincipal) authToken.getPrincipal();
    assertEquals("existingClient", principal.getPerson().getSubjectId());
  }

  @Test
  void testConvertMachineTokenWithGrantType() {
    Map<String, Object> claims = new HashMap<>();
    claims.put("client_id", "machineUser");
    claims.put("grant_type", "client_credentials");
    claims.put("scope", "openid negotiator_authz_management");
    Jwt jwt = createFakeJwt(claims, "machineToken");

    when(personRepository.findBySubjectId("machineUser")).thenReturn(Optional.empty());
    Person newClient =
        Person.builder()
            .subjectId("machineUser")
            .name("machineUser")
            .email("no_email")
            .isServiceAccount(true)
            .build();
    when(personRepository.save(any(Person.class))).thenReturn(newClient);

    var authToken = converterWithoutUserInfo.convert(jwt);
    UserPrincipal principal = (UserPrincipal) authToken.getPrincipal();
    assertTrue(principal.getPerson().isServiceAccount());
    assertTrue(
        authToken.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_AUTHORIZATION_MANAGER")));
  }

  @Test
  void testConvertUserTokenWithoutUserInfoEndpoint_createsNewUserFromJwtClaims() {
    Map<String, Object> claims = new HashMap<>();
    claims.put("sub", "test-user-1");
    claims.put("name", "Test User 1");
    claims.put("email", "test.user1@example.invalid");
    claims.put("scope", "profile");
    Jwt jwt = createFakeJwt(claims, "userToken1");

    when(personRepository.findBySubjectId("test-user-1")).thenReturn(Optional.empty());
    Person newUser =
        Person.builder()
            .subjectId("test-user-1")
            .name("Test User 1")
            .email("test.user1@example.invalid")
            .build();
    when(personRepository.save(any(Person.class))).thenReturn(newUser);

    var authToken = converterWithoutUserInfo.convert(jwt);
    UserPrincipal principal = (UserPrincipal) authToken.getPrincipal();
    assertEquals("test-user-1", principal.getPerson().getSubjectId());
    assertEquals("Test User 1", principal.getName());
    assertEquals("test.user1@example.invalid", principal.getPerson().getEmail());
  }

  @Test
  void testConvertUserTokenWithGrantType_usesJwtClaimsWithoutUserInfoRequest() {
    Map<String, Object> claims = new HashMap<>();
    claims.put("sub", "userWithGrantType");
    claims.put("client_id", "clientWithGrantType");
    claims.put("grant_type", "authorization_code");
    claims.put("name", "User With Grant Type");
    claims.put("email", "user.with.grant.type@example.com");
    claims.put(TEST_AUTHZ_CLAIM, List.of(TEST_AUTHZ_ADMIN_VALUE));
    Jwt jwt = createFakeJwt(claims, "userTokenWithGrantType");

    when(personRepository.findBySubjectId("userWithGrantType")).thenReturn(Optional.empty());
    Person newUser =
        Person.builder()
            .subjectId("userWithGrantType")
            .name("User With Grant Type")
            .email("user.with.grant.type@example.com")
            .build();
    when(personRepository.save(any(Person.class))).thenReturn(newUser);

    var authToken = converterWithUserInfo.convert(jwt);
    UserPrincipal principal = (UserPrincipal) authToken.getPrincipal();
    assertEquals("userWithGrantType", principal.getPerson().getSubjectId());
    assertTrue(
        authToken.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")));
    wireMockServer.verify(0, getRequestedFor(urlEqualTo(USER_INFO_ENDPOINT_PATH)));
  }

  @Test
  void testConvertUserTokenWithUserInfoEndpoint_success() {
    Map<String, Object> claims = new HashMap<>();
    claims.put("sub", "test-user-2");
    claims.put("client_id", "test-client-legacy");
    claims.put("scope", "openid profile");
    claims.put("name", "Fallback User");
    claims.put("email", "fallback.user@example.invalid");
    Jwt jwt = createFakeJwt(claims, "userToken2");

    wireMockServer.stubFor(
        get(USER_INFO_ENDPOINT_PATH)
            .withHeader("Authorization", equalTo("Bearer userToken2"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody(
                        "{\"name\": \"User Info Name\", \"email\": \"userinfo@example.invalid\", \"sub\": \"test-user-2\"}")));

    when(personRepository.findBySubjectId("test-user-2")).thenReturn(Optional.empty());
    Person newUser =
        Person.builder()
            .subjectId("test-user-2")
            .name("User Info Name")
            .email("userinfo@example.invalid")
            .build();
    when(personRepository.save(any(Person.class))).thenReturn(newUser);

    var authToken = converterWithUserInfo.convert(jwt);
    UserPrincipal principal = (UserPrincipal) authToken.getPrincipal();
    assertEquals("test-user-2", principal.getPerson().getSubjectId());
    assertEquals("User Info Name", principal.getName());
    assertEquals("userinfo@example.invalid", principal.getPerson().getEmail());
    wireMockServer.verify(1, getRequestedFor(urlEqualTo(USER_INFO_ENDPOINT_PATH)));
  }

  @Test
  void testConvertUserTokenWithUserInfoEndpoint_errorWhenEndpointFails() {
    Map<String, Object> claims = new HashMap<>();
    claims.put("sub", "test-user-3");
    claims.put("scope", "openid profile");
    claims.put("name", "Fallback User");
    claims.put("email", "fallback.user@example.invalid");
    Jwt jwt = createFakeJwt(claims, "userToken3");

    wireMockServer.stubFor(
        get(USER_INFO_ENDPOINT_PATH)
            .withHeader("Authorization", equalTo("Bearer userToken3"))
            .willReturn(aResponse().withStatus(500).withBody("Server Error")));

    when(personRepository.findBySubjectId("test-user-3")).thenReturn(Optional.empty());
    Person newUser =
        Person.builder()
            .subjectId("test-user-3")
            .name("Fallback User")
            .email("fallback.user@example.invalid")
            .build();
    when(personRepository.save(any(Person.class))).thenReturn(newUser);

    assertThrows(AuthenticationServiceException.class, () -> converterWithUserInfo.convert(jwt));
  }

  @Test
  void testUpdateExistingUser_updatesUserInformation() {
    Map<String, Object> claims = new HashMap<>();
    claims.put("sub", "test-user-4");
    claims.put("scope", "openid profile");
    claims.put("name", "Existing User Name");
    claims.put("email", "existing.user@example.invalid");
    Jwt jwt = createFakeJwt(claims, "userToken4");

    Person existingUser =
        Person.builder()
            .subjectId("test-user-4")
            .name("Existing User Name")
            .email("existing.user@example.invalid")
            .build();
    when(personRepository.findBySubjectId("test-user-4")).thenReturn(Optional.of(existingUser));

    wireMockServer.stubFor(
        get(USER_INFO_ENDPOINT_PATH)
            .withHeader("Authorization", equalTo("Bearer userToken4"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody(
                        "{\"name\": \"Updated User Name\", \"email\": \"updated.user@example.invalid\", \"sub\": \"test-user-4\"}")));

    Person updatedUser =
        Person.builder()
            .subjectId("test-user-4")
            .name("Updated User Name")
            .email("updated.user@example.invalid")
            .build();
    when(personRepository.save(any(Person.class))).thenReturn(updatedUser);

    var authToken = converterWithUserInfo.convert(jwt);
    UserPrincipal principal = (UserPrincipal) authToken.getPrincipal();
    assertEquals("Updated User Name", principal.getName());
    assertEquals("updated.user@example.invalid", principal.getPerson().getEmail());
  }

  @Test
  void testConvert_invalidJwt_throwsWrongJWTException() {
    Map<String, Object> claims = new HashMap<>();
    claims.put("scope", "openid profile"); // Missing required "sub" claim
    Jwt jwt = createFakeJwt(claims, "invalidToken");

    assertThrows(AuthenticationServiceException.class, () -> converterWithUserInfo.convert(jwt));
  }

  @Test
  void testParseUserAuthorities_withAdminRole() {
    Map<String, Object> claims = new HashMap<>();
    claims.put(TEST_AUTHZ_CLAIM, List.of(TEST_AUTHZ_ADMIN_VALUE));

    Collection<GrantedAuthority> authorities = converterWithUserInfo.parseUserAuthorities(claims);
    assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
  }

  @Test
  void testParseUserAuthorities_withResearcherRole() {
    Map<String, Object> claims = new HashMap<>();
    claims.put(TEST_AUTHZ_CLAIM, List.of("researcher"));

    Collection<GrantedAuthority> authorities = converterWithUserInfo.parseUserAuthorities(claims);
    assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_RESEARCHER")));
  }

  @Test
  void testGetAuthoritiesFromScope_withAuthzManagement() {
    Map<String, Object> claims = new HashMap<>();
    claims.put("scope", "negotiator_authz_management");
    Jwt jwt = createFakeJwt(claims, "scopeToken");

    Collection<GrantedAuthority> authorities = converterWithUserInfo.getAuthoritiesFromScope(jwt);
    assertTrue(
        authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_AUTHORIZATION_MANAGER")));
  }

  @Test
  void testGetAuthoritiesFromScope_withArrayScopes() {
    Map<String, Object> claims = new HashMap<>();
    claims.put("scope", List.of("negotiator_api", "negotiator_resource_management"));
    Jwt jwt = createFakeJwt(claims, "arrayScopeToken");

    Collection<GrantedAuthority> authorities = converterWithUserInfo.getAuthoritiesFromScope(jwt);

    assertTrue(
        authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_RESOURCE_MANAGER")));
  }

  @Test
  void testGetAuthoritiesFromScope_withSpaceSeparatedScopes() {
    Map<String, Object> claims = new HashMap<>();
    claims.put("scope", "negotiator_api negotiator_resource_management");
    Jwt jwt = createFakeJwt(claims, "spaceSeparatedScopeToken");

    Collection<GrantedAuthority> authorities = converterWithUserInfo.getAuthoritiesFromScope(jwt);

    assertTrue(
        authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_RESOURCE_MANAGER")));
  }

  @Test
  void testGetAuthoritiesFromScope_doesNotMatchPartialScopeName() {
    Map<String, Object> claims = new HashMap<>();
    claims.put("scope", "not_negotiator_authz_management");
    Jwt jwt = createFakeJwt(claims, "scopeToken");

    Collection<GrantedAuthority> authorities = converterWithUserInfo.getAuthoritiesFromScope(jwt);

    assertTrue(authorities.isEmpty());
  }

  @Test
  void testUserInfoCache_behavior() {
    Map<String, Object> claims = new HashMap<>();
    claims.put("sub", "test-user-cache");
    claims.put("scope", "openid profile");
    Jwt jwt = createFakeJwt(claims, "userTokenCache");

    wireMockServer.stubFor(
        get(USER_INFO_ENDPOINT_PATH)
            .withHeader("Authorization", equalTo("Bearer userTokenCache"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody(
                        "{\"name\": \"Cached Test User\", \"email\": \"cached.user@example.invalid\", \"sub\": \"test-user-cache\"}")));

    when(personRepository.findBySubjectId("test-user-cache")).thenReturn(Optional.empty());
    Person newUser =
        Person.builder()
            .subjectId("test-user-cache")
            .name("Cached Test User")
            .email("cached.user@example.invalid")
            .build();
    when(personRepository.save(any(Person.class))).thenReturn(newUser);

    // First call should hit the endpoint
    converterWithUserInfo.convert(jwt);
    // Second call should use cache
    converterWithUserInfo.convert(jwt);

    wireMockServer.verify(1, getRequestedFor(urlEqualTo(USER_INFO_ENDPOINT_PATH)));
  }

  @Test
  void testCleanCache_clearsUserInfoCache() {
    Map<String, Object> claims = new HashMap<>();
    claims.put("sub", "test-user-cache");
    claims.put("scope", "openid profile");
    Jwt jwt = createFakeJwt(claims, "userTokenCache");

    wireMockServer.stubFor(
        get(USER_INFO_ENDPOINT_PATH)
            .withHeader("Authorization", equalTo("Bearer userTokenCache"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody(
                        "{\"name\": \"Cached Test User\", \"email\": \"cached.user@example.invalid\", \"sub\": \"test-user-cache\"}")));

    when(personRepository.findBySubjectId("test-user-cache")).thenReturn(Optional.empty());
    when(personRepository.save(any(Person.class))).thenReturn(Person.builder().build());

    converterWithUserInfo.convert(jwt);
    assertEquals(1, CustomJWTAuthConverter.getUserInfoCacheSize());

    CustomJWTAuthConverter.cleanCache();
    assertEquals(0, CustomJWTAuthConverter.getUserInfoCacheSize());
  }
}
