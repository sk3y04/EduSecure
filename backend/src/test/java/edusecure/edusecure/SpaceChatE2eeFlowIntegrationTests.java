package edusecure.edusecure;

import edusecure.edusecure.document.spacechat.SpaceChatMessage;
import edusecure.edusecure.entity.audit.AuditActionType;
import edusecure.edusecure.entity.auth.Role;
import edusecure.edusecure.entity.auth.RoleName;
import edusecure.edusecure.entity.auth.User;
import edusecure.edusecure.entity.spacechatkey.SpaceChatKeyVersion;
import edusecure.edusecure.repository.audit.AuditLogRepository;
import edusecure.edusecure.repository.auth.RoleRepository;
import edusecure.edusecure.repository.auth.UserRepository;
import edusecure.edusecure.repository.spacechat.SpaceChatMessageRepository;
import edusecure.edusecure.repository.spacechatkey.SpaceChatKeyVersionRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.chat.enabled=true",
        "app.chat.e2ee.enabled=true",
        "app.chat.e2ee.require-registered-key=false"
})
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class SpaceChatE2eeFlowIntegrationTests {

    private static final String AUTH_COOKIE_NAME = "EDUSECURE_AUTH";

    @Container
    static final GenericContainer<?> mongo = new GenericContainer<>("mongo:7.0")
            .withExposedPorts(27017);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", () -> "mongodb://" + mongo.getHost() + ":" + mongo.getMappedPort(27017) + "/edusecure-chat-it");
        registry.add("spring.data.mongodb.database", () -> "edusecure-chat-it");
        registry.add("spring.data.mongodb.auto-index-creation", () -> true);
    }

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

    @Autowired
    private SpaceChatMessageRepository spaceChatMessageRepository;

    @Autowired
    private SpaceChatKeyVersionRepository spaceChatKeyVersionRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @BeforeEach
    void cleanMongoState() {
        spaceChatMessageRepository.deleteAll();
    }

    @Test
    void mixedLegacyAndEncryptedHistoryIsReturnedThroughChatApi() throws Exception {
        User lecturer = ensureUser("lecturer-chat-history-" + UUID.randomUUID() + "@example.com", "Lecturer Chat History", RoleName.LECTURER);
        User student = ensureUser("student-chat-history-" + UUID.randomUUID() + "@example.com", "Student Chat History", RoleName.STUDENT);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail(), "StrongPass123");
        Cookie studentCookie = loginAndReturnAuthCookie(student.getEmail(), "StrongPass123");
        String spaceId = createSpace(lecturerCookie, "Encrypted History Lab", "E2EE-HISTORY", "Mixed legacy and encrypted chat history.");
        addStudentToSpace(lecturerCookie, spaceId, student.getEmail());

        spaceChatMessageRepository.save(SpaceChatMessage.builder()
                .id(UUID.randomUUID().toString())
                .spaceId(spaceId)
                .authorUserId(lecturer.getId().toString())
                .authorDisplayName(lecturer.getFullName())
                .body("Legacy plaintext hello")
                .createdAt(Instant.parse("2026-04-12T09:00:00Z"))
                .build());
        spaceChatMessageRepository.save(SpaceChatMessage.builder()
                .id(UUID.randomUUID().toString())
                .spaceId(spaceId)
                .authorUserId(student.getId().toString())
                .authorDisplayName(student.getFullName())
                .keyVersion(1)
                .algorithm("AES_GCM_256")
                .nonce("nonce-1")
                .ciphertext("ciphertext-1")
                .contentType("text/plain")
                .plaintextLength(21)
                .createdAt(Instant.parse("2026-04-12T10:00:00Z"))
                .build());

        mockMvc.perform(get("/api/spaces/{spaceId}/chat/messages", spaceId)
                        .cookie(studentCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].body").value("Legacy plaintext hello"))
                .andExpect(jsonPath("$.items[0].ciphertext").doesNotExist())
                .andExpect(jsonPath("$.items[1].body").doesNotExist())
                .andExpect(jsonPath("$.items[1].keyVersion").value(1))
                .andExpect(jsonPath("$.items[1].algorithm").value("AES_GCM_256"))
                .andExpect(jsonPath("$.items[1].ciphertext").value("ciphertext-1"))
                .andExpect(jsonPath("$.items[1].plaintextLength").value(21));
    }

    @Test
    void encryptedMessageFlowRejectsPlaintextPostAndAcceptsEncryptedPostAfterKeyPublish() throws Exception {
        User lecturer = ensureUser("lecturer-chat-send-" + UUID.randomUUID() + "@example.com", "Lecturer Chat Send", RoleName.LECTURER);
        User student = ensureUser("student-chat-send-" + UUID.randomUUID() + "@example.com", "Student Chat Send", RoleName.STUDENT);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail(), "StrongPass123");
        Cookie studentCookie = loginAndReturnAuthCookie(student.getEmail(), "StrongPass123");
        String spaceId = createSpace(lecturerCookie, "Encrypted Send Lab", "E2EE-SEND", "Encrypted chat send flow.");
        addStudentToSpace(lecturerCookie, spaceId, student.getEmail());

        registerChatKey(lecturerCookie, "ECDH_P256", "{\"kty\":\"EC\",\"kid\":\"lecturer\"}", "lecturer-fp");
        registerChatKey(studentCookie, "ECDH_P256", "{\"kty\":\"EC\",\"kid\":\"student\"}", "student-fp");
        publishRoomKeyVersion(lecturerCookie, spaceId, 1, lecturer.getId(), student.getId());

        mockMvc.perform(post("/api/spaces/{spaceId}/chat/messages", spaceId)
                        .cookie(studentCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PlaintextMessagePayload("This should now be rejected"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Encrypted chat requests must not include a plaintext message body"));

        mockMvc.perform(post("/api/spaces/{spaceId}/chat/messages", spaceId)
                        .cookie(studentCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EncryptedMessagePayload(
                                1,
                                "AES_GCM_256",
                                "nonce-student-1",
                                "ciphertext-student-1",
                                "text/plain",
                                24
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.body").doesNotExist())
                .andExpect(jsonPath("$.keyVersion").value(1))
                .andExpect(jsonPath("$.ciphertext").value("ciphertext-student-1"))
                .andExpect(jsonPath("$.plaintextLength").value(24));

        assertThat(auditLogRepository.findTopByOrderByEventTimestampDesc())
                .isPresent()
                .get()
                .satisfies(auditLog -> {
                    assertThat(auditLog.getActionType()).isEqualTo(AuditActionType.SPACE_CHAT_MESSAGE_CREATED);
                    assertThat(auditLog.getDetailsJson())
                            .contains("bodyLength=24")
                            .doesNotContain("ciphertext-student-1")
                            .doesNotContain("This should now be rejected")
                            .doesNotContain(student.getEmail());
                    assertThat(auditLog.getIntegrityValue()).isNotBlank();
                });
    }

    @Test
    void membershipChangeMarksRoomKeyForRekeyAndBlocksFurtherEncryptedPosts() throws Exception {
        User lecturer = ensureUser("lecturer-chat-rekey-" + UUID.randomUUID() + "@example.com", "Lecturer Chat Rekey", RoleName.LECTURER);
        User student = ensureUser("student-chat-rekey-" + UUID.randomUUID() + "@example.com", "Student Chat Rekey", RoleName.STUDENT);
        User newStudent = ensureUser("student-chat-rekey-new-" + UUID.randomUUID() + "@example.com", "Student Chat Rekey New", RoleName.STUDENT);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail(), "StrongPass123");
        Cookie studentCookie = loginAndReturnAuthCookie(student.getEmail(), "StrongPass123");
        String spaceId = createSpace(lecturerCookie, "Encrypted Rekey Lab", "E2EE-REKEY", "Encrypted chat rekey flow.");
        addStudentToSpace(lecturerCookie, spaceId, student.getEmail());

        registerChatKey(lecturerCookie, "ECDH_P256", "{\"kty\":\"EC\",\"kid\":\"lecturer-rekey\"}", "lecturer-rekey-fp");
        registerChatKey(studentCookie, "ECDH_P256", "{\"kty\":\"EC\",\"kid\":\"student-rekey\"}", "student-rekey-fp");
        publishRoomKeyVersion(lecturerCookie, spaceId, 1, lecturer.getId(), student.getId());

        addStudentToSpace(lecturerCookie, spaceId, newStudent.getEmail());

        mockMvc.perform(get("/api/spaces/{spaceId}/chat/e2ee/state", spaceId)
                        .cookie(lecturerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeKeyVersion").value(1))
                .andExpect(jsonPath("$.requiresRekey").value(true));

        SpaceChatKeyVersion latestKeyVersion = spaceChatKeyVersionRepository.findFirstBySpaceIdOrderByKeyVersionDesc(UUID.fromString(spaceId)).orElseThrow();
        assertThat(latestKeyVersion.isRequiresRekey()).isTrue();

        mockMvc.perform(post("/api/spaces/{spaceId}/chat/messages", spaceId)
                        .cookie(studentCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EncryptedMessagePayload(
                                1,
                                "AES_GCM_256",
                                "nonce-student-rekey",
                                "ciphertext-student-rekey",
                                "text/plain",
                                19
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Encrypted chat room key must be rotated before sending new messages"));
    }

    @Test
    void nonMemberCannotAccessEncryptedChatStateOrMessages() throws Exception {
        User lecturer = ensureUser("lecturer-chat-guard-" + UUID.randomUUID() + "@example.com", "Lecturer Chat Guard", RoleName.LECTURER);
        User memberStudent = ensureUser("student-chat-member-" + UUID.randomUUID() + "@example.com", "Student Chat Member", RoleName.STUDENT);
        User outsider = ensureUser("student-chat-outsider-" + UUID.randomUUID() + "@example.com", "Student Chat Outsider", RoleName.STUDENT);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail(), "StrongPass123");
        Cookie outsiderCookie = loginAndReturnAuthCookie(outsider.getEmail(), "StrongPass123");
        String spaceId = createSpace(lecturerCookie, "Encrypted Guard Lab", "E2EE-GUARD", "Non-members must not access encrypted chat endpoints.");
        addStudentToSpace(lecturerCookie, spaceId, memberStudent.getEmail());

        mockMvc.perform(get("/api/spaces/{spaceId}/chat/e2ee/state", spaceId)
                        .cookie(outsiderCookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You are not allowed to view this space"));

        mockMvc.perform(get("/api/spaces/{spaceId}/chat/messages", spaceId)
                        .cookie(outsiderCookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You are not allowed to view this space"));
    }

    @Test
    void publishRoomKeyVersionFailsWhenParticipantKeyIsMissing() throws Exception {
        User lecturer = ensureUser("lecturer-chat-missing-key-" + UUID.randomUUID() + "@example.com", "Lecturer Chat Missing Key", RoleName.LECTURER);
        User student = ensureUser("student-chat-missing-key-" + UUID.randomUUID() + "@example.com", "Student Chat Missing Key", RoleName.STUDENT);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail(), "StrongPass123");
        String spaceId = createSpace(lecturerCookie, "Encrypted Missing Key Lab", "E2EE-MISSING", "Publishing should fail until all participants register keys.");
        addStudentToSpace(lecturerCookie, spaceId, student.getEmail());

        registerChatKey(lecturerCookie, "ECDH_P256", "{\"kty\":\"EC\",\"kid\":\"lecturer-missing\"}", "lecturer-missing-fp");

        mockMvc.perform(post("/api/spaces/{spaceId}/chat/e2ee/key-versions", spaceId)
                        .cookie(lecturerCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PublishKeyVersionPayload(
                                1,
                                "INITIAL_SETUP",
                                new PublishKeyVersionRecipient[] {
                                        new PublishKeyVersionRecipient(lecturer.getId(), "ECDH_P256_HKDF_SHA256_AES_GCM", "nonce-lecturer", "ciphertext-lecturer"),
                                        new PublishKeyVersionRecipient(student.getId(), "ECDH_P256_HKDF_SHA256_AES_GCM", "nonce-student", "ciphertext-student")
                                }
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Every current space participant must register a chat key before publishing a room key"));
    }

    @Test
    void roomKeyPublishAuditDoesNotLeakWrappedKeyMaterial() throws Exception {
        User lecturer = ensureUser("lecturer-chat-audit-" + UUID.randomUUID() + "@example.com", "Lecturer Chat Audit", RoleName.LECTURER);
        User student = ensureUser("student-chat-audit-" + UUID.randomUUID() + "@example.com", "Student Chat Audit", RoleName.STUDENT);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail(), "StrongPass123");
        Cookie studentCookie = loginAndReturnAuthCookie(student.getEmail(), "StrongPass123");
        String spaceId = createSpace(lecturerCookie, "Encrypted Audit Lab", "E2EE-AUDIT", "Audit records must stay metadata-only.");
        addStudentToSpace(lecturerCookie, spaceId, student.getEmail());

        registerChatKey(lecturerCookie, "ECDH_P256", "{\"kty\":\"EC\",\"kid\":\"lecturer-audit\"}", "lecturer-audit-fp");
        registerChatKey(studentCookie, "ECDH_P256", "{\"kty\":\"EC\",\"kid\":\"student-audit\"}", "student-audit-fp");
        publishRoomKeyVersion(lecturerCookie, spaceId, 1, lecturer.getId(), student.getId());

        assertThat(auditLogRepository.findTopByOrderByEventTimestampDesc())
                .isPresent()
                .get()
                .satisfies(auditLog -> {
                    assertThat(auditLog.getActionType()).isEqualTo(AuditActionType.SPACE_CHAT_KEY_VERSION_PUBLISHED);
                    assertThat(auditLog.getDetailsJson())
                            .contains("spaceId=" + spaceId)
                            .contains("keyVersion=1")
                            .contains("recipientCount=2")
                            .doesNotContain("ciphertext-lecturer")
                            .doesNotContain("ciphertext-student")
                            .doesNotContain("nonce-lecturer")
                            .doesNotContain("nonce-student");
                    assertThat(auditLog.getIntegrityValue()).isNotBlank();
                });
    }

    @Test
    void archivedSpaceRejectsEncryptedPostsAndRoomKeyPublishing() throws Exception {
        User lecturer = ensureUser("lecturer-chat-archived-" + UUID.randomUUID() + "@example.com", "Lecturer Chat Archived", RoleName.LECTURER);
        User student = ensureUser("student-chat-archived-" + UUID.randomUUID() + "@example.com", "Student Chat Archived", RoleName.STUDENT);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail(), "StrongPass123");
        Cookie studentCookie = loginAndReturnAuthCookie(student.getEmail(), "StrongPass123");
        String spaceId = createSpace(lecturerCookie, "Encrypted Archived Lab", "E2EE-ARCH", "Archived spaces should reject encrypted writes.");
        addStudentToSpace(lecturerCookie, spaceId, student.getEmail());

        registerChatKey(lecturerCookie, "ECDH_P256", "{\"kty\":\"EC\",\"kid\":\"lecturer-archived\"}", "lecturer-archived-fp");
        registerChatKey(studentCookie, "ECDH_P256", "{\"kty\":\"EC\",\"kid\":\"student-archived\"}", "student-archived-fp");
        publishRoomKeyVersion(lecturerCookie, spaceId, 1, lecturer.getId(), student.getId());
        archiveSpace(lecturerCookie, spaceId, "Encrypted Archived Lab", "E2EE-ARCH", "Archived spaces should reject encrypted writes.");

        mockMvc.perform(post("/api/spaces/{spaceId}/chat/messages", spaceId)
                        .cookie(studentCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EncryptedMessagePayload(
                                1,
                                "AES_GCM_256",
                                "nonce-archived",
                                "ciphertext-archived",
                                "text/plain",
                                18
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Archived spaces are read-only"));

        mockMvc.perform(post("/api/spaces/{spaceId}/chat/e2ee/key-versions", spaceId)
                        .cookie(lecturerCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PublishKeyVersionPayload(
                                2,
                                "MANUAL_ROTATION",
                                new PublishKeyVersionRecipient[] {
                                        new PublishKeyVersionRecipient(lecturer.getId(), "ECDH_P256_HKDF_SHA256_AES_GCM", "nonce-lecturer-2", "ciphertext-lecturer-2"),
                                        new PublishKeyVersionRecipient(student.getId(), "ECDH_P256_HKDF_SHA256_AES_GCM", "nonce-student-2", "ciphertext-student-2")
                                }
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Archived spaces are read-only"));
    }

    @Test
    void staleEncryptedKeyVersionIsRejectedAfterSecondPublish() throws Exception {
        User lecturer = ensureUser("lecturer-chat-stale-" + UUID.randomUUID() + "@example.com", "Lecturer Chat Stale", RoleName.LECTURER);
        User student = ensureUser("student-chat-stale-" + UUID.randomUUID() + "@example.com", "Student Chat Stale", RoleName.STUDENT);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail(), "StrongPass123");
        Cookie studentCookie = loginAndReturnAuthCookie(student.getEmail(), "StrongPass123");
        String spaceId = createSpace(lecturerCookie, "Encrypted Stale Lab", "E2EE-STALE", "Old key versions should be rejected after rotation.");
        addStudentToSpace(lecturerCookie, spaceId, student.getEmail());

        registerChatKey(lecturerCookie, "ECDH_P256", "{\"kty\":\"EC\",\"kid\":\"lecturer-stale\"}", "lecturer-stale-fp");
        registerChatKey(studentCookie, "ECDH_P256", "{\"kty\":\"EC\",\"kid\":\"student-stale\"}", "student-stale-fp");
        publishRoomKeyVersion(lecturerCookie, spaceId, 1, lecturer.getId(), student.getId());
        publishRoomKeyVersion(lecturerCookie, spaceId, 2, lecturer.getId(), student.getId());

        mockMvc.perform(post("/api/spaces/{spaceId}/chat/messages", spaceId)
                        .cookie(studentCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EncryptedMessagePayload(
                                1,
                                "AES_GCM_256",
                                "nonce-stale",
                                "ciphertext-stale",
                                "text/plain",
                                17
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Encrypted chat key version is no longer current for this space"));
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
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andReturn();

        return authCookieFrom(loginResult);
    }

    private String createSpace(Cookie lecturerCookie, String name, String code, String description) throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/spaces")
                        .cookie(lecturerCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateSpacePayload(name, code, description))))
                .andExpect(status().isCreated())
                .andReturn();
        return textField(objectMapper.readTree(createResult.getResponse().getContentAsString()), "id");
    }

    private void addStudentToSpace(Cookie lecturerCookie, String spaceId, String studentEmail) throws Exception {
        mockMvc.perform(post("/api/spaces/{spaceId}/students", spaceId)
                        .cookie(lecturerCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddStudentPayload(studentEmail))))
                .andExpect(status().isCreated());
    }

    private void registerChatKey(Cookie authCookie, String algorithm, String publicKeyJwk, String fingerprint) throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/chat/e2ee/me")
                        .cookie(authCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChatKeyPayload(algorithm, publicKeyJwk, fingerprint))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keyRegistered").value(true))
                .andExpect(jsonPath("$.fingerprint").value(fingerprint));
    }

    private void publishRoomKeyVersion(Cookie authCookie, String spaceId, int keyVersion, UUID lecturerId, UUID studentId) throws Exception {
        mockMvc.perform(post("/api/spaces/{spaceId}/chat/e2ee/key-versions", spaceId)
                        .cookie(authCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PublishKeyVersionPayload(
                                keyVersion,
                                "INITIAL_SETUP",
                                new PublishKeyVersionRecipient[] {
                                        new PublishKeyVersionRecipient(lecturerId, "ECDH_P256_HKDF_SHA256_AES_GCM", "nonce-lecturer", "ciphertext-lecturer"),
                                        new PublishKeyVersionRecipient(studentId, "ECDH_P256_HKDF_SHA256_AES_GCM", "nonce-student", "ciphertext-student")
                                }
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keyVersion").value(keyVersion))
                .andExpect(jsonPath("$.recipientCount").value(2));
    }

    private void archiveSpace(Cookie authCookie, String spaceId, String name, String code, String description) throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/spaces/{spaceId}", spaceId)
                        .cookie(authCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateSpacePayload(name, code, description, true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(true));
    }

    private Cookie authCookieFrom(MvcResult result) {
        String setCookieHeader = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookieHeader)
                .contains(AUTH_COOKIE_NAME + "=")
                .contains("HttpOnly");

        String cookieValue = setCookieHeader.split(";", 2)[0].substring((AUTH_COOKIE_NAME + "=").length());
        return new Cookie(AUTH_COOKIE_NAME, cookieValue);
    }

    private static String textField(JsonNode objectNode, String fieldName) {
        return objectNode.required(fieldName).asString();
    }

    private record LoginPayload(String email, String password) {
    }

    private record CreateSpacePayload(String name, String code, String description) {
    }

    private record AddStudentPayload(String studentEmail) {
    }

    private record UpdateSpacePayload(String name, String code, String description, boolean archived) {
    }

    private record ChatKeyPayload(String algorithm, String publicKeyJwk, String fingerprint) {
    }

    private record PublishKeyVersionPayload(int keyVersion, String rotationReason, PublishKeyVersionRecipient[] recipients) {
    }

    private record PublishKeyVersionRecipient(UUID recipientUserId, String wrapAlgorithm, String wrapNonce, String wrappedKeyCiphertext) {
    }

    private record PlaintextMessagePayload(String body) {
    }

    private record EncryptedMessagePayload(
            int keyVersion,
            String algorithm,
            String nonce,
            String ciphertext,
            String contentType,
            int plaintextLength
    ) {
    }
}



