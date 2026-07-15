package com.restaurant.ops;

import static org.assertj.core.api.Assertions.assertThat;

import com.restaurant.ops.auth.AuthDtos;
import com.restaurant.ops.employee.Position;
import com.restaurant.ops.employee.StoreCode;
import com.restaurant.ops.invitation.InvitationDtos;
import com.restaurant.ops.profile.ProfileDtos;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
@Testcontainers
class MobileProductionFlowIntegrationTest {
  @Container
  static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
      .withDatabaseName("restaurant_ops_mobile_flow")
      .withUsername("test")
      .withPassword("test");

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", mysql::getJdbcUrl);
    registry.add("spring.datasource.username", mysql::getUsername);
    registry.add("spring.datasource.password", mysql::getPassword);
    registry.add("app.jwt.secret", () -> "01234567890123456789012345678901");
    registry.add("app.toast-pin.encryption-key", () -> "toast-pin-test-key-32-bytes-long");
    registry.add("app.public-base-url", () -> "https://app.example.com");
    registry.add("app.bootstrap.enabled", () -> "true");
    registry.add("app.bootstrap.email", () -> "manager.mobile-flow@example.com");
    registry.add("app.bootstrap.phone", () -> "+12065558000");
    registry.add("app.bootstrap.password", () -> "manager-password");
    registry.add("app.bootstrap.english-name", () -> "Morgan Manager");
    registry.add("app.bootstrap.preferred-name", () -> "Morgan");
    registry.add("app.bootstrap.toast-pin", () -> "8000");
    registry.add("app.development-seed.enabled", () -> "true");
  }

  @Autowired TestRestTemplate rest;

  @BeforeEach
  void configurePatchCapableHttpClient() {
    rest.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
  }

  @Test
  void publicHealthEndpointDoesNotRequireAuthentication() {
    ResponseEntity<Map<String, String>> response = rest.exchange(
        "/api/public/health",
        HttpMethod.GET,
        HttpEntity.EMPTY,
        new ParameterizedTypeReference<>() {}
    );

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).containsEntry("status", "ok");
  }

  @Test
  void authenticatedApiCorsPreflightIsAllowedForMobileWeb() {
    HttpHeaders headers = new HttpHeaders();
    headers.setOrigin("http://localhost:8081");
    headers.setAccessControlRequestMethod(HttpMethod.PUT);
    headers.setAccessControlRequestHeaders(java.util.List.of("authorization", "content-type"));

    ResponseEntity<Void> response = rest.exchange("/api/inventory-counts/40/lines", HttpMethod.OPTIONS, new HttpEntity<>(headers), Void.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getHeaders().getAccessControlAllowOrigin()).isEqualTo("http://localhost:8081");
    assertThat(response.getHeaders().getAccessControlAllowMethods()).contains(HttpMethod.PUT);
  }

  @Test
  void loginEndpointCorsAndTokenFlowWorkForDevelopmentAccount() {
    HttpHeaders preflightHeaders = new HttpHeaders();
    preflightHeaders.setOrigin("http://localhost:8081");
    preflightHeaders.setAccessControlRequestMethod(HttpMethod.POST);
    preflightHeaders.setAccessControlRequestHeaders(java.util.List.of("content-type"));

    ResponseEntity<Void> preflight = rest.exchange("/api/auth/login", HttpMethod.OPTIONS, new HttpEntity<>(preflightHeaders), Void.class);

    assertThat(preflight.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(preflight.getHeaders().getAccessControlAllowOrigin()).isEqualTo("http://localhost:8081");
    assertThat(preflight.getHeaders().getAccessControlAllowMethods()).contains(HttpMethod.POST);

    ResponseEntity<AuthDtos.TokenResponse> login = rest.exchange(
        "/api/auth/login",
        HttpMethod.POST,
        new HttpEntity<>(new AuthDtos.LoginRequest("test_admin", "Test1234!"), jsonHeadersWithOrigin()),
        AuthDtos.TokenResponse.class
    );

    assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(login.getHeaders().getAccessControlAllowOrigin()).isEqualTo("http://localhost:8081");
    assertThat(login.getBody()).isNotNull();
    assertThat(login.getBody().accessToken()).isNotBlank();

    ResponseEntity<ProfileDtos.EmployeePrivateResponse> me = rest.exchange(
        "/api/me",
        HttpMethod.GET,
        authenticated(login.getBody().accessToken(), null, true),
        ProfileDtos.EmployeePrivateResponse.class
    );

    assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(me.getHeaders().getAccessControlAllowOrigin()).isEqualTo("http://localhost:8081");
    assertThat(me.getBody()).isNotNull();
    assertThat(me.getBody().email()).isEqualTo("test_admin@dev.example.com");
  }

  @Test
  void invalidLoginAndUnauthenticatedProtectedRequestsReturnCorsReadableResponses() {
    ResponseEntity<Map<String, String>> invalidLogin = rest.exchange(
        "/api/auth/login",
        HttpMethod.POST,
        new HttpEntity<>(new AuthDtos.LoginRequest("test_admin", "wrong-password"), jsonHeadersWithOrigin()),
        new ParameterizedTypeReference<>() {}
    );

    assertThat(invalidLogin.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(invalidLogin.getHeaders().getAccessControlAllowOrigin()).isEqualTo("http://localhost:8081");
    assertThat(invalidLogin.getBody()).containsEntry("code", "AUTH_INVALID_CREDENTIALS");

    HttpHeaders headers = new HttpHeaders();
    headers.setOrigin("http://localhost:8081");
    ResponseEntity<String> unauthenticatedMe = rest.exchange("/api/me", HttpMethod.GET, new HttpEntity<>(headers), String.class);

    assertThat(unauthenticatedMe.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    assertThat(unauthenticatedMe.getHeaders().getAccessControlAllowOrigin()).isEqualTo("http://localhost:8081");
  }

  @Test
  void mobileProductionFlowUsesRealBackendEndpoints() {
    AuthDtos.TokenResponse managerTokens = login("manager.mobile-flow@example.com", "manager-password");

    ResponseEntity<InvitationDtos.InvitationResponse> createdInvitation = rest.exchange(
        "/api/manager/invitations",
        HttpMethod.POST,
        authenticated(managerTokens.accessToken(), Map.of("positions", Set.of(Position.HOST))),
        InvitationDtos.InvitationResponse.class
    );
    assertThat(createdInvitation.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(createdInvitation.getBody()).isNotNull();
    String activationLink = createdInvitation.getBody().activationLink();
    assertThat(activationLink).contains("token=");
    String invitationToken = activationLink.substring(activationLink.indexOf("token=") + "token=".length());

    ResponseEntity<InvitationDtos.ValidateInvitationResponse> validatedInvitation = rest.getForEntity(
        "/api/public/invitations/validate?token={token}",
        InvitationDtos.ValidateInvitationResponse.class,
        invitationToken
    );
    assertThat(validatedInvitation.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(validatedInvitation.getBody()).isNotNull();
    assertThat(validatedInvitation.getBody().valid()).isTrue();
    assertThat(validatedInvitation.getBody().positions()).containsExactly(Position.HOST);

    ResponseEntity<AuthDtos.ActivationResponse> activation = rest.postForEntity(
        "/api/public/invitations/activate",
        new AuthDtos.ActivationRequest(
            invitationToken,
            "Avery Production",
            "Avery",
            "avery.production@example.com",
            "+12065558001",
            "employee-password",
            "8001",
            StoreCode.SEATTLE,
            Set.of(StoreCode.SEATTLE, StoreCode.REDMOND)
        ),
        AuthDtos.ActivationResponse.class
    );
    assertThat(activation.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(activation.getBody()).isNotNull();
    assertThat(activation.getBody().tokens().accessToken()).isNotBlank();

    AuthDtos.TokenResponse employeeTokens = login("avery.production@example.com", "employee-password");

    ProfileDtos.EmployeePrivateResponse employeeProfile = getMe(employeeTokens.accessToken());
    assertThat(employeeProfile.eligibleStores()).containsExactlyInAnyOrder(StoreCode.SEATTLE, StoreCode.REDMOND);

    ResponseEntity<ProfileDtos.EmployeePrivateResponse> updatedStores = rest.exchange(
        "/api/me/stores",
        HttpMethod.PATCH,
        authenticated(employeeTokens.accessToken(), Map.of("eligibleStores", Set.of(StoreCode.REDMOND))),
        ProfileDtos.EmployeePrivateResponse.class
    );
    assertThat(updatedStores.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(updatedStores.getBody()).isNotNull();
    assertThat(updatedStores.getBody().eligibleStores()).containsExactly(StoreCode.REDMOND);

    ResponseEntity<ProfileDtos.EmployeePrivateResponse> updatedProfile = rest.exchange(
        "/api/me/profile",
        HttpMethod.PATCH,
        authenticated(employeeTokens.accessToken(), Map.of("englishName", "Avery Verified", "preferredName", "Ave")),
        ProfileDtos.EmployeePrivateResponse.class
    );
    assertThat(updatedProfile.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(updatedProfile.getBody()).isNotNull();
    assertThat(updatedProfile.getBody().displayName()).isEqualTo("Ave");

    ResponseEntity<Map<String, String>> toastPin = rest.exchange(
        "/api/me/toast-pin",
        HttpMethod.PATCH,
        authenticated(employeeTokens.accessToken(), Map.of("toastPin", "8002")),
        new ParameterizedTypeReference<>() {}
    );
    assertThat(toastPin.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(toastPin.getBody()).containsEntry("status", "ok");

    ResponseEntity<java.util.List<ProfileDtos.EmployeePrivateResponse>> employees = rest.exchange(
        "/api/manager/employees",
        HttpMethod.GET,
        authenticated(managerTokens.accessToken(), null),
        new ParameterizedTypeReference<>() {}
    );
    assertThat(employees.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(employees.getBody()).isNotNull();
    assertThat(employees.getBody())
        .anySatisfy(employee -> {
          assertThat(employee.email()).isEqualTo("avery.production@example.com");
          assertThat(employee.displayName()).isEqualTo("Ave");
          assertThat(employee.eligibleStores()).containsExactly(StoreCode.REDMOND);
          assertThat(employee.toastPin()).isEqualTo("8002");
        });
  }

  private AuthDtos.TokenResponse login(String identifier, String password) {
    ResponseEntity<AuthDtos.TokenResponse> login = rest.postForEntity(
        "/api/auth/login",
        new AuthDtos.LoginRequest(identifier, password),
        AuthDtos.TokenResponse.class
    );
    assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(login.getBody()).isNotNull();
    return login.getBody();
  }

  private ProfileDtos.EmployeePrivateResponse getMe(String accessToken) {
    ResponseEntity<ProfileDtos.EmployeePrivateResponse> me = rest.exchange(
        "/api/me",
        HttpMethod.GET,
        authenticated(accessToken, null),
        ProfileDtos.EmployeePrivateResponse.class
    );
    assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(me.getBody()).isNotNull();
    return me.getBody();
  }

  private HttpEntity<?> authenticated(String accessToken, Object body) {
    return authenticated(accessToken, body, false);
  }

  private HttpEntity<?> authenticated(String accessToken, Object body, boolean includeOrigin) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);
    if (includeOrigin) {
      headers.setOrigin("http://localhost:8081");
    }
    return new HttpEntity<>(body, headers);
  }

  private HttpHeaders jsonHeadersWithOrigin() {
    HttpHeaders headers = new HttpHeaders();
    headers.setOrigin("http://localhost:8081");
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }
}
