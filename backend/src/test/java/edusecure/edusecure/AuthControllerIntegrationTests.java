package edusecure.edusecure;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTests {

    private static final String AUTH_COOKIE_NAME = "EDUSECURE_AUTH";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerLoginAndMeFlowWorks() throws Exception {
        String email = "student-" + UUID.randomUUID() + "@example.com";
        String password = "StrongPass123!";

        String registerPayload = objectMapper.writeValueAsString(new RegisterPayload(email, password, "Student Example"));

        MvcResult registrationResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.fullName").value("Student Example"))
                .andExpect(jsonPath("$.roles[*]").value(hasItem("STUDENT")))
                .andReturn();

        Cookie registrationCookie = authCookieFrom(registrationResult);

        mockMvc.perform(get("/api/auth/me")
                        .cookie(registrationCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.roles[*]").value(hasItem("STUDENT")));

        String loginPayload = objectMapper.writeValueAsString(new LoginPayload(email, password));

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andReturn();

        Cookie loginCookie = authCookieFrom(loginResult);

        mockMvc.perform(get("/api/auth/me")
                        .cookie(loginCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void meEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void duplicateRegistrationIsRejected() throws Exception {
        String email = "duplicate-" + UUID.randomUUID() + "@example.com";
        String payload = objectMapper.writeValueAsString(new RegisterPayload(email, "StrongPass123!", "Duplicate User"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email is already registered"))
                .andExpect(jsonPath("$.errors._global[0]").value("Email is already registered"));
    }

    @Test
    void registrationRejectsPasswordShorterThanEightCharacters() throws Exception {
        String email = "short-password-" + UUID.randomUUID() + "@example.com";
        String payload = objectMapper.writeValueAsString(new RegisterPayload(email, "Ab!12", "Short Password User"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registrationRejectsPasswordWithoutSpecialCharacter() throws Exception {
        String email = "no-special-" + UUID.randomUUID() + "@example.com";
        String payload = objectMapper.writeValueAsString(new RegisterPayload(email, "StrongPass123", "No Special User"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registrationRejectsPasswordWithoutUppercaseCharacter() throws Exception {
        String email = "no-uppercase-" + UUID.randomUUID() + "@example.com";
        String payload = objectMapper.writeValueAsString(new RegisterPayload(email, "strongpass123!", "No Uppercase User"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registrationRejectsPasswordWithoutLowercaseCharacter() throws Exception {
        String email = "no-lowercase-" + UUID.randomUUID() + "@example.com";
        String payload = objectMapper.writeValueAsString(new RegisterPayload(email, "STRONGPASS123!", "No Lowercase User"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registrationRejectsPasswordWithoutNumericCharacter() throws Exception {
        String email = "no-number-" + UUID.randomUUID() + "@example.com";
        String payload = objectMapper.writeValueAsString(new RegisterPayload(email, "StrongPass!", "No Number User"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registrationReturnsFieldLevelValidationErrorsForWeakPassword() throws Exception {
        String email = "weak-password-" + UUID.randomUUID() + "@example.com";
        String payload = objectMapper.writeValueAsString(new RegisterPayload(email, "weak", "Weak Password User"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.password", hasItems(
                        "Password must be at least 8 characters",
                        "Password must include at least one uppercase letter",
                        "Password must include at least one number",
                        "Password must include at least one special character"
                )));
    }

    @Test
    void loginReturnsFieldLevelValidationErrors() throws Exception {
        String payload = objectMapper.writeValueAsString(new LoginPayload("", ""));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.email", hasItem("Email is required")))
                .andExpect(jsonPath("$.errors.password", hasItem("Password is required")));
    }

    @Test
    void loginReturnsStructuredErrorForInvalidCredentials() throws Exception {
        String payload = objectMapper.writeValueAsString(new LoginPayload("missing@example.com", "WrongPass123!"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"))
                .andExpect(jsonPath("$.errors._global[0]").value("Invalid credentials"));
    }

    @Test
    void logoutClearsAuthenticationCookie() throws Exception {
        String email = "logout-" + UUID.randomUUID() + "@example.com";
        String payload = objectMapper.writeValueAsString(new RegisterPayload(email, "StrongPass123!", "Logout User"));

        Cookie authCookie = authCookieFrom(mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn());

        MvcResult logoutResult = mockMvc.perform(post("/api/auth/logout")
                        .cookie(authCookie))
                .andExpect(status().isNoContent())
                .andReturn();

        assertCookieCleared(logoutResult);
    }

    @Test
    void tamperedAuthCookieIsRejected() throws Exception {
        String email = "tampered-cookie-" + UUID.randomUUID() + "@example.com";
        String payload = objectMapper.writeValueAsString(new RegisterPayload(email, "StrongPass123!", "Tampered Cookie User"));

        Cookie authCookie = authCookieFrom(mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn());

        mockMvc.perform(get("/api/auth/me")
                        .cookie(new Cookie(AUTH_COOKIE_NAME, tamperJwt(authCookie.getValue()))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tamperedBearerTokenIsRejected() throws Exception {
        String email = "tampered-bearer-" + UUID.randomUUID() + "@example.com";
        String payload = objectMapper.writeValueAsString(new RegisterPayload(email, "StrongPass123!", "Tampered Bearer User"));

        Cookie authCookie = authCookieFrom(mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn());

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tamperJwt(authCookie.getValue())))
                .andExpect(status().isUnauthorized());
    }

    private record RegisterPayload(String email, String password, String fullName) {
    }

    private record LoginPayload(String email, String password) {
    }

    private String tamperJwt(String token) {
        String[] parts = token.split("\\.");
        org.assertj.core.api.Assertions.assertThat(parts).hasSize(3);

        String signature = parts[2];
        org.assertj.core.api.Assertions.assertThat(signature).isNotEmpty();

        char lastCharacter = signature.charAt(signature.length() - 1);
        char replacement = lastCharacter == 'a' ? 'b' : 'a';
        parts[2] = signature.substring(0, signature.length() - 1) + replacement;
        return String.join(".", parts);
    }

    private Cookie authCookieFrom(MvcResult result) {
        String setCookieHeader = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        org.assertj.core.api.Assertions.assertThat(setCookieHeader)
                .contains(AUTH_COOKIE_NAME + "=")
                .contains("HttpOnly")
                .contains("Path=/")
                .contains("SameSite=Lax")
                .doesNotContain("Secure");

        String cookieValue = setCookieHeader.split(";", 2)[0].substring((AUTH_COOKIE_NAME + "=").length());
        return new Cookie(AUTH_COOKIE_NAME, cookieValue);
    }

    private void assertCookieCleared(MvcResult result) {
        String setCookieHeader = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        org.assertj.core.api.Assertions.assertThat(setCookieHeader)
                .contains(AUTH_COOKIE_NAME + "=")
                .contains("Max-Age=0")
                .contains("HttpOnly")
                .contains("Path=/")
                .contains("SameSite=Lax");
    }
}

