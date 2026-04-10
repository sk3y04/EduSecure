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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ExamFlowIntegrationTests {

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
    void lecturerCanCreateAndUpdateExamForOwnedSpace() throws Exception {
        User lecturer = ensureUser("lecturer-exam-" + UUID.randomUUID() + "@example.com", "Lecturer Exam", RoleName.LECTURER);
        User student = ensureUser("student-exam-" + UUID.randomUUID() + "@example.com", "Student Exam", RoleName.STUDENT);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail(), "StrongPass123");
        Cookie studentCookie = loginAndReturnAuthCookie(student.getEmail(), "StrongPass123");

        String spaceId = createSpace(lecturerCookie, "Applied Cryptography Group A", "CRYPTO-EXAM", "Exam scheduling space for cryptography teaching and assessment.");
        addStudentToSpace(lecturerCookie, spaceId, student.getEmail());

        Instant startsAt = Instant.now().plus(20, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        Instant endsAt = startsAt.plus(2, ChronoUnit.HOURS);

        MvcResult createResult = mockMvc.perform(post("/api/exams")
                        .cookie(lecturerCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateExamPayload(
                                UUID.fromString(spaceId),
                                "Applied Cryptography Final Exam",
                                "Closed-book final assessment.",
                                "Room B201",
                                startsAt,
                                endsAt,
                                true
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.spaceId").value(spaceId))
                .andExpect(jsonPath("$.spaceCode").value("CRYPTO-EXAM"))
                .andExpect(jsonPath("$.published").value(true))
                .andExpect(jsonPath("$.canManage").value(true))
                .andReturn();

        String examId = textField(objectMapper.readTree(createResult.getResponse().getContentAsString()), "id");

        mockMvc.perform(get("/api/exams")
                        .cookie(studentCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(examId))
                .andExpect(jsonPath("$[0].canManage").value(false));

        Instant updatedStartsAt = startsAt.plus(1, ChronoUnit.DAYS);
        Instant updatedEndsAt = updatedStartsAt.plus(3, ChronoUnit.HOURS);
        mockMvc.perform(put("/api/exams/{examId}", examId)
                        .cookie(lecturerCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateExamPayload(
                                UUID.fromString(spaceId),
                                "Applied Cryptography Final Exam",
                                "Updated closed-book final assessment.",
                                "Room C104",
                                updatedStartsAt,
                                updatedEndsAt,
                                false
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.location").value("Room C104"))
                .andExpect(jsonPath("$.published").value(false));

        mockMvc.perform(get("/api/exams/{examId}", examId)
                        .cookie(studentCookie))
                .andExpect(status().isForbidden());

        List<AuditLog> auditLogs = auditLogRepository.findByEntityTypeAndEntityIdOrderByEventTimestampAsc("Exam", UUID.fromString(examId));
        assertThat(auditLogs)
                .extracting(log -> log.getActionType().name())
                .contains("EXAM_CREATED", "EXAM_UPDATED");
        assertThat(auditLogs).allSatisfy(log -> {
            assertThat(log.getIntegrityValue()).isNotBlank();
            assertThat(log.getDetailsJson())
                    .contains("spaceCode=CRYPTO-EXAM")
                    .doesNotContain("Updated closed-book final assessment.");
        });
    }

    @Test
    void studentSeesOnlyPublishedExamsInEnrolledSpaces() throws Exception {
        User lecturer = ensureUser("lecturer-exam-student-" + UUID.randomUUID() + "@example.com", "Lecturer Student Exams", RoleName.LECTURER);
        User student = ensureUser("student-exam-student-" + UUID.randomUUID() + "@example.com", "Student Visible Exams", RoleName.STUDENT);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail(), "StrongPass123");
        Cookie studentCookie = loginAndReturnAuthCookie(student.getEmail(), "StrongPass123");

        String memberSpaceId = createSpace(lecturerCookie, "Network Defence Lab", "NET-EXAM", "Exam-capable lab space for enrolled students.");
        String otherSpaceId = createSpace(lecturerCookie, "Forensics Lab", "FORENSICS-EXAM", "Separate lab space for non-member visibility checks.");
        addStudentToSpace(lecturerCookie, memberSpaceId, student.getEmail());

        Instant baseStart = Instant.now().plus(15, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        createExam(lecturerCookie, memberSpaceId, "Published member exam", "Room A1", baseStart, baseStart.plus(90, ChronoUnit.MINUTES), true);
        createExam(lecturerCookie, memberSpaceId, "Draft member exam", "Room A2", baseStart.plus(3, ChronoUnit.HOURS), baseStart.plus(5, ChronoUnit.HOURS), false);
        createExam(lecturerCookie, otherSpaceId, "Published other exam", "Room A3", baseStart.plus(1, ChronoUnit.DAYS), baseStart.plus(1, ChronoUnit.DAYS).plus(2, ChronoUnit.HOURS), true);

        mockMvc.perform(get("/api/exams")
                        .cookie(studentCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Published member exam"))
                .andExpect(jsonPath("$[0].spaceCode").value("NET-EXAM"))
                .andExpect(jsonPath("$[0].published").value(true));
    }

    @Test
    void lecturerCannotManageAnotherLecturersSpaceAndAdminCan() throws Exception {
        User owner = ensureUser("lecturer-exam-owner-" + UUID.randomUUID() + "@example.com", "Lecturer Owner Exams", RoleName.LECTURER);
        User otherLecturer = ensureUser("lecturer-exam-other-" + UUID.randomUUID() + "@example.com", "Lecturer Other Exams", RoleName.LECTURER);
        User admin = ensureUser("admin-exam-" + UUID.randomUUID() + "@example.com", "Admin Exams", RoleName.ADMIN);

        Cookie ownerCookie = loginAndReturnAuthCookie(owner.getEmail(), "StrongPass123");
        Cookie otherLecturerCookie = loginAndReturnAuthCookie(otherLecturer.getEmail(), "StrongPass123");
        Cookie adminCookie = loginAndReturnAuthCookie(admin.getEmail(), "StrongPass123");

        String spaceId = createSpace(ownerCookie, "Secure Systems Seminar", "SYS-EXAM", "Owned lecturer space for exam authorization tests.");
        Instant startsAt = Instant.now().plus(30, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        Instant endsAt = startsAt.plus(2, ChronoUnit.HOURS);

        mockMvc.perform(post("/api/exams")
                        .cookie(otherLecturerCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateExamPayload(
                                UUID.fromString(spaceId),
                                "Unauthorized Exam",
                                "This should be rejected.",
                                "Room D1",
                                startsAt,
                                endsAt,
                                true
                        ))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/exams")
                        .cookie(adminCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateExamPayload(
                                UUID.fromString(spaceId),
                                "Admin Created Exam",
                                "Admin may schedule for any space.",
                                "Room D2",
                                startsAt.plus(1, ChronoUnit.DAYS),
                                endsAt.plus(1, ChronoUnit.DAYS),
                                true
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Admin Created Exam"));
    }

    @Test
    void overlappingExamCreationAndArchivedSpaceChangesAreRejected() throws Exception {
        User lecturer = ensureUser("lecturer-exam-conflict-" + UUID.randomUUID() + "@example.com", "Lecturer Conflict Exams", RoleName.LECTURER);
        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail(), "StrongPass123");

        String spaceId = createSpace(lecturerCookie, "Digital Forensics Group", "FORENSICS-TIME", "Space used for timetable conflict and archived-space checks.");
        Instant startsAt = Instant.now().plus(18, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        Instant endsAt = startsAt.plus(2, ChronoUnit.HOURS);

        createExam(lecturerCookie, spaceId, "Forensics Final", "Room F1", startsAt, endsAt, true);

        mockMvc.perform(post("/api/exams")
                        .cookie(lecturerCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateExamPayload(
                                UUID.fromString(spaceId),
                                "Forensics Overlap",
                                "Overlap should fail.",
                                "Room F2",
                                startsAt.plus(30, ChronoUnit.MINUTES),
                                endsAt.plus(30, ChronoUnit.MINUTES),
                                true
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Exam schedule overlaps with another exam in this space"));

        mockMvc.perform(put("/api/spaces/{spaceId}", spaceId)
                        .cookie(lecturerCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateSpacePayload(
                                "Digital Forensics Group",
                                "FORENSICS-TIME",
                                "Space used for timetable conflict and archived-space checks.",
                                true
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(true));

        mockMvc.perform(post("/api/exams")
                        .cookie(lecturerCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateExamPayload(
                                UUID.fromString(spaceId),
                                "Archived Space Exam",
                                "Archived space should reject scheduling.",
                                "Room F3",
                                startsAt.plus(2, ChronoUnit.DAYS),
                                endsAt.plus(2, ChronoUnit.DAYS),
                                false
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Archived spaces cannot accept exam schedule changes"));
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

    private String createExam(
            Cookie actorCookie,
            String spaceId,
            String title,
            String location,
            Instant startsAt,
            Instant endsAt,
            boolean published
    ) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/exams")
                        .cookie(actorCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateExamPayload(
                                UUID.fromString(spaceId),
                                title,
                                title + " description",
                                location,
                                startsAt,
                                endsAt,
                                published
                        ))))
                .andExpect(status().isCreated())
                .andReturn();

        return textField(objectMapper.readTree(result.getResponse().getContentAsString()), "id");
    }

    private void addStudentToSpace(Cookie lecturerCookie, String spaceId, String studentEmail) throws Exception {
        mockMvc.perform(post("/api/spaces/{spaceId}/students", spaceId)
                        .cookie(lecturerCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddStudentPayload(studentEmail))))
                .andExpect(status().isCreated());
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

    private record UpdateSpacePayload(String name, String code, String description, boolean archived) {
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
}