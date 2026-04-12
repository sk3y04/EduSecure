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
 import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class SpaceRegistrationRequestIntegrationTests {

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
    void studentCanCreateAndCancelOwnRegistrationRequest() throws Exception {
        User lecturer = ensureUser("lecturer-reg-" + UUID.randomUUID() + "@example.com", "Lecturer Registration", RoleName.LECTURER);
        User student = ensureUser("student-reg-" + UUID.randomUUID() + "@example.com", "Student Registration", RoleName.STUDENT);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail(), "StrongPass123");
        Cookie studentCookie = loginAndReturnAuthCookie(student.getEmail(), "StrongPass123");

        String spaceId = createSpace(lecturerCookie, "Applied Cryptography Group A", "CRYPTO-A", "Shared space for lectures, secure resources, and submission coordination.");

        MvcResult createResult = mockMvc.perform(post("/api/space-registration-requests")
                        .cookie(studentCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateRegistrationPayload(
                                "crypto-a",
                                "Please add me to the applied cryptography cohort."
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.spaceId").value(spaceId))
                .andExpect(jsonPath("$.spaceCode").value("CRYPTO-A"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        String requestId = textField(objectMapper.readTree(createResult.getResponse().getContentAsString()), "id");

        mockMvc.perform(get("/api/space-registration-requests/mine")
                        .cookie(studentCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].spaceCode").value("CRYPTO-A"))
                .andExpect(jsonPath("$[0].status").value("PENDING"));

        mockMvc.perform(post("/api/space-registration-requests/{requestId}/cancel", requestId)
                        .cookie(studentCookie)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        List<AuditLog> auditLogs = auditLogRepository.findByEntityTypeAndEntityIdOrderByEventTimestampAsc(
                "SpaceRegistrationRequest",
                UUID.fromString(requestId)
        );
        assertThat(auditLogs)
                .extracting(log -> log.getActionType().name())
                .contains("SPACE_REGISTRATION_REQUEST_CREATED", "SPACE_REGISTRATION_REQUEST_CANCELLED");
        assertThat(auditLogs).allSatisfy(log -> {
            assertThat(log.getIntegrityValue()).isNotBlank();
            assertThat(log.getDetailsJson())
                    .doesNotContain("Please add me to the applied cryptography cohort.")
                    .doesNotContain(student.getEmail());
        });
    }

    @Test
    void duplicatePendingRequestAndExistingMembershipAreRejected() throws Exception {
        User lecturer = ensureUser("lecturer-reg-dup-" + UUID.randomUUID() + "@example.com", "Lecturer Registration Dup", RoleName.LECTURER);
        User student = ensureUser("student-reg-dup-" + UUID.randomUUID() + "@example.com", "Student Registration Dup", RoleName.STUDENT);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail(), "StrongPass123");
        Cookie studentCookie = loginAndReturnAuthCookie(student.getEmail(), "StrongPass123");

        String spaceId = createSpace(lecturerCookie, "Digital Security Practice", "DSP-1", "Practice space for secure systems labs and coursework discussion.");

        String payload = objectMapper.writeValueAsString(new CreateRegistrationPayload("DSP-1", "Please add me."));
        mockMvc.perform(post("/api/space-registration-requests")
                        .cookie(studentCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/space-registration-requests")
                        .cookie(studentCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/spaces/{spaceId}/students", spaceId)
                        .cookie(lecturerCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddStudentPayload(student.getEmail()))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/space-registration-requests")
                        .cookie(studentCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateRegistrationPayload("DSP-1", "Try again after direct add."))))
                .andExpect(status().isConflict());
    }

    @Test
    void lecturerCanReviewOwnedRequestsAndApprovalCreatesMembership() throws Exception {
        User lecturer = ensureUser("lecturer-reg-review-" + UUID.randomUUID() + "@example.com", "Lecturer Registration Review", RoleName.LECTURER);
        User student = ensureUser("student-reg-review-" + UUID.randomUUID() + "@example.com", "Student Registration Review", RoleName.STUDENT);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail(), "StrongPass123");
        Cookie studentCookie = loginAndReturnAuthCookie(student.getEmail(), "StrongPass123");

        String spaceId = createSpace(lecturerCookie, "Network Defence Lab", "NET-DEF", "Shared lab space for network defence exercises and reviews.");

        MvcResult createRequestResult = mockMvc.perform(post("/api/space-registration-requests")
                        .cookie(studentCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateRegistrationPayload(
                                "NET-DEF",
                                "Please enroll me in this lab space."
                        ))))
                .andExpect(status().isCreated())
                .andReturn();

        String requestId = textField(objectMapper.readTree(createRequestResult.getResponse().getContentAsString()), "id");

        mockMvc.perform(get("/api/space-registration-requests/review")
                        .cookie(lecturerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].studentUserId").value(student.getId().toString()))
                .andExpect(jsonPath("$[0].spaceId").value(spaceId))
                .andExpect(jsonPath("$[0].canReview").value(true));

        mockMvc.perform(post("/api/space-registration-requests/{requestId}/approve", requestId)
                        .cookie(lecturerCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReviewRegistrationPayload(
                                "Confirmed against the class list."
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.reviewNote").value("Confirmed against the class list."));

        mockMvc.perform(get("/api/spaces/{spaceId}", spaceId)
                        .cookie(studentCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isMember").value(true));

        mockMvc.perform(post("/api/space-registration-requests/{requestId}/approve", requestId)
                        .cookie(lecturerCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReviewRegistrationPayload(
                                "Repeat approval should fail."
                        ))))
                .andExpect(status().isConflict());

        List<AuditLog> auditLogs = auditLogRepository.findByEntityTypeAndEntityIdOrderByEventTimestampAsc(
                "SpaceRegistrationRequest",
                UUID.fromString(requestId)
        );
        assertThat(auditLogs)
                .extracting(log -> log.getActionType().name())
                .contains("SPACE_REGISTRATION_REQUEST_CREATED", "SPACE_REGISTRATION_REQUEST_APPROVED");
        assertThat(auditLogs).allSatisfy(log -> {
            assertThat(log.getDetailsJson())
                    .doesNotContain("Confirmed against the class list.")
                    .doesNotContain(student.getEmail());
        });
    }

    @Test
    void lecturerCannotReviewAnotherLecturersRequestButAdminCanReject() throws Exception {
        User owner = ensureUser("lecturer-reg-owner-" + UUID.randomUUID() + "@example.com", "Lecturer Registration Owner", RoleName.LECTURER);
        User otherLecturer = ensureUser("lecturer-reg-other-" + UUID.randomUUID() + "@example.com", "Lecturer Registration Other", RoleName.LECTURER);
        User admin = ensureUser("admin-reg-" + UUID.randomUUID() + "@example.com", "Admin Registration", RoleName.ADMIN);
        User student = ensureUser("student-reg-admin-" + UUID.randomUUID() + "@example.com", "Student Registration Admin", RoleName.STUDENT);

        Cookie ownerCookie = loginAndReturnAuthCookie(owner.getEmail(), "StrongPass123");
        Cookie otherLecturerCookie = loginAndReturnAuthCookie(otherLecturer.getEmail(), "StrongPass123");
        Cookie adminCookie = loginAndReturnAuthCookie(admin.getEmail(), "StrongPass123");
        Cookie studentCookie = loginAndReturnAuthCookie(student.getEmail(), "StrongPass123");

        createSpace(ownerCookie, "Secure Systems Seminar", "SYS-SEM", "Seminar space for secure systems case-study cohorts.");

        MvcResult createRequestResult = mockMvc.perform(post("/api/space-registration-requests")
                        .cookie(studentCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateRegistrationPayload(
                                "SYS-SEM",
                                "Please review my access request."
                        ))))
                .andExpect(status().isCreated())
                .andReturn();

        String requestId = textField(objectMapper.readTree(createRequestResult.getResponse().getContentAsString()), "id");

        mockMvc.perform(post("/api/space-registration-requests/{requestId}/reject", requestId)
                        .cookie(otherLecturerCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReviewRegistrationPayload(
                                "Unauthorized reviewer."
                        ))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/space-registration-requests/{requestId}/reject", requestId)
                        .cookie(adminCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReviewRegistrationPayload(
                                "This space is restricted to a different cohort."
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        mockMvc.perform(post("/api/space-registration-requests/{requestId}/cancel", requestId)
                        .cookie(studentCookie)
                        .with(csrf()))
                .andExpect(status().isConflict());
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

    private record CreateRegistrationPayload(String spaceCode, String requestMessage) {
    }

    private record ReviewRegistrationPayload(String reviewNote) {
    }
}