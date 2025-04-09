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
  private static final String TEST_AUTHZ_RESEARCHER_VALUE = "researcher";
  private static final String TEST_AUTHZ_BIOBANKER_VALUE = "biobanker";
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
            personRepository,
            userInfoEndpointUrl,
            TEST_AUTHZ_CLAIM,
            TEST_AUTHZ_ADMIN_VALUE,
            TEST_AUTHZ_RESEARCHER_VALUE,
            TEST_AUTHZ_BIOBANKER_VALUE);
    converterWithoutUserInfo =
        new CustomJWTAuthConverter(
            personRepository,
            null,
            TEST_AUTHZ_CLAIM,
            TEST_AUTHZ_ADMIN_VALUE,
            TEST_AUTHZ_RESEARCHER_VALUE,
            TEST_AUTHZ_BIOBANKER_VALUE);
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
  void testConvertUserTokenWithoutUserInfoEndpoint_createsNewUserFromJwtClaims() {
    Map<String, Object> claims = new HashMap<>();
    claims.put("sub", "user1");
    claims.put("name", "User One");
    claims.put("email", "user1@example.com");
    claims.put("scope", "profile");
    Jwt jwt = createFakeJwt(claims, "userToken");

    when(personRepository.findBySubjectId("user1")).thenReturn(Optional.empty());
    Person newUser =
        Person.builder().subjectId("user1").name("User One").email("user1@example.com").build();
    when(personRepository.save(any(Person.class))).thenReturn(newUser);

    var authToken = converterWithoutUserInfo.convert(jwt);
    UserPrincipal principal = (UserPrincipal) authToken.getPrincipal();
    assertEquals("user1", principal.getPerson().getSubjectId());
    assertEquals("User One", principal.getName());
    assertEquals("user1@example.com", principal.getPerson().getEmail());
  }

  @Test
  void testConvertUserTokenWithUserInfoEndpoint_success() {
    Map<String, Object> claims = new HashMap<>();
    claims.put("sub", "user2");
    claims.put("scope", "openid profile");
    claims.put("name", "Fallback Name");
    claims.put("email", "fallback@example.com");
    Jwt jwt = createFakeJwt(claims, "userToken2");

    wireMockServer.stubFor(
        get(USER_INFO_ENDPOINT_PATH)
            .withHeader("Authorization", equalTo("Bearer userToken2"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody(
                        "{\"name\": \"Jane Doe\", \"email\": \"jane@example.com\", \"sub\": \"user2\"}")));

    when(personRepository.findBySubjectId("user2")).thenReturn(Optional.empty());
    Person newUser =
        Person.builder().subjectId("user2").name("Jane Doe").email("jane@example.com").build();
    when(personRepository.save(any(Person.class))).thenReturn(newUser);

    var authToken = converterWithUserInfo.convert(jwt);
    UserPrincipal principal = (UserPrincipal) authToken.getPrincipal();
    assertEquals("user2", principal.getPerson().getSubjectId());
    assertEquals("Jane Doe", principal.getName());
    assertEquals("jane@example.com", principal.getPerson().getEmail());
  }

  @Test
  void testConvertUserTokenWithUserInfoEndpoint_errorWhenEndpointFails() {
    Map<String, Object> claims = new HashMap<>();
    claims.put("sub", "user3");
    claims.put("scope", "openid profile");
    claims.put("name", "Fallback Name");
    claims.put("email", "fallback@example.com");
    Jwt jwt = createFakeJwt(claims, "userToken3");

    wireMockServer.stubFor(
        get(USER_INFO_ENDPOINT_PATH)
            .withHeader("Authorization", equalTo("Bearer userToken3"))
            .willReturn(aResponse().withStatus(500).withBody("Server Error")));

    when(personRepository.findBySubjectId("user3")).thenReturn(Optional.empty());
    Person newUser =
        Person.builder()
            .subjectId("user3")
            .name("Fallback Name")
            .email("fallback@example.com")
            .build();
    when(personRepository.save(any(Person.class))).thenReturn(newUser);

    assertThrows(AuthenticationServiceException.class, () -> converterWithUserInfo.convert(jwt));
  }

  @Test
  void testUpdateExistingUser_updatesUserInformation() {
    Map<String, Object> claims = new HashMap<>();
    claims.put("sub", "user4");
    claims.put("scope", "openid profile");
    claims.put("name", "Old Name");
    claims.put("email", "old@example.com");
    Jwt jwt = createFakeJwt(claims, "userToken4");

    Person existingUser =
        Person.builder().subjectId("user4").name("Old Name").email("old@example.com").build();
    when(personRepository.findBySubjectId("user4")).thenReturn(Optional.of(existingUser));

    wireMockServer.stubFor(
        get(USER_INFO_ENDPOINT_PATH)
            .withHeader("Authorization", equalTo("Bearer userToken4"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody(
                        "{\"name\": \"New Name\", \"email\": \"new@example.com\", \"sub\": \"user4\"}")));

    Person updatedUser =
        Person.builder().subjectId("user4").name("New Name").email("new@example.com").build();
    when(personRepository.save(any(Person.class))).thenReturn(updatedUser);

    var authToken = converterWithUserInfo.convert(jwt);
    UserPrincipal principal = (UserPrincipal) authToken.getPrincipal();
    assertEquals("New Name", principal.getName());
    assertEquals("new@example.com", principal.getPerson().getEmail());
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
    claims.put("roles", List.of("admin"));

    Collection<GrantedAuthority> authorities = converterWithUserInfo.parseUserAuthorities(claims);
    assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
  }

  @Test
  void testParseUserAuthorities_withResearcherRole() {
    Map<String, Object> claims = new HashMap<>();
    claims.put("roles", List.of("researcher"));

    Collection<GrantedAuthority> authorities = converterWithUserInfo.parseUserAuthorities(claims);
    assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_RESEARCHER")));
  }

  @Test
  void testParseUserAuthorities_withBiobankerRole() {
    Map<String, Object> claims = new HashMap<>();
    claims.put("roles", List.of("biobanker"));

    Collection<GrantedAuthority> authorities = converterWithUserInfo.parseUserAuthorities(claims);
    assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_REPRESENTATIVE")));
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
  void testUserInfoCache_behavior() {
    Map<String, Object> claims = new HashMap<>();
    claims.put("sub", "cacheUser");
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
                        "{\"name\": \"Cached User\", \"email\": \"cached@user.com\", \"sub\": \"cacheUser\"}")));

    when(personRepository.findBySubjectId("cacheUser")).thenReturn(Optional.empty());
    Person newUser =
        Person.builder()
            .subjectId("cacheUser")
            .name("Cached User")
            .email("cached@user.com")
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
    claims.put("sub", "cacheUser");
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
                        "{\"name\": \"Cached User\", \"email\": \"cached@user.com\", \"sub\": \"cacheUser\"}")));

    when(personRepository.findBySubjectId("cacheUser")).thenReturn(Optional.empty());
    when(personRepository.save(any(Person.class))).thenReturn(Person.builder().build());

    converterWithUserInfo.convert(jwt);
    assertEquals(1, CustomJWTAuthConverter.getUserInfoCacheSize());

    CustomJWTAuthConverter.cleanCache();
    assertEquals(0, CustomJWTAuthConverter.getUserInfoCacheSize());
  }
}
