package edusecure.edusecure;

import edusecure.edusecure.entity.assignment.Assignment;
import edusecure.edusecure.entity.audit.AuditLog;
import edusecure.edusecure.entity.grade.Grade;
import edusecure.edusecure.entity.auth.Role;
import edusecure.edusecure.entity.auth.RoleName;
import edusecure.edusecure.entity.submission.Submission;
import edusecure.edusecure.entity.submission.SubmissionVerificationStatus;
import edusecure.edusecure.entity.auth.User;
import edusecure.edusecure.repository.assignment.AssignmentRepository;
import edusecure.edusecure.repository.audit.AuditLogRepository;
import edusecure.edusecure.repository.grade.GradeRepository;
import edusecure.edusecure.repository.auth.RoleRepository;
import edusecure.edusecure.repository.submission.SubmissionRepository;
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
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GradeFlowIntegrationTests {

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
    private AssignmentRepository assignmentRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private GradeRepository gradeRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    void lecturerCanCreateAndUpdateGradeAndStudentCanRetrieveOwnGrade() throws Exception {
        User lecturer = ensureUser("lecturer-grade-" + UUID.randomUUID() + "@example.com", "Lecturer Grade", RoleName.LECTURER);
        User student = ensureUser("student-grade-" + UUID.randomUUID() + "@example.com", "Student Grade", RoleName.STUDENT);
        Submission submission = ensureSubmissionForStudent(student, SubmissionVerificationStatus.VERIFIED);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail(), "StrongPass123");
        Cookie studentCookie = loginAndReturnAuthCookie(student.getEmail(), "StrongPass123");

        String createPayload = objectMapper.writeValueAsString(new CreateGradePayload("78", "Strong work overall."));
        MvcResult createResult = mockMvc.perform(post("/api/submissions/{submissionId}/grade", submission.getId())
                        .cookie(lecturerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.submissionId").value(submission.getId().toString()))
                .andExpect(jsonPath("$.value").value("78"))
                .andReturn();

        String gradeId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        String updatePayload = objectMapper.writeValueAsString(new UpdateGradePayload("81", "Updated after remarking."));
        mockMvc.perform(put("/api/grades/{gradeId}", gradeId)
                        .cookie(lecturerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value("81"))
                .andExpect(jsonPath("$.lastModifiedAt").isNotEmpty());

        mockMvc.perform(get("/api/my/grades/{gradeId}", gradeId)
                        .cookie(studentCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(gradeId))
                .andExpect(jsonPath("$.value").value("81"));

        List<AuditLog> auditLogs = auditLogRepository.findByEntityTypeAndEntityIdOrderByEventTimestampAsc("Grade", UUID.fromString(gradeId));
        assertThat(auditLogs)
                .extracting(log -> log.getActionType().name())
                .containsExactly("GRADE_CREATED", "GRADE_UPDATED");
        assertThat(auditLogs).allSatisfy(log -> assertThat(log.getIntegrityValue()).isNotBlank());
    }

    @Test
    void studentCannotCreateOrUpdateGrade() throws Exception {
        User lecturer = ensureUser("lecturer-grade-guard-" + UUID.randomUUID() + "@example.com", "Lecturer Guard", RoleName.LECTURER);
        User student = ensureUser("student-grade-guard-" + UUID.randomUUID() + "@example.com", "Student Guard", RoleName.STUDENT);
        Submission submission = ensureSubmissionForStudent(student, SubmissionVerificationStatus.VERIFIED);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail(), "StrongPass123");
        Cookie studentCookie = loginAndReturnAuthCookie(student.getEmail(), "StrongPass123");

        String createPayload = objectMapper.writeValueAsString(new CreateGradePayload("70", "Initial feedback."));
        MvcResult createResult = mockMvc.perform(post("/api/submissions/{submissionId}/grade", submission.getId())
                        .cookie(lecturerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andReturn();
        String gradeId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/submissions/{submissionId}/grade", submission.getId())
                        .cookie(studentCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isForbidden());

        String updatePayload = objectMapper.writeValueAsString(new UpdateGradePayload("75", "Student should not update grades."));
        mockMvc.perform(put("/api/grades/{gradeId}", gradeId)
                        .cookie(studentCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isForbidden());
    }

    @Test
    void duplicateGradeCreationIsRejected() throws Exception {
        User lecturer = ensureUser("lecturer-grade-dup-" + UUID.randomUUID() + "@example.com", "Lecturer Dup", RoleName.LECTURER);
        User student = ensureUser("student-grade-dup-" + UUID.randomUUID() + "@example.com", "Student Dup", RoleName.STUDENT);
        Submission submission = ensureSubmissionForStudent(student, SubmissionVerificationStatus.VERIFIED);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail(), "StrongPass123");
        String createPayload = objectMapper.writeValueAsString(new CreateGradePayload("65", "First grade."));

        mockMvc.perform(post("/api/submissions/{submissionId}/grade", submission.getId())
                        .cookie(lecturerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/submissions/{submissionId}/grade", submission.getId())
                        .cookie(lecturerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isConflict());
    }

    @Test
    void nonVerifiedSubmissionCannotBeGraded() throws Exception {
        User lecturer = ensureUser("lecturer-grade-fail-" + UUID.randomUUID() + "@example.com", "Lecturer Fail", RoleName.LECTURER);
        User student = ensureUser("student-grade-fail-" + UUID.randomUUID() + "@example.com", "Student Fail", RoleName.STUDENT);
        Submission failedSubmission = ensureSubmissionForStudent(student, SubmissionVerificationStatus.FAILED_VERIFICATION);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail(), "StrongPass123");
        String createPayload = objectMapper.writeValueAsString(new CreateGradePayload("55", "Should fail because submission was not verified."));

        mockMvc.perform(post("/api/submissions/{submissionId}/grade", failedSubmission.getId())
                        .cookie(lecturerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void studentCannotReadAnotherStudentsGrade() throws Exception {
        User lecturer = ensureUser("lecturer-grade-owner-" + UUID.randomUUID() + "@example.com", "Lecturer Owner", RoleName.LECTURER);
        User owner = ensureUser("student-grade-owner-" + UUID.randomUUID() + "@example.com", "Student Owner Grade", RoleName.STUDENT);
        User otherStudent = ensureUser("student-grade-other-" + UUID.randomUUID() + "@example.com", "Student Other Grade", RoleName.STUDENT);
        Submission submission = ensureSubmissionForStudent(owner, SubmissionVerificationStatus.VERIFIED);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail(), "StrongPass123");
        Cookie otherStudentCookie = loginAndReturnAuthCookie(otherStudent.getEmail(), "StrongPass123");

        String createPayload = objectMapper.writeValueAsString(new CreateGradePayload("88", "Visible only to owner."));
        MvcResult createResult = mockMvc.perform(post("/api/submissions/{submissionId}/grade", submission.getId())
                        .cookie(lecturerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andReturn();
        String gradeId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/my/grades/{gradeId}", gradeId)
                        .cookie(otherStudentCookie))
                .andExpect(status().isForbidden());
    }

    private Submission ensureSubmissionForStudent(User student, SubmissionVerificationStatus verificationStatus) {
        Assignment assignment = assignmentRepository.save(Assignment.builder()
                .title("Assignment " + UUID.randomUUID())
                .description("Test assignment")
                .dueAt(Instant.now().plusSeconds(86400))
                .createdByLecturerId(student.getId())
                .open(true)
                .build());

        return submissionRepository.save(Submission.builder()
                .assignmentId(assignment.getId())
                .studentUserId(student.getId())
                .submittedAt(Instant.now())
                .fileName("submission.txt")
                .contentType("text/plain")
                .storedFileReference("submission://" + UUID.randomUUID())
                .storageEncryptionAlgorithm("AES/GCM/NoPadding")
                .storageEncryptionNonce("fixture-nonce")
                .wrappedContentEncryptionKey("fixture-wrapped-key")
                .keyWrapAlgorithm("AESWrap")
                .storageKeyVersion("test-v1")
                .ciphertextLengthBytes(128L)
                .hashDigest("digest")
                .digitalSignature("signature")
                .signatureAlgorithm("SHA256withRSA")
                .verificationStatus(verificationStatus)
                .verificationMessage(verificationStatus.name())
                .build());
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

    private record CreateGradePayload(String value, String feedback) {
    }

    private record UpdateGradePayload(String value, String feedback) {
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

