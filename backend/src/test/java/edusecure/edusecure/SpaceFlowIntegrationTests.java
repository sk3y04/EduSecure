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

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SpaceFlowIntegrationTests {

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
    void lecturerCanManageSpaceMembershipsAndStudentSeesAssignedSpace() throws Exception {
        User lecturer = ensureUser("lecturer-space-" + UUID.randomUUID() + "@example.com", "Lecturer Space", RoleName.LECTURER);
        User student = ensureUser("student-space-" + UUID.randomUUID() + "@example.com", "Student Space", RoleName.STUDENT);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail(), "StrongPass123");
        Cookie studentCookie = loginAndReturnAuthCookie(student.getEmail(), "StrongPass123");

        String createPayload = objectMapper.writeValueAsString(new CreateSpacePayload(
                "Applied Cryptography Group A",
                "crypto-a",
                "Shared space for lectures, secure resources, and submission coordination."
        ));

        MvcResult createResult = mockMvc.perform(post("/api/spaces")
                        .cookie(lecturerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("CRYPTO-A"))
                .andExpect(jsonPath("$.memberCount").value(0))
                .andReturn();

        String spaceId = textField(objectMapper.readTree(createResult.getResponse().getContentAsString()), "id");

        String addStudentPayload = objectMapper.writeValueAsString(new AddStudentPayload(student.getEmail()));
        mockMvc.perform(post("/api/spaces/{spaceId}/students", spaceId)
                        .cookie(lecturerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addStudentPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.studentEmail").value(student.getEmail()));

        String updatePayload = objectMapper.writeValueAsString(new UpdateSpacePayload(
                "Applied Cryptography Group A",
                "CRYPTO-A",
                "Updated guidance and resource summary for the cohort.",
                false
        ));
        mockMvc.perform(put("/api/spaces/{spaceId}", spaceId)
                        .cookie(lecturerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberCount").value(1))
                .andExpect(jsonPath("$.memberships[0].studentUserId").value(student.getId().toString()));

        mockMvc.perform(get("/api/spaces/{spaceId}", spaceId)
                        .cookie(studentCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberCount").value(1))
                .andExpect(jsonPath("$.canManage").value(false))
                .andExpect(jsonPath("$.isMember").value(true))
                .andExpect(jsonPath("$.memberships.length()").value(0));

        mockMvc.perform(get("/api/spaces")
                        .cookie(studentCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("CRYPTO-A"))
                .andExpect(jsonPath("$[0].canManage").value(false))
                .andExpect(jsonPath("$[0].isMember").value(true));

        mockMvc.perform(delete("/api/spaces/{spaceId}/students/{studentUserId}", spaceId, student.getId())
                        .cookie(lecturerCookie))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/spaces")
                        .cookie(studentCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        List<AuditLog> auditLogs = auditLogRepository.findByEntityTypeAndEntityIdOrderByEventTimestampAsc("Space", UUID.fromString(spaceId));
        assertThat(auditLogs)
                .extracting(log -> log.getActionType().name())
                .contains("SPACE_CREATED", "SPACE_STUDENT_ADDED", "SPACE_UPDATED", "SPACE_STUDENT_REMOVED");
        assertThat(auditLogs).allSatisfy(log -> assertThat(log.getIntegrityValue()).isNotBlank());
    }

    @Test
    void studentCannotManageSpaceEndpoints() throws Exception {
        User lecturer = ensureUser("lecturer-space-guard-" + UUID.randomUUID() + "@example.com", "Lecturer Guard", RoleName.LECTURER);
        User student = ensureUser("student-space-guard-" + UUID.randomUUID() + "@example.com", "Student Guard", RoleName.STUDENT);
        User otherStudent = ensureUser("student-space-target-" + UUID.randomUUID() + "@example.com", "Student Target", RoleName.STUDENT);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail(), "StrongPass123");
        Cookie studentCookie = loginAndReturnAuthCookie(student.getEmail(), "StrongPass123");

        String createPayload = objectMapper.writeValueAsString(new CreateSpacePayload(
                "Security Operations Lab",
                "SECOPS-LAB",
                "Shared operational practice area for privileged staff and student coordination."
        ));
        MvcResult createResult = mockMvc.perform(post("/api/spaces")
                        .cookie(lecturerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andReturn();
        String spaceId = textField(objectMapper.readTree(createResult.getResponse().getContentAsString()), "id");

        mockMvc.perform(post("/api/spaces")
                        .cookie(studentCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isForbidden());

        String updatePayload = objectMapper.writeValueAsString(new UpdateSpacePayload(
                "Security Operations Lab",
                "SECOPS-LAB",
                "Updated description for unauthorized test.",
                false
        ));
        mockMvc.perform(put("/api/spaces/{spaceId}", spaceId)
                        .cookie(studentCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isForbidden());

        String addStudentPayload = objectMapper.writeValueAsString(new AddStudentPayload(otherStudent.getEmail()));
        mockMvc.perform(post("/api/spaces/{spaceId}/students", spaceId)
                        .cookie(studentCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addStudentPayload))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/spaces/{spaceId}/students/{studentUserId}", spaceId, otherStudent.getId())
                        .cookie(studentCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void lecturerCannotManageAnotherLecturersSpaceButAdminCan() throws Exception {
        User owner = ensureUser("lecturer-space-owner-" + UUID.randomUUID() + "@example.com", "Lecturer Owner", RoleName.LECTURER);
        User otherLecturer = ensureUser("lecturer-space-other-" + UUID.randomUUID() + "@example.com", "Lecturer Other", RoleName.LECTURER);
        User admin = ensureUser("admin-space-" + UUID.randomUUID() + "@example.com", "Admin Space", RoleName.ADMIN);
        User student = ensureUser("student-space-admin-" + UUID.randomUUID() + "@example.com", "Student Admin", RoleName.STUDENT);

        Cookie ownerCookie = loginAndReturnAuthCookie(owner.getEmail(), "StrongPass123");
        Cookie otherLecturerCookie = loginAndReturnAuthCookie(otherLecturer.getEmail(), "StrongPass123");
        Cookie adminCookie = loginAndReturnAuthCookie(admin.getEmail(), "StrongPass123");

        String createPayload = objectMapper.writeValueAsString(new CreateSpacePayload(
                "Digital Forensics Group",
                "FORENSICS-1",
                "Evidence-oriented collaboration area for coursework and review activities."
        ));
        MvcResult createResult = mockMvc.perform(post("/api/spaces")
                        .cookie(ownerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andReturn();
        String spaceId = textField(objectMapper.readTree(createResult.getResponse().getContentAsString()), "id");

        String otherLecturerUpdate = objectMapper.writeValueAsString(new UpdateSpacePayload(
                "Digital Forensics Group",
                "FORENSICS-1",
                "Attempted unauthorized update.",
                false
        ));
        mockMvc.perform(put("/api/spaces/{spaceId}", spaceId)
                        .cookie(otherLecturerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(otherLecturerUpdate))
                .andExpect(status().isForbidden());

        String addStudentPayload = objectMapper.writeValueAsString(new AddStudentPayload(student.getEmail()));
        mockMvc.perform(post("/api/spaces/{spaceId}/students", spaceId)
                        .cookie(otherLecturerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addStudentPayload))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/spaces/{spaceId}/students", spaceId)
                        .cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addStudentPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.studentUserId").value(student.getId().toString()));
    }

    @Test
    void duplicateSpaceCodeAndDuplicateMembershipAreRejected() throws Exception {
        User lecturer = ensureUser("lecturer-space-dup-" + UUID.randomUUID() + "@example.com", "Lecturer Dup", RoleName.LECTURER);
        User student = ensureUser("student-space-dup-" + UUID.randomUUID() + "@example.com", "Student Dup", RoleName.STUDENT);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail(), "StrongPass123");

        String createPayload = objectMapper.writeValueAsString(new CreateSpacePayload(
                "Cloud Security Seminar",
                "cloud-sec",
                "Seminar space for cloud security collaboration and assessment support."
        ));
        MvcResult createResult = mockMvc.perform(post("/api/spaces")
                        .cookie(lecturerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andReturn();
        String spaceId = textField(objectMapper.readTree(createResult.getResponse().getContentAsString()), "id");

        String duplicateCodePayload = objectMapper.writeValueAsString(new CreateSpacePayload(
                "Cloud Security Seminar B",
                "CLOUD-SEC",
                "Another space attempting to reuse the same normalized code."
        ));
        mockMvc.perform(post("/api/spaces")
                        .cookie(lecturerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(duplicateCodePayload))
                .andExpect(status().isConflict());

        String addStudentPayload = objectMapper.writeValueAsString(new AddStudentPayload(student.getEmail()));
        mockMvc.perform(post("/api/spaces/{spaceId}/students", spaceId)
                        .cookie(lecturerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addStudentPayload))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/spaces/{spaceId}/students", spaceId)
                        .cookie(lecturerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addStudentPayload))
                .andExpect(status().isConflict());
    }

    @Test
    void nonStudentMembershipAndArchivedSpaceAdditionAreRejected() throws Exception {
        User lecturer = ensureUser("lecturer-space-archive-" + UUID.randomUUID() + "@example.com", "Lecturer Archive", RoleName.LECTURER);
        User otherLecturer = ensureUser("lecturer-space-member-" + UUID.randomUUID() + "@example.com", "Lecturer Member", RoleName.LECTURER);
        User student = ensureUser("student-space-archive-" + UUID.randomUUID() + "@example.com", "Student Archive", RoleName.STUDENT);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail(), "StrongPass123");

        String createPayload = objectMapper.writeValueAsString(new CreateSpacePayload(
                "Applied Security Studio",
                "SEC-STUDIO",
                "Studio space for collaborative academic security exercises and reviews."
        ));
        MvcResult createResult = mockMvc.perform(post("/api/spaces")
                        .cookie(lecturerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andReturn();
        String spaceId = textField(objectMapper.readTree(createResult.getResponse().getContentAsString()), "id");

        String addLecturerPayload = objectMapper.writeValueAsString(new AddStudentPayload(otherLecturer.getEmail()));
        mockMvc.perform(post("/api/spaces/{spaceId}/students", spaceId)
                        .cookie(lecturerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addLecturerPayload))
                .andExpect(status().isUnprocessableContent());

        String archivePayload = objectMapper.writeValueAsString(new UpdateSpacePayload(
                "Applied Security Studio",
                "SEC-STUDIO",
                "Studio space for collaborative academic security exercises and reviews.",
                true
        ));
        mockMvc.perform(put("/api/spaces/{spaceId}", spaceId)
                        .cookie(lecturerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(archivePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(true));

        String addStudentPayload = objectMapper.writeValueAsString(new AddStudentPayload(student.getEmail()));
        mockMvc.perform(post("/api/spaces/{spaceId}/students", spaceId)
                        .cookie(lecturerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addStudentPayload))
                .andExpect(status().isConflict());
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
}