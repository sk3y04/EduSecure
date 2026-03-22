package edusecure.edusecure;

import edusecure.edusecure.entity.auth.Role;
import edusecure.edusecure.entity.auth.RoleName;
import edusecure.edusecure.entity.auth.User;
import edusecure.edusecure.repository.auth.RoleRepository;
import edusecure.edusecure.repository.auth.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AesDemoIntegrationTests {

    private static final String AUTH_COOKIE_NAME = "EDUSECURE_AUTH";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void authenticatedUserCanEncryptAndDecryptMessage() throws Exception {
        User student = ensureUser("student-aes-" + UUID.randomUUID() + "@example.com", "Student AES", RoleName.STUDENT);
        Cookie authCookie = loginAndReturnAuthCookie(student.getEmail(), "StrongPass123");

        String plaintext = "Confidential lecturer feedback message";
        String encryptPayload = objectMapper.writeValueAsString(new EncryptPayload(plaintext));

        MvcResult encryptResult = mockMvc.perform(post("/api/crypto-demo/encrypt")
                        .cookie(authCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(encryptPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.algorithm").value("AES/GCM/NoPadding"))
                .andExpect(jsonPath("$.nonce").isNotEmpty())
                .andExpect(jsonPath("$.ciphertext").isNotEmpty())
                .andReturn();

        JsonNode encryptionJson = objectMapper.readTree(encryptResult.getResponse().getContentAsString());
        String nonce = encryptionJson.get("nonce").asText();
        String ciphertext = encryptionJson.get("ciphertext").asText();
        assertThat(ciphertext).isNotEqualTo(plaintext);

        String decryptPayload = objectMapper.writeValueAsString(new DecryptPayload(nonce, ciphertext));
        mockMvc.perform(post("/api/crypto-demo/decrypt")
                        .cookie(authCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(decryptPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plaintext").value(plaintext));
    }

    @Test
    void tamperedCiphertextIsRejected() throws Exception {
        User student = ensureUser("student-aes-tamper-" + UUID.randomUUID() + "@example.com", "Student AES Tamper", RoleName.STUDENT);
        Cookie authCookie = loginAndReturnAuthCookie(student.getEmail(), "StrongPass123");

        String encryptPayload = objectMapper.writeValueAsString(new EncryptPayload("Sensitive note"));
        MvcResult encryptResult = mockMvc.perform(post("/api/crypto-demo/encrypt")
                        .cookie(authCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(encryptPayload))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode encryptionJson = objectMapper.readTree(encryptResult.getResponse().getContentAsString());
        String nonce = encryptionJson.get("nonce").asText();
        String ciphertext = encryptionJson.get("ciphertext").asText();
        String tamperedCiphertext = ciphertext.substring(0, ciphertext.length() - 2) + "AA";

        String decryptPayload = objectMapper.writeValueAsString(new DecryptPayload(nonce, tamperedCiphertext));
        mockMvc.perform(post("/api/crypto-demo/decrypt")
                        .cookie(authCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(decryptPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void malformedBase64IsRejected() throws Exception {
        User student = ensureUser("student-aes-b64-" + UUID.randomUUID() + "@example.com", "Student AES Base64", RoleName.STUDENT);
        Cookie authCookie = loginAndReturnAuthCookie(student.getEmail(), "StrongPass123");

        String decryptPayload = objectMapper.writeValueAsString(new DecryptPayload("not-base64", "also-not-base64"));
        mockMvc.perform(post("/api/crypto-demo/decrypt")
                        .cookie(authCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(decryptPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unauthenticatedAccessIsRejected() throws Exception {
        String encryptPayload = objectMapper.writeValueAsString(new EncryptPayload("No token should fail"));
        mockMvc.perform(post("/api/crypto-demo/encrypt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(encryptPayload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void emptyPlaintextFailsValidation() throws Exception {
        User student = ensureUser("student-aes-empty-" + UUID.randomUUID() + "@example.com", "Student AES Empty", RoleName.STUDENT);
        Cookie authCookie = loginAndReturnAuthCookie(student.getEmail(), "StrongPass123");

        String encryptPayload = objectMapper.writeValueAsString(new EncryptPayload(""));
        mockMvc.perform(post("/api/crypto-demo/encrypt")
                        .cookie(authCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(encryptPayload))
                .andExpect(status().isBadRequest());
    }

    private User ensureUser(String email, String fullName, RoleName roleName) {
        Role role = roleRepository.findByName(roleName)
                .orElseGet(() -> roleRepository.save(Role.builder().name(roleName).build()));

        return userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.save(User.builder()
                        .email(email)
                        .passwordHash(passwordEncoder.encode("StrongPass123"))
                        .fullName(fullName)
                        .roles(Set.of(role))
                        .build()));
    }

    private Cookie loginAndReturnAuthCookie(String email, String password) throws Exception {
        String loginPayload = objectMapper.writeValueAsString(new LoginPayload(email, password));
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andReturn();

        return authCookieFrom(loginResult);
    }

    private record LoginPayload(String email, String password) {
    }

    private record EncryptPayload(String plaintext) {
    }

    private record DecryptPayload(String nonce, String ciphertext) {
    }

    private Cookie authCookieFrom(MvcResult result) {
        String setCookieHeader = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookieHeader)
                .contains(AUTH_COOKIE_NAME + "=")
                .contains("HttpOnly");

        String cookieValue = setCookieHeader.split(";", 2)[0].substring((AUTH_COOKIE_NAME + "=").length());
        return new Cookie(AUTH_COOKIE_NAME, cookieValue);
    }
}

