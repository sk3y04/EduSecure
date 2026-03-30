package edusecure.edusecure;

import edusecure.edusecure.entity.audit.AuditLog;
import edusecure.edusecure.entity.submission.Submission;
import edusecure.edusecure.entity.auth.Role;
import edusecure.edusecure.entity.auth.RoleName;
import edusecure.edusecure.entity.auth.User;
import edusecure.edusecure.repository.audit.AuditLogRepository;
import edusecure.edusecure.repository.auth.RoleRepository;
import edusecure.edusecure.repository.submission.SubmissionRepository;
import edusecure.edusecure.repository.auth.UserRepository;
import edusecure.edusecure.service.submission.SubmissionContentStore;
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

import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SubmissionFlowIntegrationTests {

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

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private SubmissionContentStore submissionContentStore;

    @Test
    void lecturerCreatesAssignmentAndStudentSubmitsVerifiedWork() throws Exception {
        User lecturer = ensureUser("lecturer-" + UUID.randomUUID() + "@example.com", "Lecturer Example", RoleName.LECTURER);
        User student = ensureUser("student-submission-" + UUID.randomUUID() + "@example.com", "Student Submitter", RoleName.STUDENT);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail(), "StrongPass123");
        Cookie studentCookie = loginAndReturnAuthCookie(student.getEmail(), "StrongPass123");

        String assignmentPayload = objectMapper.writeValueAsString(new CreateAssignmentPayload(
                "Cryptography Coursework",
                "Submit your signed coursework.",
                Instant.now().plusSeconds(86400)
        ));

        MvcResult assignmentResult = mockMvc.perform(post("/api/assignments")
                        .cookie(lecturerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignmentPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Cryptography Coursework"))
                .andReturn();

        JsonNode assignmentJson = objectMapper.readTree(assignmentResult.getResponse().getContentAsString());
        String assignmentId = textField(assignmentJson, "id");

        String submittedContent = "This is my authentic coursework submission content.";
        String submissionPayload = objectMapper.writeValueAsString(new CreateSubmissionPayload(
                "coursework.txt",
                "text/plain",
                submittedContent
        ));

        MvcResult submissionResult = mockMvc.perform(post("/api/assignments/{assignmentId}/submissions", assignmentId)
                        .cookie(studentCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submissionPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assignmentId").value(assignmentId))
                .andExpect(jsonPath("$.studentUserId").value(student.getId().toString()))
                .andExpect(jsonPath("$.storedFileReference").doesNotExist())
                .andExpect(jsonPath("$.hashDigest").isNotEmpty())
                .andExpect(jsonPath("$.digitalSignature").isNotEmpty())
                .andExpect(jsonPath("$.signatureAlgorithm").value("SHA256withRSA"))
                .andExpect(jsonPath("$.verificationStatus").value("VERIFIED"))
                .andReturn();

        JsonNode submissionJson = objectMapper.readTree(submissionResult.getResponse().getContentAsString());
        String submissionId = textField(submissionJson, "id");

        Submission savedSubmission = submissionRepository.findById(UUID.fromString(submissionId)).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(savedSubmission.getStorageEncryptionAlgorithm()).isEqualTo("AES/GCM/NoPadding");
        org.assertj.core.api.Assertions.assertThat(savedSubmission.getStorageEncryptionNonce()).isNotBlank();
        org.assertj.core.api.Assertions.assertThat(savedSubmission.getWrappedContentEncryptionKey()).isNotBlank();
        org.assertj.core.api.Assertions.assertThat(savedSubmission.getKeyWrapAlgorithm()).isEqualTo("AESWrap");
        org.assertj.core.api.Assertions.assertThat(savedSubmission.getStorageKeyVersion()).isEqualTo("test-v1");
        org.assertj.core.api.Assertions.assertThat(savedSubmission.getCiphertextLengthBytes()).isPositive();

        byte[] storedCiphertext = submissionContentStore.read(savedSubmission.getStoredFileReference());
        org.assertj.core.api.Assertions.assertThat(storedCiphertext).isNotEmpty();
        org.assertj.core.api.Assertions.assertThat(Arrays.equals(storedCiphertext, submittedContent.getBytes(StandardCharsets.UTF_8))).isFalse();

        mockMvc.perform(get("/api/submissions/{submissionId}", submissionId)
                        .cookie(lecturerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(submissionId))
                .andExpect(jsonPath("$.storedFileReference").doesNotExist())
                .andExpect(jsonPath("$.verificationStatus").value("VERIFIED"));

        mockMvc.perform(get("/api/submissions/{submissionId}/content", submissionId)
                        .cookie(studentCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.submissionId").value(submissionId))
                .andExpect(jsonPath("$.content").value(submittedContent));

        mockMvc.perform(get("/api/submissions/{submissionId}/content", submissionId)
                        .cookie(lecturerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.submissionId").value(submissionId))
                .andExpect(jsonPath("$.fileName").value("coursework.txt"))
                .andExpect(jsonPath("$.content").value(submittedContent));

        List<AuditLog> auditLogs = auditLogRepository.findByEntityTypeAndEntityIdOrderByEventTimestampAsc("Submission", UUID.fromString(submissionId));
        org.assertj.core.api.Assertions.assertThat(auditLogs)
                .hasSize(4)
                .extracting(log -> log.getActionType().name())
                .containsExactly("SUBMISSION_CREATED", "SUBMISSION_VERIFIED", "SUBMISSION_CONTENT_ACCESSED", "SUBMISSION_CONTENT_ACCESSED");
        org.assertj.core.api.Assertions.assertThat(auditLogs)
                .allSatisfy(log -> org.assertj.core.api.Assertions.assertThat(log.getIntegrityValue()).isNotBlank());
    }

    @Test
    void studentCannotReadAnotherStudentsSubmission() throws Exception {
        User lecturer = ensureUser("lecturer-guard-" + UUID.randomUUID() + "@example.com", "Lecturer Guard", RoleName.LECTURER);
        User owner = ensureUser("student-owner-" + UUID.randomUUID() + "@example.com", "Student Owner", RoleName.STUDENT);
        User otherStudent = ensureUser("student-other-" + UUID.randomUUID() + "@example.com", "Student Other", RoleName.STUDENT);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail(), "StrongPass123");
        Cookie ownerCookie = loginAndReturnAuthCookie(owner.getEmail(), "StrongPass123");
        Cookie otherStudentCookie = loginAndReturnAuthCookie(otherStudent.getEmail(), "StrongPass123");

        String assignmentPayload = objectMapper.writeValueAsString(new CreateAssignmentPayload(
                "Guarded Coursework",
                "A submission should not be visible to another student.",
                Instant.now().plusSeconds(86400)
        ));

        MvcResult assignmentResult = mockMvc.perform(post("/api/assignments")
                        .cookie(lecturerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignmentPayload))
                .andExpect(status().isCreated())
                .andReturn();

        String assignmentId = textField(objectMapper.readTree(assignmentResult.getResponse().getContentAsString()), "id");

        String submissionPayload = objectMapper.writeValueAsString(new CreateSubmissionPayload(
                "guarded.txt",
                "text/plain",
                "Owner-only submission content"
        ));

        MvcResult submissionResult = mockMvc.perform(post("/api/assignments/{assignmentId}/submissions", assignmentId)
                        .cookie(ownerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submissionPayload))
                .andExpect(status().isCreated())
                .andReturn();

        String submissionId = textField(objectMapper.readTree(submissionResult.getResponse().getContentAsString()), "id");

        mockMvc.perform(get("/api/submissions/{submissionId}", submissionId)
                        .cookie(otherStudentCookie))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/submissions/{submissionId}/content", submissionId)
                        .cookie(otherStudentCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentCannotCreateAssignment() throws Exception {
        User student = ensureUser("student-noassign-" + UUID.randomUUID() + "@example.com", "Student No Assign", RoleName.STUDENT);
        Cookie studentCookie = loginAndReturnAuthCookie(student.getEmail(), "StrongPass123");

        String assignmentPayload = objectMapper.writeValueAsString(new CreateAssignmentPayload(
                "Forbidden Assignment",
                "Students should not create assignments.",
                Instant.now().plusSeconds(86400)
        ));

        mockMvc.perform(post("/api/assignments")
                        .cookie(studentCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignmentPayload))
                .andExpect(status().isForbidden());
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

    private record CreateAssignmentPayload(String title, String description, Instant dueAt) {
    }

    private record CreateSubmissionPayload(String fileName, String contentType, String content) {
    }

    private static String textField(JsonNode objectNode, String fieldName) {
        return objectNode.required(fieldName).asString();
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

