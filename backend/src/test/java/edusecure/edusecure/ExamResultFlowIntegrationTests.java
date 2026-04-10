package edusecure.edusecure;

import edusecure.edusecure.entity.audit.AuditLog;
import edusecure.edusecure.entity.auth.Role;
import edusecure.edusecure.entity.auth.RoleName;
import edusecure.edusecure.entity.auth.User;
import edusecure.edusecure.repository.audit.AuditLogRepository;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ExamResultFlowIntegrationTests {

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

    @Test
    void lecturerCanCreateAndUpdateExamResultAndStudentSeesPublishedOwnResult() throws Exception {
        User lecturer = ensureUser("lecturer-exam-result-" + UUID.randomUUID() + "@example.com", "Lecturer Exam Result", RoleName.LECTURER);
        User student = ensureUser("student-exam-result-" + UUID.randomUUID() + "@example.com", "Student Exam Result", RoleName.STUDENT);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail(), "StrongPass123");
        Cookie studentCookie = loginAndReturnAuthCookie(student.getEmail(), "StrongPass123");

        String spaceId = createSpace(lecturerCookie, "Cryptography Exam Space", "CRYPTO-RESULT", "Space for exam-result integration tests.");
        addStudentToSpace(lecturerCookie, spaceId, student.getEmail());
        String examId = createExam(lecturerCookie, spaceId, "Cryptography Final", true);

        MvcResult createResult = mockMvc.perform(post("/api/exams/{examId}/results", examId)
                        .cookie(lecturerCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateExamResultPayload(
                                student.getEmail(),
                                74,
                                "Strong reasoning with minor precision issues.",
                                false
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.studentUserId").value(student.getId().toString()))
                .andExpect(jsonPath("$.published").value(false))
                .andReturn();

        String examResultId = textField(objectMapper.readTree(createResult.getResponse().getContentAsString()), "id");

        mockMvc.perform(get("/api/my/exam-results")
                        .cookie(studentCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(put("/api/exam-results/{examResultId}", examResultId)
                        .cookie(lecturerCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateExamResultPayload(
                                79,
                                "Published after review.",
                                true
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value(79))
                .andExpect(jsonPath("$.published").value(true));

        mockMvc.perform(get("/api/my/exam-results")
                        .cookie(studentCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].examId").value(examId))
                .andExpect(jsonPath("$[0].value").value(79));

        mockMvc.perform(get("/api/my/exams/{examId}/result", examId)
                        .cookie(studentCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spaceCode").value("CRYPTO-RESULT"));

        List<AuditLog> auditLogs = auditLogRepository.findByEntityTypeAndEntityIdOrderByEventTimestampAsc("ExamResult", UUID.fromString(examResultId));
        assertThat(auditLogs)
                .extracting(log -> log.getActionType().name())
                .contains("EXAM_RESULT_CREATED", "EXAM_RESULT_UPDATED");
        assertThat(auditLogs).allSatisfy(log -> {
            assertThat(log.getIntegrityValue()).isNotBlank();
            assertThat(log.getDetailsJson())
                    .contains("published=")
                    .doesNotContain("Published after review.");
        });
    }

    @Test
    void duplicateResultAndInvalidStudentTargetAreRejected() throws Exception {
        User lecturer = ensureUser("lecturer-exam-result-dup-" + UUID.randomUUID() + "@example.com", "Lecturer Exam Result Dup", RoleName.LECTURER);
        User student = ensureUser("student-exam-result-dup-" + UUID.randomUUID() + "@example.com", "Student Exam Result Dup", RoleName.STUDENT);
        User otherLecturer = ensureUser("lecturer-target-" + UUID.randomUUID() + "@example.com", "Lecturer Invalid Target", RoleName.LECTURER);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail(), "StrongPass123");
        String spaceId = createSpace(lecturerCookie, "Network Defence Results", "NET-RESULT", "Space for duplicate exam-result checks.");
        addStudentToSpace(lecturerCookie, spaceId, student.getEmail());
        String examId = createExam(lecturerCookie, spaceId, "Network Defence Final", true);

        String payload = objectMapper.writeValueAsString(new CreateExamResultPayload(student.getEmail(), 68, "Initial result.", false));
        mockMvc.perform(post("/api/exams/{examId}/results", examId)
                        .cookie(lecturerCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/exams/{examId}/results", examId)
                        .cookie(lecturerCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/exams/{examId}/results", examId)
                        .cookie(lecturerCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateExamResultPayload(
                                otherLecturer.getEmail(),
                                51,
                                "Invalid target.",
                                false
                        ))))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void studentCannotSeeUnpublishedResultAndLosesAccessAfterMembershipRemoval() throws Exception {
        User lecturer = ensureUser("lecturer-exam-result-guard-" + UUID.randomUUID() + "@example.com", "Lecturer Exam Result Guard", RoleName.LECTURER);
        User student = ensureUser("student-exam-result-guard-" + UUID.randomUUID() + "@example.com", "Student Exam Result Guard", RoleName.STUDENT);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail(), "StrongPass123");
        Cookie studentCookie = loginAndReturnAuthCookie(student.getEmail(), "StrongPass123");

        String spaceId = createSpace(lecturerCookie, "Forensics Results", "FORENSICS-RESULT", "Space for result visibility rules.");
        addStudentToSpace(lecturerCookie, spaceId, student.getEmail());
        String examId = createExam(lecturerCookie, spaceId, "Forensics Final", true);

        MvcResult createResult = mockMvc.perform(post("/api/exams/{examId}/results", examId)
                        .cookie(lecturerCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateExamResultPayload(
                                student.getEmail(),
                                83,
                                "Not yet published.",
                                false
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        String examResultId = textField(objectMapper.readTree(createResult.getResponse().getContentAsString()), "id");

        mockMvc.perform(get("/api/my/exams/{examId}/result", examId)
                        .cookie(studentCookie))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/exam-results/{examResultId}", examResultId)
                        .cookie(lecturerCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateExamResultPayload(
                                83,
                                "Now published.",
                                true
                        ))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/my/exams/{examId}/result", examId)
                        .cookie(studentCookie))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/spaces/{spaceId}/students/{studentUserId}", spaceId, student.getId())
                        .cookie(lecturerCookie)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/my/exams/{examId}/result", examId)
                        .cookie(studentCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void unrelatedLecturerCannotManageAnotherLecturersExamResults() throws Exception {
        User owner = ensureUser("lecturer-exam-result-owner-" + UUID.randomUUID() + "@example.com", "Lecturer Owner Result", RoleName.LECTURER);
        User otherLecturer = ensureUser("lecturer-exam-result-other-" + UUID.randomUUID() + "@example.com", "Lecturer Other Result", RoleName.LECTURER);
        User student = ensureUser("student-exam-result-other-" + UUID.randomUUID() + "@example.com", "Student Other Result", RoleName.STUDENT);
        User admin = ensureUser("admin-exam-result-" + UUID.randomUUID() + "@example.com", "Admin Result", RoleName.ADMIN);

        Cookie ownerCookie = loginAndReturnAuthCookie(owner.getEmail(), "StrongPass123");
        Cookie otherLecturerCookie = loginAndReturnAuthCookie(otherLecturer.getEmail(), "StrongPass123");
        Cookie adminCookie = loginAndReturnAuthCookie(admin.getEmail(), "StrongPass123");

        String spaceId = createSpace(ownerCookie, "Secure Systems Results", "SYS-RESULT", "Space for cross-lecturer authorization checks.");
        addStudentToSpace(ownerCookie, spaceId, student.getEmail());
        String examId = createExam(ownerCookie, spaceId, "Secure Systems Final", true);

        mockMvc.perform(post("/api/exams/{examId}/results", examId)
                        .cookie(otherLecturerCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateExamResultPayload(
                                student.getEmail(),
                                66,
                                "Unauthorized.",
                                false
                        ))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/exams/{examId}/results", examId)
                        .cookie(adminCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateExamResultPayload(
                                student.getEmail(),
                                91,
                                "Admin oversight entry.",
                                true
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.value").value(91));
    }

    private String createSpace(Cookie lecturerCookie, String name, String code, String description) throws Exception {
        MvcResult createSpaceResult = mockMvc.perform(post("/api/spaces")
                        .cookie(lecturerCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateSpacePayload(name, code, description))))
                .andExpect(status().isCreated())
                .andReturn();

        return textField(objectMapper.readTree(createSpaceResult.getResponse().getContentAsString()), "id");
    }

    private void addStudentToSpace(Cookie lecturerCookie, String spaceId, String studentEmail) throws Exception {
        mockMvc.perform(post("/api/spaces/{spaceId}/students", spaceId)
                        .cookie(lecturerCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddStudentPayload(studentEmail))))
                .andExpect(status().isCreated());
    }

    private String createExam(Cookie lecturerCookie, String spaceId, String title, boolean published) throws Exception {
        Instant startsAt = Instant.now().plus(21, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        Instant endsAt = startsAt.plus(2, ChronoUnit.HOURS);
        MvcResult result = mockMvc.perform(post("/api/exams")
                        .cookie(lecturerCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateExamPayload(
                                UUID.fromString(spaceId),
                                title,
                                title + " description",
                                "Room R1",
                                startsAt,
                                endsAt,
                                published
                        ))))
                .andExpect(status().isCreated())
                .andReturn();

        return textField(objectMapper.readTree(result.getResponse().getContentAsString()), "id");
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

    private record CreateExamPayload(
            UUID spaceId,
            String title,
            String description,
            String location,
            Instant startsAt,
            Instant endsAt,
            boolean published
    ) {
    }

    private record CreateExamResultPayload(String studentEmail, Integer value, String feedback, boolean published) {
    }

    private record UpdateExamResultPayload(Integer value, String feedback, boolean published) {
    }
}