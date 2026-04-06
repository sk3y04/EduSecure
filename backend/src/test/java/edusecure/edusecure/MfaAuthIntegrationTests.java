package edusecure.edusecure;

import edusecure.edusecure.entity.auth.MfaChallenge;
import edusecure.edusecure.repository.auth.MfaChallengeRepository;
import edusecure.edusecure.service.auth.TotpProvider;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MfaAuthIntegrationTests {

    private static final String AUTH_COOKIE_NAME = "EDUSECURE_AUTH";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TotpProvider totpProvider;

    @Autowired
    private MfaChallengeRepository mfaChallengeRepository;

    @Test
    void mfaSetupEnableLoginVerifyAndDisableFlowWorks() throws Exception {
        String email = "mfa-user-" + UUID.randomUUID() + "@example.com";
        String password = "StrongPass123!";

        Cookie registerCookie = registerAndReturnAuthCookie(email, password, "MFA Student");

        mockMvc.perform(get("/api/auth/mfa/status")
                        .cookie(registerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mfaEnabled").value(false))
                .andExpect(jsonPath("$.recoveryCodesRemaining").value(0));

        JsonNode setupJson = objectMapper.readTree(mockMvc.perform(post("/api/auth/mfa/setup")
                        .cookie(registerCookie)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mfaMethod").value("TOTP"))
                .andExpect(jsonPath("$.manualEntryKey").isNotEmpty())
                .andReturn().getResponse().getContentAsString());

        String manualEntryKey = textField(setupJson, "manualEntryKey");
        String currentTotp = totpProvider.generateCurrentCodeFromBase32(manualEntryKey);

        mockMvc.perform(post("/api/auth/mfa/enable")
                        .cookie(registerCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerificationPayload(currentTotp))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mfaEnabled").value(true))
                .andExpect(jsonPath("$.mfaMethod").value("TOTP"))
                .andExpect(jsonPath("$.recoveryCodes.length()").value(8));

        mockMvc.perform(get("/api/auth/mfa/status")
                        .cookie(registerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mfaEnabled").value(true))
                .andExpect(jsonPath("$.mfaMethod").value("TOTP"))
                .andExpect(jsonPath("$.recoveryCodesRemaining").value(8));

        JsonNode loginJson = loginAndReturnJson(email, password);
        String challengeId = textField(loginJson, "challengeId");

        MvcResult verifyResult = mockMvc.perform(post("/api/auth/mfa/verify")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyPayload(UUID.fromString(challengeId), totpProvider.generateCurrentCodeFromBase32(manualEntryKey)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authStatus").value("AUTHENTICATED"))
                .andExpect(jsonPath("$.mfaEnabled").value(true))
                .andExpect(jsonPath("$.amr[*]").value(hasItem("pwd")))
                .andExpect(jsonPath("$.amr[*]").value(hasItem("otp")))
                .andReturn();

        Cookie verifiedCookie = authCookieFrom(verifyResult);

        mockMvc.perform(post("/api/auth/mfa/verify")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyPayload(UUID.fromString(challengeId), totpProvider.generateCurrentCodeFromBase32(manualEntryKey)))))
                .andExpect(status().isGone());

        mockMvc.perform(get("/api/auth/me")
                        .cookie(verifiedCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));

        mockMvc.perform(post("/api/auth/mfa/disable")
                        .cookie(registerCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DisablePayload(password, totpProvider.generateCurrentCodeFromBase32(manualEntryKey)))))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/auth/mfa/status")
                        .cookie(registerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mfaEnabled").value(false))
                .andExpect(jsonPath("$.recoveryCodesRemaining").value(0));

        MvcResult finalLoginResult = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginPayload(email, password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authStatus").value("AUTHENTICATED"))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.challengeId").value(nullValue()))
                .andReturn();

        authCookieFrom(finalLoginResult);
    }

    @Test
    void recoveryCodeCanBeUsedOnlyOnce() throws Exception {
        String email = "recovery-user-" + UUID.randomUUID() + "@example.com";
        String password = "StrongPass123!";

        Cookie registerCookie = registerAndReturnAuthCookie(email, password, "Recovery Student");

        JsonNode setupJson = objectMapper.readTree(mockMvc.perform(post("/api/auth/mfa/setup")
                        .cookie(registerCookie)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        String manualEntryKey = textField(setupJson, "manualEntryKey");
        JsonNode enableJson = objectMapper.readTree(mockMvc.perform(post("/api/auth/mfa/enable")
                        .cookie(registerCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerificationPayload(totpProvider.generateCurrentCodeFromBase32(manualEntryKey)))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        String recoveryCode = firstTextElement(enableJson.required("recoveryCodes"));

        JsonNode firstChallenge = loginAndReturnJson(email, password);
        mockMvc.perform(post("/api/auth/mfa/verify")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyPayload(UUID.fromString(textField(firstChallenge, "challengeId")), recoveryCode))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authStatus").value("AUTHENTICATED"));

        mockMvc.perform(get("/api/auth/mfa/status")
                        .cookie(registerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recoveryCodesRemaining").value(7));

        JsonNode secondChallenge = loginAndReturnJson(email, password);
        mockMvc.perform(post("/api/auth/mfa/verify")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyPayload(UUID.fromString(textField(secondChallenge, "challengeId")), recoveryCode))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid MFA verification code"))
                .andExpect(jsonPath("$.errors._global[0]").value("Invalid MFA verification code"));
    }

    @Test
    void invalidTotpIsRejectedButChallengeCanStillBeCompletedWithinLimit() throws Exception {
        String email = "invalid-totp-" + UUID.randomUUID() + "@example.com";
        String password = "StrongPass123!";

        Cookie registerCookie = registerAndReturnAuthCookie(email, password, "Invalid Code Student");

        JsonNode setupJson = objectMapper.readTree(mockMvc.perform(post("/api/auth/mfa/setup")
                        .cookie(registerCookie)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        String manualEntryKey = textField(setupJson, "manualEntryKey");
        mockMvc.perform(post("/api/auth/mfa/enable")
                        .cookie(registerCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerificationPayload(totpProvider.generateCurrentCodeFromBase32(manualEntryKey)))))
                .andExpect(status().isOk());

        JsonNode challengeJson = loginAndReturnJson(email, password);
        UUID challengeId = UUID.fromString(textField(challengeJson, "challengeId"));

        mockMvc.perform(post("/api/auth/mfa/verify")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyPayload(challengeId, "000000"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid MFA verification code"))
                .andExpect(jsonPath("$.errors._global[0]").value("Invalid MFA verification code"));

        mockMvc.perform(post("/api/auth/mfa/verify")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyPayload(challengeId, totpProvider.generateCurrentCodeFromBase32(manualEntryKey)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authStatus").value("AUTHENTICATED"));
    }

    @Test
    void mfaChallengeLocksAfterMaximumFailedAttempts() throws Exception {
        String email = "mfa-max-attempts-" + UUID.randomUUID() + "@example.com";
        String password = "StrongPass123!";

        Cookie registerCookie = registerAndReturnAuthCookie(email, password, "MFA Max Attempts User");
        String manualEntryKey = setupAndEnableMfa(registerCookie);

        JsonNode challengeJson = loginAndReturnJson(email, password);
        UUID challengeId = UUID.fromString(textField(challengeJson, "challengeId"));

        for (int attempt = 1; attempt < 5; attempt++) {
            mockMvc.perform(post("/api/auth/mfa/verify")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new VerifyPayload(challengeId, "000000"))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Invalid MFA verification code"));
        }

        mockMvc.perform(post("/api/auth/mfa/verify")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyPayload(challengeId, "000000"))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("Too many MFA verification attempts"))
                .andExpect(jsonPath("$.errors._global[0]").value("Too many MFA verification attempts"));

        mockMvc.perform(post("/api/auth/mfa/verify")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyPayload(challengeId, totpProvider.generateCurrentCodeFromBase32(manualEntryKey)))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("Too many MFA verification attempts"));
    }

    @Test
    void expiredChallengeIsRejected() throws Exception {
        String email = "expired-challenge-" + UUID.randomUUID() + "@example.com";
        String password = "StrongPass123!";

        Cookie registerCookie = registerAndReturnAuthCookie(email, password, "Expired Challenge Student");

        JsonNode setupJson = objectMapper.readTree(mockMvc.perform(post("/api/auth/mfa/setup")
                        .cookie(registerCookie)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        String manualEntryKey = textField(setupJson, "manualEntryKey");
        mockMvc.perform(post("/api/auth/mfa/enable")
                        .cookie(registerCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerificationPayload(totpProvider.generateCurrentCodeFromBase32(manualEntryKey)))))
                .andExpect(status().isOk());

        JsonNode challengeJson = loginAndReturnJson(email, password);
        UUID challengeId = UUID.fromString(textField(challengeJson, "challengeId"));

        MfaChallenge challenge = mfaChallengeRepository.findById(challengeId)
                .orElseThrow(() -> new AssertionError("Expected persisted MFA challenge"));
        challenge.setExpiresAt(Instant.now().minusSeconds(1));
        mfaChallengeRepository.save(challenge);

        mockMvc.perform(post("/api/auth/mfa/verify")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyPayload(challengeId, totpProvider.generateCurrentCodeFromBase32(manualEntryKey)))))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.message").value("MFA challenge has expired or is no longer valid"))
                .andExpect(jsonPath("$.errors._global[0]").value("MFA challenge has expired or is no longer valid"));
    }

    @Test
    void mfaEnableReturnsFieldLevelValidationErrors() throws Exception {
        String email = "mfa-enable-validation-" + UUID.randomUUID() + "@example.com";
        String password = "StrongPass123!";

        Cookie registerCookie = registerAndReturnAuthCookie(email, password, "MFA Enable Validation User");

        mockMvc.perform(post("/api/auth/mfa/enable")
                        .cookie(registerCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerificationPayload(""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.verificationCode[0]").value("Verification code is required"));
    }

    @Test
    void mfaSetupReturnsStructuredErrorWhenAlreadyEnabled() throws Exception {
        String email = "mfa-setup-conflict-" + UUID.randomUUID() + "@example.com";
        String password = "StrongPass123!";

        Cookie registerCookie = registerAndReturnAuthCookie(email, password, "MFA Setup Conflict User");

        JsonNode setupJson = objectMapper.readTree(mockMvc.perform(post("/api/auth/mfa/setup")
                        .cookie(registerCookie)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        mockMvc.perform(post("/api/auth/mfa/enable")
                        .cookie(registerCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerificationPayload(totpProvider.generateCurrentCodeFromBase32(textField(setupJson, "manualEntryKey"))))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/mfa/setup")
                        .cookie(registerCookie)
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("MFA is already enabled for this account"))
                .andExpect(jsonPath("$.errors._global[0]").value("MFA is already enabled for this account"));
    }

    @Test
    void mfaVerifyReturnsFieldLevelValidationErrors() throws Exception {
        mockMvc.perform(post("/api/auth/mfa/verify")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.challengeId[0]").value("Challenge ID is required"))
                .andExpect(jsonPath("$.errors.verificationCode[0]").value("Verification code is required"));
    }

    @Test
    void mfaVerifyReturnsStructuredErrorForMalformedRequestBody() throws Exception {
        mockMvc.perform(post("/api/auth/mfa/verify")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"challengeId\":\"not-a-uuid\",\"verificationCode\":\"123456\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors._global[0]").value("Request body is malformed or contains invalid values"));
    }

    @Test
    void mfaDisableReturnsFieldLevelValidationErrors() throws Exception {
        String email = "mfa-disable-validation-" + UUID.randomUUID() + "@example.com";
        String password = "StrongPass123!";

        Cookie registerCookie = registerAndReturnAuthCookie(email, password, "MFA Disable Validation User");

        mockMvc.perform(post("/api/auth/mfa/disable")
                        .cookie(registerCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DisablePayload("", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.password[0]").value("Password is required"))
                .andExpect(jsonPath("$.errors.verificationCode[0]").value("Verification code is required"));
    }

    @Test
    void mfaDisableReturnsStructuredErrorForInvalidCredentials() throws Exception {
        String email = "mfa-disable-invalid-" + UUID.randomUUID() + "@example.com";
        String password = "StrongPass123!";

        Cookie registerCookie = registerAndReturnAuthCookie(email, password, "MFA Disable Invalid Credentials User");

        JsonNode setupJson = objectMapper.readTree(mockMvc.perform(post("/api/auth/mfa/setup")
                        .cookie(registerCookie)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        mockMvc.perform(post("/api/auth/mfa/enable")
                        .cookie(registerCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerificationPayload(totpProvider.generateCurrentCodeFromBase32(textField(setupJson, "manualEntryKey"))))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/mfa/disable")
                        .cookie(registerCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DisablePayload("WrongPass123!", totpProvider.generateCurrentCodeFromBase32(textField(setupJson, "manualEntryKey"))))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"))
                .andExpect(jsonPath("$.errors._global[0]").value("Invalid credentials"));
    }

    @Test
    void mfaDisableRejectsReusedRecoveryCode() throws Exception {
        String email = "mfa-disable-reused-recovery-" + UUID.randomUUID() + "@example.com";
        String password = "StrongPass123!";

        Cookie registerCookie = registerAndReturnAuthCookie(email, password, "MFA Disable Reused Recovery User");

        JsonNode setupJson = objectMapper.readTree(mockMvc.perform(post("/api/auth/mfa/setup")
                        .cookie(registerCookie)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        JsonNode enableJson = objectMapper.readTree(mockMvc.perform(post("/api/auth/mfa/enable")
                        .cookie(registerCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerificationPayload(totpProvider.generateCurrentCodeFromBase32(textField(setupJson, "manualEntryKey"))))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        String recoveryCode = firstTextElement(enableJson.required("recoveryCodes"));

        JsonNode challengeJson = loginAndReturnJson(email, password);
        mockMvc.perform(post("/api/auth/mfa/verify")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyPayload(UUID.fromString(textField(challengeJson, "challengeId")), recoveryCode))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authStatus").value("AUTHENTICATED"));

        mockMvc.perform(post("/api/auth/mfa/disable")
                        .cookie(registerCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DisablePayload(password, recoveryCode))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid MFA verification code"))
                .andExpect(jsonPath("$.errors._global[0]").value("Invalid MFA verification code"));

        mockMvc.perform(get("/api/auth/mfa/status")
                        .cookie(registerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mfaEnabled").value(true))
                .andExpect(jsonPath("$.recoveryCodesRemaining").value(7));
    }

    private Cookie registerAndReturnAuthCookie(String email, String password, String fullName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterPayload(email, password, fullName))))
                .andExpect(status().isCreated())
                .andReturn();
        return authCookieFrom(result);
    }

    private JsonNode loginAndReturnJson(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginPayload(email, password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authStatus").value("MFA_REQUIRED"))
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        if (!json.hasNonNull("challengeId")) {
            throw new AssertionError("Expected MFA challenge response");
        }
        return json;
    }

    private String setupAndEnableMfa(Cookie registerCookie) throws Exception {
        JsonNode setupJson = objectMapper.readTree(mockMvc.perform(post("/api/auth/mfa/setup")
                        .cookie(registerCookie)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        String manualEntryKey = textField(setupJson, "manualEntryKey");
        mockMvc.perform(post("/api/auth/mfa/enable")
                        .cookie(registerCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerificationPayload(totpProvider.generateCurrentCodeFromBase32(manualEntryKey)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mfaEnabled").value(true));

        return manualEntryKey;
    }

    private record RegisterPayload(String email, String password, String fullName) {
    }

    private record LoginPayload(String email, String password) {
    }

    private record VerificationPayload(String verificationCode) {
    }

    private record VerifyPayload(UUID challengeId, String verificationCode) {
    }

    private record DisablePayload(String password, String verificationCode) {
    }

    private static String textField(JsonNode objectNode, String fieldName) {
        return objectNode.required(fieldName).asString();
    }

    private static String firstTextElement(JsonNode arrayNode) {
        return arrayNode.required(0).asString();
    }

    private Cookie authCookieFrom(MvcResult result) {
        String setCookieHeader = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        org.assertj.core.api.Assertions.assertThat(setCookieHeader)
                .contains(AUTH_COOKIE_NAME + "=")
                .contains("HttpOnly");

        String cookieValue = setCookieHeader.split(";", 2)[0].substring((AUTH_COOKIE_NAME + "=").length());
        return new Cookie(AUTH_COOKIE_NAME, cookieValue);
    }
}

