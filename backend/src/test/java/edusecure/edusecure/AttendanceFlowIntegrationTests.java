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
class AttendanceFlowIntegrationTests {

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
    void lecturerCanCreateAttendanceSessionSnapshotRosterAndRecordStatuses() throws Exception {
        User lecturer = ensureUser("lecturer-attendance-" + UUID.randomUUID() + "@example.com", "Lecturer Attendance", RoleName.LECTURER);
        User studentOne = ensureUser("student-attendance-1-" + UUID.randomUUID() + "@example.com", "Student Attendance One", RoleName.STUDENT);
        User studentTwo = ensureUser("student-attendance-2-" + UUID.randomUUID() + "@example.com", "Student Attendance Two", RoleName.STUDENT);
        User studentThree = ensureUser("student-attendance-3-" + UUID.randomUUID() + "@example.com", "Student Attendance Three", RoleName.STUDENT);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail(), "StrongPass123");
        Cookie studentOneCookie = loginAndReturnAuthCookie(studentOne.getEmail(), "StrongPass123");

        String spaceId = createSpace(lecturerCookie, "Applied Security Attendance", "ATTEND-A", "Attendance-managed teaching space.");
        addStudentToSpace(lecturerCookie, spaceId, studentOne.getEmail());
        addStudentToSpace(lecturerCookie, spaceId, studentTwo.getEmail());

        Instant startsAt = Instant.now().plus(7, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        Instant endsAt = startsAt.plus(2, ChronoUnit.HOURS);

        MvcResult createResult = mockMvc.perform(post("/api/attendance-sessions")
                        .cookie(lecturerCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateAttendanceSessionPayload(
                                UUID.fromString(spaceId),
                                "Week 4 seminar",
                                "Applied security seminar attendance.",
                                startsAt,
                                endsAt
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.spaceId").value(spaceId))
                .andExpect(jsonPath("$.memberCount").value(2))
                .andExpect(jsonPath("$.recordedCount").value(0))
                .andExpect(jsonPath("$.canManage").value(true))
                .andReturn();

        String sessionId = textField(objectMapper.readTree(createResult.getResponse().getContentAsString()));

        addStudentToSpace(lecturerCookie, spaceId, studentThree.getEmail());

        mockMvc.perform(get("/api/attendance-sessions/{sessionId}/records", sessionId)
                        .cookie(lecturerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session.id").value(sessionId))
                .andExpect(jsonPath("$.session.memberCount").value(2))
                .andExpect(jsonPath("$.records.length()").value(2));

        mockMvc.perform(put("/api/attendance-sessions/{sessionId}/records", sessionId)
                        .cookie(lecturerCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateAttendanceRecordsPayload(List.of(
                                new AttendanceRecordPayload(studentOne.getId(), "PRESENT"),
                                new AttendanceRecordPayload(studentTwo.getId(), "LATE")
                        )))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session.recordedCount").value(2))
                .andExpect(jsonPath("$.session.presentCount").value(1))
                .andExpect(jsonPath("$.session.lateCount").value(1));

        mockMvc.perform(get("/api/attendance-sessions")
                        .cookie(studentOneCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(sessionId))
                .andExpect(jsonPath("$[0].myStatus").value("PRESENT"))
                .andExpect(jsonPath("$[0].memberCount").value(2))
                .andExpect(jsonPath("$[0].canManage").value(false));

        mockMvc.perform(get("/api/attendance-sessions/{sessionId}/records", sessionId)
                        .cookie(studentOneCookie))
                .andExpect(status().isForbidden());

        List<AuditLog> auditLogs = auditLogRepository.findByEntityTypeAndEntityIdOrderByEventTimestampAsc("AttendanceSession", UUID.fromString(sessionId));
        assertThat(auditLogs)
                .extracting(log -> log.getActionType().name())
                .contains("ATTENDANCE_SESSION_CREATED", "ATTENDANCE_RECORDS_UPDATED");
        assertThat(auditLogs).allSatisfy(log -> {
            assertThat(log.getIntegrityValue()).isNotBlank();
            assertThat(log.getDetailsJson())
                    .contains("spaceCode=ATTEND-A")
                    .doesNotContain("Applied security seminar attendance.");
        });
    }

    @Test
    void studentSeesOnlyAttendanceSessionsForEnrolledSpaces() throws Exception {
        User lecturer = ensureUser("lecturer-attendance-visible-" + UUID.randomUUID() + "@example.com", "Lecturer Attendance Visible", RoleName.LECTURER);
        User student = ensureUser("student-attendance-visible-" + UUID.randomUUID() + "@example.com", "Student Attendance Visible", RoleName.STUDENT);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail(), "StrongPass123");
        Cookie studentCookie = loginAndReturnAuthCookie(student.getEmail(), "StrongPass123");

        String memberSpaceId = createSpace(lecturerCookie, "Member Attendance Space", "ATTEND-M", "Member-visible attendance space.");
        String otherSpaceId = createSpace(lecturerCookie, "Other Attendance Space", "ATTEND-O", "Non-member attendance space.");
        addStudentToSpace(lecturerCookie, memberSpaceId, student.getEmail());

        Instant startsAt = Instant.now().plus(9, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        String memberSessionId = createAttendanceSession(lecturerCookie, memberSpaceId, "Member session", startsAt, startsAt.plus(1, ChronoUnit.HOURS));
        createAttendanceSession(lecturerCookie, otherSpaceId, "Other session", startsAt.plus(1, ChronoUnit.DAYS), startsAt.plus(1, ChronoUnit.DAYS).plus(1, ChronoUnit.HOURS));

        mockMvc.perform(get("/api/attendance-sessions")
                        .cookie(studentCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(memberSessionId))
                .andExpect(jsonPath("$[0].spaceCode").value("ATTEND-M"));
    }

    @Test
    void lecturerCannotManageAnotherLecturersAttendanceSessionButAdminCan() throws Exception {
        User owner = ensureUser("lecturer-attendance-owner-" + UUID.randomUUID() + "@example.com", "Lecturer Attendance Owner", RoleName.LECTURER);
        User otherLecturer = ensureUser("lecturer-attendance-other-" + UUID.randomUUID() + "@example.com", "Lecturer Attendance Other", RoleName.LECTURER);
        User admin = ensureUser("admin-attendance-" + UUID.randomUUID() + "@example.com", "Admin Attendance", RoleName.ADMIN);

        Cookie ownerCookie = loginAndReturnAuthCookie(owner.getEmail(), "StrongPass123");
        Cookie otherLecturerCookie = loginAndReturnAuthCookie(otherLecturer.getEmail(), "StrongPass123");
        Cookie adminCookie = loginAndReturnAuthCookie(admin.getEmail(), "StrongPass123");

        String spaceId = createSpace(ownerCookie, "Attendance Authorization Space", "ATTEND-AUTH", "Authorization-sensitive attendance space.");
        Instant startsAt = Instant.now().plus(11, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        Instant endsAt = startsAt.plus(90, ChronoUnit.MINUTES);
        String sessionId = createAttendanceSession(ownerCookie, spaceId, "Authorization session", startsAt, endsAt);

        mockMvc.perform(get("/api/attendance-sessions/{sessionId}/records", sessionId)
                        .cookie(otherLecturerCookie))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/attendance-sessions/{sessionId}", sessionId)
                        .cookie(otherLecturerCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateAttendanceSessionPayload(
                                "Unauthorized update",
                                "Should be rejected.",
                                startsAt.plus(1, ChronoUnit.HOURS),
                                endsAt.plus(1, ChronoUnit.HOURS)
                        ))))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/attendance-sessions/{sessionId}", sessionId)
                        .cookie(adminCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateAttendanceSessionPayload(
                                "Admin-reviewed session",
                                "Admin can update any managed space session.",
                                startsAt.plus(2, ChronoUnit.HOURS),
                                endsAt.plus(2, ChronoUnit.HOURS)
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Admin-reviewed session"));
    }

    @Test
    void archivedSpacesRejectAttendanceChangesAndDuplicateBatchEntriesAreRejected() throws Exception {
        User lecturer = ensureUser("lecturer-attendance-archive-" + UUID.randomUUID() + "@example.com", "Lecturer Attendance Archive", RoleName.LECTURER);
        User student = ensureUser("student-attendance-archive-" + UUID.randomUUID() + "@example.com", "Student Attendance Archive", RoleName.STUDENT);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail(), "StrongPass123");

        String spaceId = createSpace(lecturerCookie, "Attendance Archive Space", "ATTEND-ARCH", "Archived-space attendance checks.");
        addStudentToSpace(lecturerCookie, spaceId, student.getEmail());

        Instant startsAt = Instant.now().plus(13, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        Instant endsAt = startsAt.plus(1, ChronoUnit.HOURS);
        String sessionId = createAttendanceSession(lecturerCookie, spaceId, "Archive test session", startsAt, endsAt);

        mockMvc.perform(put("/api/attendance-sessions/{sessionId}/records", sessionId)
                        .cookie(lecturerCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateAttendanceRecordsPayload(List.of(
                                new AttendanceRecordPayload(student.getId(), "ABSENT"),
                                new AttendanceRecordPayload(student.getId(), "PRESENT")
                        )))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Each student may appear only once in an attendance update request"));

        archiveSpace(lecturerCookie, spaceId);

        mockMvc.perform(post("/api/attendance-sessions")
                        .cookie(lecturerCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateAttendanceSessionPayload(
                                UUID.fromString(spaceId),
                                "Blocked archive session",
                                "Archived spaces must reject new sessions.",
                                startsAt.plus(1, ChronoUnit.DAYS),
                                endsAt.plus(1, ChronoUnit.DAYS)
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Archived spaces cannot accept attendance changes"));

        mockMvc.perform(put("/api/attendance-sessions/{sessionId}/records", sessionId)
                        .cookie(lecturerCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateAttendanceRecordsPayload(List.of(
                                new AttendanceRecordPayload(student.getId(), "EXCUSED")
                        )))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Archived spaces cannot accept attendance changes"));
    }

    private String createSpace(Cookie lecturerCookie, String name, String code, String description) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/spaces")
                        .cookie(lecturerCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateSpacePayload(name, code, description))))
                .andExpect(status().isCreated())
                .andReturn();

        return textField(objectMapper.readTree(result.getResponse().getContentAsString()));
    }

    private void addStudentToSpace(Cookie lecturerCookie, String spaceId, String studentEmail) throws Exception {
        mockMvc.perform(post("/api/spaces/{spaceId}/students", spaceId)
                        .cookie(lecturerCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddStudentPayload(studentEmail))))
                .andExpect(status().isCreated());
    }

    private void archiveSpace(Cookie lecturerCookie, String spaceId) throws Exception {
        mockMvc.perform(put("/api/spaces/{spaceId}", spaceId)
                        .cookie(lecturerCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateSpacePayload(
                                "Attendance Archive Space",
                                "ATTEND-ARCH",
                                "Archived-space attendance checks.",
                                true
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(true));
    }

    private String createAttendanceSession(
            Cookie actorCookie,
            String spaceId,
            String title,
            Instant startsAt,
            Instant endsAt
    ) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/attendance-sessions")
                        .cookie(actorCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateAttendanceSessionPayload(
                                UUID.fromString(spaceId),
                                title,
                                title + " description",
                                startsAt,
                                endsAt
                        ))))
                .andExpect(status().isCreated())
                .andReturn();

        return textField(objectMapper.readTree(result.getResponse().getContentAsString()));
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

    private Cookie loginAndReturnAuthCookie(String email, String ignoredPassword) throws Exception {
        String loginPayload = objectMapper.writeValueAsString(new LoginPayload(email, "StrongPass123"));
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

    private static String textField(JsonNode objectNode) {
        return objectNode.required("id").asString();
    }

    private record LoginPayload(String email, String password) {
    }

    private record CreateSpacePayload(String name, String code, String description) {
    }

    private record UpdateSpacePayload(String name, String code, String description, boolean archived) {
    }

    private record AddStudentPayload(String studentEmail) {
    }

    private record CreateAttendanceSessionPayload(
            UUID spaceId,
            String title,
            String description,
            Instant startsAt,
            Instant endsAt
    ) {
    }

    private record UpdateAttendanceSessionPayload(
            String title,
            String description,
            Instant startsAt,
            Instant endsAt
    ) {
    }

    private record UpdateAttendanceRecordsPayload(List<AttendanceRecordPayload> records) {
    }

    private record AttendanceRecordPayload(UUID studentUserId, String status) {
    }
}



