package edusecure.edusecure;

import edusecure.edusecure.entity.audit.AuditLog;
import edusecure.edusecure.entity.assignment.Assignment;
import edusecure.edusecure.entity.submission.Submission;
import edusecure.edusecure.entity.auth.Role;
import edusecure.edusecure.entity.auth.RoleName;
import edusecure.edusecure.entity.auth.User;
import edusecure.edusecure.entity.space.SpaceMembership;
import edusecure.edusecure.repository.assignment.AssignmentRepository;
import edusecure.edusecure.repository.audit.AuditLogRepository;
import edusecure.edusecure.repository.auth.RoleRepository;
import edusecure.edusecure.repository.space.SpaceMembershipRepository;
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
import org.springframework.mock.web.MockMultipartFile;
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

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
    private AssignmentRepository assignmentRepository;

    @Autowired
    private SubmissionContentStore submissionContentStore;

    @Autowired
    private SpaceMembershipRepository spaceMembershipRepository;

    @Test
    void lecturerCreatesAssignmentAndStudentSubmitsVerifiedWork() throws Exception {
        User lecturer = ensureUser("lecturer-" + UUID.randomUUID() + "@example.com", "Lecturer Example", RoleName.LECTURER);
        User student = ensureUser("student-submission-" + UUID.randomUUID() + "@example.com", "Student Submitter", RoleName.STUDENT);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail());
        Cookie studentCookie = loginAndReturnAuthCookie(student.getEmail());
        String spaceId = createSpaceAndReturnId(lecturerCookie);
        addStudentToSpace(lecturerCookie, spaceId, student.getEmail());

        String assignmentPayload = objectMapper.writeValueAsString(new CreateAssignmentPayload(
                "Cryptography Coursework",
                "Submit your signed coursework.",
                Instant.now().plusSeconds(86400),
                UUID.fromString(spaceId)
        ));

        MvcResult assignmentResult = mockMvc.perform(post("/api/assignments")
                        .cookie(lecturerCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignmentPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Cryptography Coursework"))
                .andReturn();

        String assignmentId = submissionIdFrom(assignmentResult);

        String submittedContent = "This is my authentic coursework submission content.";
        MockMultipartFile submissionFile = new MockMultipartFile(
                "file",
                "coursework.txt",
                "text/plain",
                submittedContent.getBytes(StandardCharsets.UTF_8)
        );

        MvcResult submissionResult = mockMvc.perform(multipart("/api/assignments/{assignmentId}/submissions", assignmentId)
                        .cookie(studentCookie)
                        .with(csrf())
                        .file(submissionFile))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assignmentId").value(assignmentId))
                .andExpect(jsonPath("$.studentUserId").value(student.getId().toString()))
                .andExpect(jsonPath("$.storedFileReference").doesNotExist())
                .andExpect(jsonPath("$.storageEncryptionAlgorithm").doesNotExist())
                .andExpect(jsonPath("$.storageEncryptionNonce").doesNotExist())
                .andExpect(jsonPath("$.wrappedContentEncryptionKey").doesNotExist())
                .andExpect(jsonPath("$.keyWrapAlgorithm").doesNotExist())
                .andExpect(jsonPath("$.storageKeyVersion").doesNotExist())
                .andExpect(jsonPath("$.ciphertextLengthBytes").doesNotExist())
                .andExpect(jsonPath("$.hashDigest").isNotEmpty())
                .andExpect(jsonPath("$.digitalSignature").isNotEmpty())
                .andExpect(jsonPath("$.signatureAlgorithm").value("SHA256withECDSA"))
                .andExpect(jsonPath("$.verificationStatus").value("VERIFIED"))
                .andReturn();

        String submissionId = submissionIdFrom(submissionResult);

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
                .andExpect(jsonPath("$.storageEncryptionAlgorithm").doesNotExist())
                .andExpect(jsonPath("$.storageEncryptionNonce").doesNotExist())
                .andExpect(jsonPath("$.wrappedContentEncryptionKey").doesNotExist())
                .andExpect(jsonPath("$.keyWrapAlgorithm").doesNotExist())
                .andExpect(jsonPath("$.storageKeyVersion").doesNotExist())
                .andExpect(jsonPath("$.ciphertextLengthBytes").doesNotExist())
                .andExpect(jsonPath("$.verificationStatus").value("VERIFIED"));

        mockMvc.perform(get("/api/submissions/{submissionId}/content", submissionId)
                        .cookie(studentCookie))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("coursework.txt")))
                .andExpect(content().contentType(MediaType.TEXT_PLAIN))
                .andExpect(content().bytes(submittedContent.getBytes(StandardCharsets.UTF_8)));

        mockMvc.perform(get("/api/submissions/{submissionId}/content", submissionId)
                        .cookie(lecturerCookie))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("coursework.txt")))
                .andExpect(content().contentType(MediaType.TEXT_PLAIN))
                .andExpect(content().bytes(submittedContent.getBytes(StandardCharsets.UTF_8)));

        List<AuditLog> auditLogs = auditLogRepository.findByEntityTypeAndEntityIdOrderByEventTimestampAsc("Submission", UUID.fromString(submissionId));
        org.assertj.core.api.Assertions.assertThat(auditLogs)
                .hasSize(4)
                .extracting(log -> log.getActionType().name())
                .containsExactly("SUBMISSION_CREATED", "SUBMISSION_VERIFIED", "SUBMISSION_CONTENT_ACCESSED", "SUBMISSION_CONTENT_ACCESSED");
        org.assertj.core.api.Assertions.assertThat(auditLogs)
                .allSatisfy(log -> {
                    org.assertj.core.api.Assertions.assertThat(log.getIntegrityValue()).isNotBlank();
                    org.assertj.core.api.Assertions.assertThat(log.getDetailsJson())
                            .doesNotContain(submittedContent)
                            .doesNotContain(savedSubmission.getStoredFileReference())
                            .doesNotContain(savedSubmission.getStorageEncryptionNonce())
                            .doesNotContain(savedSubmission.getWrappedContentEncryptionKey());
                });
    }

    @Test
    void studentCanRetrieveLatestSubmissionForAssignment() throws Exception {
        User lecturer = ensureUser("lecturer-latest-" + UUID.randomUUID() + "@example.com", "Lecturer Latest", RoleName.LECTURER);
        User student = ensureUser("student-latest-" + UUID.randomUUID() + "@example.com", "Student Latest", RoleName.STUDENT);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail());
        Cookie studentCookie = loginAndReturnAuthCookie(student.getEmail());

        String assignmentId = createAssignmentAndReturnId(lecturerCookie, "Latest Submission Coursework", "Latest submission should be returned.", student);

        mockMvc.perform(multipart("/api/assignments/{assignmentId}/submissions", assignmentId)
                        .cookie(studentCookie)
                        .with(csrf())
                        .file(new MockMultipartFile(
                                "file",
                                "attempt-one.txt",
                                "text/plain",
                                "first attempt".getBytes(StandardCharsets.UTF_8)
                        )))
                .andExpect(status().isCreated());

        mockMvc.perform(multipart("/api/assignments/{assignmentId}/submissions", assignmentId)
                        .cookie(studentCookie)
                        .with(csrf())
                        .file(new MockMultipartFile(
                                "file",
                                "attempt-two.txt",
                                "text/plain",
                                "second attempt".getBytes(StandardCharsets.UTF_8)
                        )))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/assignments/{assignmentId}/submissions/me", assignmentId)
                        .cookie(studentCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignmentId").value(assignmentId))
                .andExpect(jsonPath("$.studentUserId").value(student.getId().toString()))
                .andExpect(jsonPath("$.fileName").value("attempt-two.txt"));
    }

    @Test
    void lecturerCanListSubmissionsForAssignment() throws Exception {
        User lecturer = ensureUser("lecturer-list-submissions-" + UUID.randomUUID() + "@example.com", "Lecturer List Submissions", RoleName.LECTURER);
        User student = ensureUser("student-list-submissions-" + UUID.randomUUID() + "@example.com", "Student List Submissions", RoleName.STUDENT);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail());
        Cookie studentCookie = loginAndReturnAuthCookie(student.getEmail());

        String assignmentId = createAssignmentAndReturnId(lecturerCookie, "Listing Coursework", "Lecturers should see assignment submissions.", student);

        MvcResult submissionResult = mockMvc.perform(multipart("/api/assignments/{assignmentId}/submissions", assignmentId)
                        .cookie(studentCookie)
                        .with(csrf())
                        .file(new MockMultipartFile(
                                "file",
                                "listing.txt",
                                "text/plain",
                                "submission for lecturer listing".getBytes(StandardCharsets.UTF_8)
                        )))
                .andExpect(status().isCreated())
                .andReturn();

        String submissionId = submissionIdFrom(submissionResult);

        mockMvc.perform(get("/api/assignments/{assignmentId}/submissions", assignmentId)
                        .cookie(lecturerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(submissionId))
                .andExpect(jsonPath("$[0].assignmentId").value(assignmentId))
                .andExpect(jsonPath("$[0].studentUserId").value(student.getId().toString()))
                .andExpect(jsonPath("$[0].graded").value(false))
                .andExpect(jsonPath("$[0].gradeId").doesNotExist());
    }

    @Test
    void studentCannotListSubmissionsForAssignment() throws Exception {
        User lecturer = ensureUser("lecturer-student-list-guard-" + UUID.randomUUID() + "@example.com", "Lecturer Student List Guard", RoleName.LECTURER);
        User student = ensureUser("student-list-forbidden-" + UUID.randomUUID() + "@example.com", "Student List Forbidden", RoleName.STUDENT);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail());
        Cookie studentCookie = loginAndReturnAuthCookie(student.getEmail());

        String assignmentId = createAssignmentAndReturnId(lecturerCookie, "Lecturer-only submission listing", "Students must not access lecturer submission listings.", student);

        uploadSubmissionAndReturnId(
                studentCookie,
                assignmentId,
                "listing-guard.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "student submission for lecturer-only listing".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(get("/api/assignments/{assignmentId}/submissions", assignmentId)
                        .cookie(studentCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void unrelatedLecturerCannotListSubmissionsForAnotherLecturersAssignment() throws Exception {
        User ownerLecturer = ensureUser("lecturer-list-owner-" + UUID.randomUUID() + "@example.com", "Lecturer List Owner", RoleName.LECTURER);
        User otherLecturer = ensureUser("lecturer-list-other-" + UUID.randomUUID() + "@example.com", "Lecturer List Other", RoleName.LECTURER);
        User student = ensureUser("student-list-guard-" + UUID.randomUUID() + "@example.com", "Student List Guard", RoleName.STUDENT);

        Cookie ownerLecturerCookie = loginAndReturnAuthCookie(ownerLecturer.getEmail());
        Cookie otherLecturerCookie = loginAndReturnAuthCookie(otherLecturer.getEmail());
        Cookie studentCookie = loginAndReturnAuthCookie(student.getEmail());

        String assignmentId = createAssignmentAndReturnId(ownerLecturerCookie, "Owned Submission Listing", "Only the owning lecturer should list submissions.", student);

        uploadSubmissionAndReturnId(
                studentCookie,
                assignmentId,
                "owner-only-listing.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "only assignment owner should list this submission".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(get("/api/assignments/{assignmentId}/submissions", assignmentId)
                        .cookie(otherLecturerCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentAssignmentListIncludesLatestSubmissionMetadata() throws Exception {
        User lecturer = ensureUser("lecturer-assignment-list-" + UUID.randomUUID() + "@example.com", "Lecturer Assignment List", RoleName.LECTURER);
        User student = ensureUser("student-assignment-list-" + UUID.randomUUID() + "@example.com", "Student Assignment List", RoleName.STUDENT);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail());
        Cookie studentCookie = loginAndReturnAuthCookie(student.getEmail());

        String assignmentId = createAssignmentAndReturnId(lecturerCookie, "Visible Submission Coursework", "Student should see latest submission metadata on assignment listing.", student);

        MvcResult submissionResult = mockMvc.perform(multipart("/api/assignments/{assignmentId}/submissions", assignmentId)
                        .cookie(studentCookie)
                        .with(csrf())
                        .file(new MockMultipartFile(
                                "file",
                                "visible.txt",
                                "text/plain",
                                "visible submission".getBytes(StandardCharsets.UTF_8)
                        )))
                .andExpect(status().isCreated())
                .andReturn();

        String submissionId = submissionIdFrom(submissionResult);

        mockMvc.perform(get("/api/assignments")
                        .cookie(studentCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + assignmentId + "')].latestSubmissionId").value(org.hamcrest.Matchers.hasItem(submissionId)))
                .andExpect(jsonPath("$[?(@.id=='" + assignmentId + "')].latestSubmittedAt").isNotEmpty());
    }

    @Test
    void studentGetsNotFoundWhenNoSubmissionExistsForAssignment() throws Exception {
        User lecturer = ensureUser("lecturer-nosub-" + UUID.randomUUID() + "@example.com", "Lecturer No Submission", RoleName.LECTURER);
        User student = ensureUser("student-nosub-" + UUID.randomUUID() + "@example.com", "Student No Submission", RoleName.STUDENT);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail());
        Cookie studentCookie = loginAndReturnAuthCookie(student.getEmail());

        String assignmentId = createAssignmentAndReturnId(lecturerCookie, "No Submission Coursework", "No submission exists yet.", student);

        mockMvc.perform(get("/api/assignments/{assignmentId}/submissions/me", assignmentId)
                        .cookie(studentCookie))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No submission found for this assignment"))
                .andExpect(jsonPath("$.errors._global[0]").value("No submission found for this assignment"));
    }

    @Test
    void studentSeesOnlyAssignmentsForEnrolledSpacesAndCannotUseHiddenAssignments() throws Exception {
        User lecturer = ensureUser("lecturer-assignment-scope-" + UUID.randomUUID() + "@example.com", "Lecturer Assignment Scope", RoleName.LECTURER);
        User student = ensureUser("student-assignment-scope-" + UUID.randomUUID() + "@example.com", "Student Assignment Scope", RoleName.STUDENT);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail());
        Cookie studentCookie = loginAndReturnAuthCookie(student.getEmail());

        String visibleAssignmentId = createAssignmentAndReturnId(lecturerCookie, "Visible Space Coursework", "Assignment should be visible inside the enrolled space.", student);
        String hiddenAssignmentId = createAssignmentAndReturnId(lecturerCookie, "Hidden Space Coursework", "Assignment should stay hidden outside the enrolled space.");

        mockMvc.perform(get("/api/assignments")
                        .cookie(studentCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + visibleAssignmentId + "')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.id=='" + hiddenAssignmentId + "')]").isEmpty());

        mockMvc.perform(multipart("/api/assignments/{assignmentId}/submissions", hiddenAssignmentId)
                        .cookie(studentCookie)
                        .with(csrf())
                        .file(new MockMultipartFile(
                                "file",
                                "hidden.txt",
                                MediaType.TEXT_PLAIN_VALUE,
                                "hidden assignment submission attempt".getBytes(StandardCharsets.UTF_8)
                        )))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You cannot access this assignment"));

        mockMvc.perform(get("/api/assignments/{assignmentId}/submissions/me", hiddenAssignmentId)
                        .cookie(studentCookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You cannot access this assignment"));
    }

    @Test
    void lecturerSeesOnlyOwnedAssignmentsWhileAdminSeesAllAssignments() throws Exception {
        User lecturer = ensureUser("lecturer-owned-list-" + UUID.randomUUID() + "@example.com", "Lecturer Owned List", RoleName.LECTURER);
        User otherLecturer = ensureUser("lecturer-other-list-" + UUID.randomUUID() + "@example.com", "Lecturer Other List", RoleName.LECTURER);
        User admin = ensureUser("admin-assignment-list-" + UUID.randomUUID() + "@example.com", "Admin Assignment List", RoleName.ADMIN);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail());
        Cookie otherLecturerCookie = loginAndReturnAuthCookie(otherLecturer.getEmail());
        Cookie adminCookie = loginAndReturnAuthCookie(admin.getEmail());

        String ownedAssignmentId = createAssignmentAndReturnId(lecturerCookie, "Owned Assignment", "Lecturer should only see their own assignment in listings.");
        String otherAssignmentId = createAssignmentAndReturnId(otherLecturerCookie, "Other Assignment", "Second lecturer assignment should stay hidden from the first lecturer.");

        mockMvc.perform(get("/api/assignments")
                        .cookie(lecturerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + ownedAssignmentId + "')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.id=='" + otherAssignmentId + "')]").isEmpty());

        mockMvc.perform(get("/api/assignments")
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + ownedAssignmentId + "')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.id=='" + otherAssignmentId + "')]").isNotEmpty());
    }

    @Test
    void lecturerCannotCreateAssignmentForAnotherLecturersSpaceButAdminCan() throws Exception {
        User ownerLecturer = ensureUser("lecturer-space-owner-assign-" + UUID.randomUUID() + "@example.com", "Lecturer Space Owner", RoleName.LECTURER);
        User otherLecturer = ensureUser("lecturer-space-other-assign-" + UUID.randomUUID() + "@example.com", "Lecturer Space Other", RoleName.LECTURER);
        User admin = ensureUser("admin-space-assign-" + UUID.randomUUID() + "@example.com", "Admin Space Assignment", RoleName.ADMIN);

        Cookie ownerLecturerCookie = loginAndReturnAuthCookie(ownerLecturer.getEmail());
        Cookie otherLecturerCookie = loginAndReturnAuthCookie(otherLecturer.getEmail());
        Cookie adminCookie = loginAndReturnAuthCookie(admin.getEmail());

        String spaceId = createSpaceAndReturnId(ownerLecturerCookie);

        String foreignSpaceAssignmentPayload = objectMapper.writeValueAsString(new CreateAssignmentPayload(
                "Foreign Space Coursework",
                "Only the space owner or an admin should be able to scope assignments to this space.",
                Instant.now().plusSeconds(86400),
                UUID.fromString(spaceId)
        ));

        mockMvc.perform(post("/api/assignments")
                        .cookie(otherLecturerCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(foreignSpaceAssignmentPayload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You are not allowed to create assignments for this space"));

        mockMvc.perform(post("/api/assignments")
                        .cookie(adminCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(foreignSpaceAssignmentPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.spaceId").value(spaceId));
    }

    @Test
    void studentCannotReadAnotherStudentsSubmission() throws Exception {
        User lecturer = ensureUser("lecturer-guard-" + UUID.randomUUID() + "@example.com", "Lecturer Guard", RoleName.LECTURER);
        User owner = ensureUser("student-owner-" + UUID.randomUUID() + "@example.com", "Student Owner", RoleName.STUDENT);
        User otherStudent = ensureUser("student-other-" + UUID.randomUUID() + "@example.com", "Student Other", RoleName.STUDENT);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail());
        Cookie ownerCookie = loginAndReturnAuthCookie(owner.getEmail());
        Cookie otherStudentCookie = loginAndReturnAuthCookie(otherStudent.getEmail());
        String spaceId = createSpaceAndReturnId(lecturerCookie);
        addStudentToSpace(lecturerCookie, spaceId, owner.getEmail());

        String assignmentPayload = objectMapper.writeValueAsString(new CreateAssignmentPayload(
                "Guarded Coursework",
                "A submission should not be visible to another student.",
                Instant.now().plusSeconds(86400),
                UUID.fromString(spaceId)
        ));

        MvcResult assignmentResult = mockMvc.perform(post("/api/assignments")
                        .cookie(lecturerCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignmentPayload))
                .andExpect(status().isCreated())
                .andReturn();

        String assignmentId = submissionIdFrom(assignmentResult);

        MockMultipartFile submissionFile = new MockMultipartFile(
                "file",
                "guarded.txt",
                "text/plain",
                "Owner-only submission content".getBytes(StandardCharsets.UTF_8)
        );

        MvcResult submissionResult = mockMvc.perform(multipart("/api/assignments/{assignmentId}/submissions", assignmentId)
                        .cookie(ownerCookie)
                        .with(csrf())
                        .file(submissionFile))
                .andExpect(status().isCreated())
                .andReturn();

        String submissionId = submissionIdFrom(submissionResult);

        mockMvc.perform(get("/api/submissions/{submissionId}", submissionId)
                        .cookie(otherStudentCookie))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/submissions/{submissionId}/content", submissionId)
                        .cookie(otherStudentCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedUserCannotReadSubmissionMetadataOrContent() throws Exception {
        User lecturer = ensureUser("lecturer-unauth-sub-" + UUID.randomUUID() + "@example.com", "Lecturer Unauth Submission", RoleName.LECTURER);
        User student = ensureUser("student-unauth-sub-" + UUID.randomUUID() + "@example.com", "Student Unauth Submission", RoleName.STUDENT);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail());
        Cookie studentCookie = loginAndReturnAuthCookie(student.getEmail());

        String assignmentId = createAssignmentAndReturnId(lecturerCookie, "Unauthenticated Submission Access", "Anonymous users must not read submissions.", student);
        String submissionId = uploadSubmissionAndReturnId(
                studentCookie,
                assignmentId,
                "unauth-check.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "protected submission content".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(get("/api/submissions/{submissionId}", submissionId))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/submissions/{submissionId}/content", submissionId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void studentCannotCreateAssignment() throws Exception {
        User student = ensureUser("student-noassign-" + UUID.randomUUID() + "@example.com", "Student No Assign", RoleName.STUDENT);
        Cookie studentCookie = loginAndReturnAuthCookie(student.getEmail());

        String assignmentPayload = objectMapper.writeValueAsString(new CreateAssignmentPayload(
                "Forbidden Assignment",
                "Students should not create assignments.",
                Instant.now().plusSeconds(86400),
                UUID.randomUUID()
        ));

        mockMvc.perform(post("/api/assignments")
                        .cookie(studentCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignmentPayload))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentCannotUploadSubmissionWithTraversalStyleFilename() throws Exception {
        User lecturer = ensureUser("lecturer-traversal-" + UUID.randomUUID() + "@example.com", "Lecturer Traversal", RoleName.LECTURER);
        User student = ensureUser("student-traversal-" + UUID.randomUUID() + "@example.com", "Student Traversal", RoleName.STUDENT);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail());
        Cookie studentCookie = loginAndReturnAuthCookie(student.getEmail());

        String assignmentId = createAssignmentAndReturnId(lecturerCookie, "Traversal Filename Coursework", "Traversal-style filenames must be rejected.", student);

        mockMvc.perform(multipart("/api/assignments/{assignmentId}/submissions", assignmentId)
                        .cookie(studentCookie)
                        .with(csrf())
                        .file(new MockMultipartFile(
                                "file",
                                "../secrets.txt",
                                MediaType.TEXT_PLAIN_VALUE,
                                "attempted traversal".getBytes(StandardCharsets.UTF_8)
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Submission file name is invalid"))
                .andExpect(jsonPath("$.errors._global[0]").value("Submission file name is invalid"));
    }

    @Test
    void studentCannotSubmitToClosedAssignment() throws Exception {
        User lecturer = ensureUser("lecturer-closed-" + UUID.randomUUID() + "@example.com", "Lecturer Closed Assignment", RoleName.LECTURER);
        User student = ensureUser("student-closed-" + UUID.randomUUID() + "@example.com", "Student Closed Assignment", RoleName.STUDENT);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail());
        Cookie studentCookie = loginAndReturnAuthCookie(student.getEmail());

        String assignmentId = createAssignmentAndReturnId(lecturerCookie, "Closed Coursework", "Closed assignments must reject new submissions.", student);
        Assignment assignment = assignmentRepository.findById(UUID.fromString(assignmentId)).orElseThrow();
        assignment.setOpen(false);
        assignmentRepository.save(assignment);

        mockMvc.perform(multipart("/api/assignments/{assignmentId}/submissions", assignmentId)
                        .cookie(studentCookie)
                        .with(csrf())
                        .file(new MockMultipartFile(
                                "file",
                                "closed.txt",
                                MediaType.TEXT_PLAIN_VALUE,
                                "late work".getBytes(StandardCharsets.UTF_8)
                        )))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Assignment is closed"))
                .andExpect(jsonPath("$.errors._global[0]").value("Assignment is closed"));
    }

    @Test
    void lecturerCannotReadAnotherLecturersStudentSubmission() throws Exception {
        User ownerLecturer = ensureUser("lect-owner-sub-" + UUID.randomUUID() + "@example.com", "Lecturer Owner Submission", RoleName.LECTURER);
        User otherLecturer = ensureUser("lect-other-sub-" + UUID.randomUUID() + "@example.com", "Lecturer Other Submission", RoleName.LECTURER);
        User student = ensureUser("stu-cross-lect-sub-" + UUID.randomUUID() + "@example.com", "Student Cross Lecturer Submission", RoleName.STUDENT);

        Cookie ownerLecturerCookie = loginAndReturnAuthCookie(ownerLecturer.getEmail());
        Cookie otherLecturerCookie = loginAndReturnAuthCookie(otherLecturer.getEmail());
        Cookie studentCookie = loginAndReturnAuthCookie(student.getEmail());

        String assignmentId = createAssignmentAndReturnId(ownerLecturerCookie, "Cross Lecturer Submission Policy", "Only the owning lecturer should retain access to student submissions.", student);
        String content = "submission visible to privileged lecturers under current policy";
        String submissionId = uploadSubmissionAndReturnId(
                studentCookie,
                assignmentId,
                "cross-lecturer.txt",
                MediaType.TEXT_PLAIN_VALUE,
                content.getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(get("/api/submissions/{submissionId}", submissionId)
                        .cookie(otherLecturerCookie))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/submissions/{submissionId}/content", submissionId)
                        .cookie(otherLecturerCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanReadAnotherLecturersStudentSubmission() throws Exception {
        User ownerLecturer = ensureUser("lect-owner-admin-sub-" + UUID.randomUUID() + "@example.com", "Lecturer Owner Admin Submission", RoleName.LECTURER);
        User admin = ensureUser("admin-cross-sub-" + UUID.randomUUID() + "@example.com", "Admin Cross Submission", RoleName.ADMIN);
        User student = ensureUser("stu-admin-sub-" + UUID.randomUUID() + "@example.com", "Student Admin Submission", RoleName.STUDENT);

        Cookie ownerLecturerCookie = loginAndReturnAuthCookie(ownerLecturer.getEmail());
        Cookie adminCookie = loginAndReturnAuthCookie(admin.getEmail());
        Cookie studentCookie = loginAndReturnAuthCookie(student.getEmail());

        String assignmentId = createAssignmentAndReturnId(ownerLecturerCookie, "Admin Submission Visibility", "Admins should retain global submission access.", student);
        String content = "submission visible to admins across lecturer ownership boundaries";
        String submissionId = uploadSubmissionAndReturnId(
                studentCookie,
                assignmentId,
                "admin-cross-lecturer.txt",
                MediaType.TEXT_PLAIN_VALUE,
                content.getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(get("/api/submissions/{submissionId}", submissionId)
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(submissionId))
                .andExpect(jsonPath("$.studentUserId").value(student.getId().toString()));

        mockMvc.perform(get("/api/submissions/{submissionId}/content", submissionId)
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.TEXT_PLAIN))
                .andExpect(content().bytes(content.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void studentCannotReadOwnSubmissionAfterLosingAssignmentSpaceMembership() throws Exception {
        User lecturer = ensureUser("lecturer-sub-space-loss-" + UUID.randomUUID() + "@example.com", "Lecturer Submission Space Loss", RoleName.LECTURER);
        User student = ensureUser("student-sub-space-loss-" + UUID.randomUUID() + "@example.com", "Student Submission Space Loss", RoleName.STUDENT);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail());
        Cookie studentCookie = loginAndReturnAuthCookie(student.getEmail());

        String assignmentId = createAssignmentAndReturnId(lecturerCookie, "Submission Space Revocation", "Losing space membership should revoke student submission reads.", student);
        String submissionId = uploadSubmissionAndReturnId(
                studentCookie,
                assignmentId,
                "space-loss.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "submission visible only while student remains in the assignment space".getBytes(StandardCharsets.UTF_8)
        );

        Assignment assignment = assignmentRepository.findById(UUID.fromString(assignmentId)).orElseThrow();
        removeStudentFromAssignmentSpace(assignment.getSpaceId(), student.getId());

        mockMvc.perform(get("/api/submissions/{submissionId}", submissionId)
                        .cookie(studentCookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You cannot view this submission"));

        mockMvc.perform(get("/api/submissions/{submissionId}/content", submissionId)
                        .cookie(studentCookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You cannot view this submission"));
    }

    @Test
    void studentCanUploadAndDownloadPdfSubmission() throws Exception {
        User lecturer = ensureUser("lecturer-pdf-" + UUID.randomUUID() + "@example.com", "Lecturer PDF", RoleName.LECTURER);
        User student = ensureUser("student-pdf-" + UUID.randomUUID() + "@example.com", "Student PDF", RoleName.STUDENT);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail());
        Cookie studentCookie = loginAndReturnAuthCookie(student.getEmail());

        String assignmentId = createAssignmentAndReturnId(lecturerCookie, "PDF Coursework", "PDF uploads should be accepted.", student);

        byte[] pdfBytes = "%PDF-1.4\n1 0 obj\n<< /Type /Catalog >>\nendobj\ntrailer\n<<>>\n%%EOF".getBytes(StandardCharsets.US_ASCII);

        MvcResult submissionResult = mockMvc.perform(multipart("/api/assignments/{assignmentId}/submissions", assignmentId)
                        .cookie(studentCookie)
                        .with(csrf())
                        .file(new MockMultipartFile(
                                "file",
                                "coursework.pdf",
                                "application/pdf",
                                pdfBytes
                        )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileName").value("coursework.pdf"))
                .andExpect(jsonPath("$.contentType").value(MediaType.APPLICATION_PDF_VALUE))
                .andReturn();

        String submissionId = submissionIdFrom(submissionResult);

        mockMvc.perform(get("/api/submissions/{submissionId}/content", submissionId)
                        .cookie(studentCookie))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("coursework.pdf")))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(pdfBytes));
    }

    @Test
    void studentCannotUploadUnsupportedSubmissionType() throws Exception {
        User lecturer = ensureUser("lecturer-upload-" + UUID.randomUUID() + "@example.com", "Lecturer Upload", RoleName.LECTURER);
        User student = ensureUser("student-upload-" + UUID.randomUUID() + "@example.com", "Student Upload", RoleName.STUDENT);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail());
        Cookie studentCookie = loginAndReturnAuthCookie(student.getEmail());
        String spaceId = createSpaceAndReturnId(lecturerCookie);
        addStudentToSpace(lecturerCookie, spaceId, student.getEmail());

        String assignmentPayload = objectMapper.writeValueAsString(new CreateAssignmentPayload(
                "Supported Upload Coursework",
                "Only text and validated PDF uploads are supported in the current implementation.",
                Instant.now().plusSeconds(86400),
                UUID.fromString(spaceId)
        ));

        MvcResult assignmentResult = mockMvc.perform(post("/api/assignments")
                        .cookie(lecturerCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignmentPayload))
                .andExpect(status().isCreated())
                .andReturn();

        String assignmentId = submissionIdFrom(assignmentResult);

        MockMultipartFile imageUpload = new MockMultipartFile(
                "file",
                "coursework.png",
                "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47}
        );

        mockMvc.perform(multipart("/api/assignments/{assignmentId}/submissions", assignmentId)
                        .cookie(studentCookie)
                        .with(csrf())
                        .file(imageUpload))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.message").value("Only text/plain and application/pdf uploads are supported in the current submission flow"))
                .andExpect(jsonPath("$.errors._global[0]").value("Only text/plain and application/pdf uploads are supported in the current submission flow"));
    }

    @Test
    void studentCannotUploadPdfWithoutValidHeader() throws Exception {
        User lecturer = ensureUser("lecturer-pdf-header-" + UUID.randomUUID() + "@example.com", "Lecturer PDF Header", RoleName.LECTURER);
        User student = ensureUser("student-pdf-header-" + UUID.randomUUID() + "@example.com", "Student PDF Header", RoleName.STUDENT);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail());
        Cookie studentCookie = loginAndReturnAuthCookie(student.getEmail());

        String assignmentId = createAssignmentAndReturnId(lecturerCookie, "PDF Validation Coursework", "Malformed PDF uploads should be rejected.", student);

        MockMultipartFile invalidPdfUpload = new MockMultipartFile(
                "file",
                "coursework.pdf",
                "application/pdf",
                "not-a-real-pdf".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/assignments/{assignmentId}/submissions", assignmentId)
                        .cookie(studentCookie)
                        .with(csrf())
                        .file(invalidPdfUpload))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.message").value("Uploaded PDF files must include a valid PDF header"))
                .andExpect(jsonPath("$.errors._global[0]").value("Uploaded PDF files must include a valid PDF header"));
    }

    @Test
    void studentCannotUploadEmptySubmissionFile() throws Exception {
        User lecturer = ensureUser("lecturer-empty-" + UUID.randomUUID() + "@example.com", "Lecturer Empty", RoleName.LECTURER);
        User student = ensureUser("student-empty-" + UUID.randomUUID() + "@example.com", "Student Empty", RoleName.STUDENT);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail());
        Cookie studentCookie = loginAndReturnAuthCookie(student.getEmail());

        String assignmentId = createAssignmentAndReturnId(lecturerCookie, "Empty Upload Coursework", "Empty uploads should be rejected.", student);

        MockMultipartFile emptyUpload = new MockMultipartFile(
                "file",
                "empty.txt",
                "text/plain",
                new byte[0]
        );

        mockMvc.perform(multipart("/api/assignments/{assignmentId}/submissions", assignmentId)
                        .cookie(studentCookie)
                        .with(csrf())
                        .file(emptyUpload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Submission file must not be empty"))
                .andExpect(jsonPath("$.errors._global[0]").value("Submission file must not be empty"));
    }

    @Test
    void studentCannotUploadInvalidUtf8SubmissionFile() throws Exception {
        User lecturer = ensureUser("lecturer-utf8-" + UUID.randomUUID() + "@example.com", "Lecturer Utf8", RoleName.LECTURER);
        User student = ensureUser("student-utf8-" + UUID.randomUUID() + "@example.com", "Student Utf8", RoleName.STUDENT);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail());
        Cookie studentCookie = loginAndReturnAuthCookie(student.getEmail());

        String assignmentId = createAssignmentAndReturnId(lecturerCookie, "UTF-8 Upload Coursework", "Invalid UTF-8 uploads should be rejected.", student);

        MockMultipartFile invalidUtf8Upload = new MockMultipartFile(
                "file",
                "invalid.txt",
                "text/plain",
                new byte[]{(byte) 0xC3, (byte) 0x28}
        );

        mockMvc.perform(multipart("/api/assignments/{assignmentId}/submissions", assignmentId)
                        .cookie(studentCookie)
                        .with(csrf())
                        .file(invalidUtf8Upload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only UTF-8 text/plain files are supported"))
                .andExpect(jsonPath("$.errors._global[0]").value("Only UTF-8 text/plain files are supported"));
    }

    @Test
    void studentCannotUploadOversizedSubmissionFile() throws Exception {
        User lecturer = ensureUser("lecturer-size-" + UUID.randomUUID() + "@example.com", "Lecturer Size", RoleName.LECTURER);
        User student = ensureUser("student-size-" + UUID.randomUUID() + "@example.com", "Student Size", RoleName.STUDENT);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail());
        Cookie studentCookie = loginAndReturnAuthCookie(student.getEmail());

        String assignmentId = createAssignmentAndReturnId(lecturerCookie, "Oversized Upload Coursework", "Oversized uploads should be rejected.", student);

        MockMultipartFile oversizedUpload = new MockMultipartFile(
                "file",
                "large.txt",
                "text/plain",
                new byte[6 * 1024 * 1024]
        );

        mockMvc.perform(multipart("/api/assignments/{assignmentId}/submissions", assignmentId)
                        .cookie(studentCookie)
                        .with(csrf())
                        .file(oversizedUpload))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.message").value("Submission file exceeds the current 5MB limit"))
                .andExpect(jsonPath("$.errors._global[0]").value("Submission file exceeds the current 5MB limit"));
    }

    private String createAssignmentAndReturnId(Cookie lecturerCookie, String title, String description, User... enrolledStudents) throws Exception {
        String spaceId = createSpaceAndReturnId(lecturerCookie);
        for (User enrolledStudent : enrolledStudents) {
            addStudentToSpace(lecturerCookie, spaceId, enrolledStudent.getEmail());
        }

        String assignmentPayload = objectMapper.writeValueAsString(new CreateAssignmentPayload(
                title,
                description,
                Instant.now().plusSeconds(86400),
                UUID.fromString(spaceId)
        ));

        MvcResult assignmentResult = mockMvc.perform(post("/api/assignments")
                        .cookie(lecturerCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignmentPayload))
                .andExpect(status().isCreated())
                .andReturn();

        return submissionIdFrom(assignmentResult);
    }

    private String createSpaceAndReturnId(Cookie lecturerCookie) throws Exception {
        String createSpacePayload = objectMapper.writeValueAsString(new CreateSpacePayload(
                "Assignment Space " + UUID.randomUUID(),
                "space-" + UUID.randomUUID().toString().substring(0, 8),
                "Managed space for assignment visibility and submission scoping tests."
        ));

        MvcResult createSpaceResult = mockMvc.perform(post("/api/spaces")
                        .cookie(lecturerCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createSpacePayload))
                .andExpect(status().isCreated())
                .andReturn();

        return submissionIdFrom(createSpaceResult);
    }

    private void addStudentToSpace(Cookie lecturerCookie, String spaceId, String studentEmail) throws Exception {
        String addStudentPayload = objectMapper.writeValueAsString(new AddStudentPayload(studentEmail));
        mockMvc.perform(post("/api/spaces/{spaceId}/students", spaceId)
                        .cookie(lecturerCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addStudentPayload))
                .andExpect(status().isCreated());
    }

    private String uploadSubmissionAndReturnId(Cookie studentCookie, String assignmentId, String fileName, String contentType, byte[] contentBytes) throws Exception {
        MvcResult submissionResult = mockMvc.perform(multipart("/api/assignments/{assignmentId}/submissions", assignmentId)
                        .cookie(studentCookie)
                        .with(csrf())
                        .file(new MockMultipartFile(
                                "file",
                                fileName,
                                contentType,
                                contentBytes
                        )))
                .andExpect(status().isCreated())
                .andReturn();

        return submissionIdFrom(submissionResult);
    }

    private void removeStudentFromAssignmentSpace(UUID spaceId, UUID studentUserId) {
        SpaceMembership membership = spaceMembershipRepository.findBySpaceIdAndStudentUserId(spaceId, studentUserId)
                .orElseThrow();
        spaceMembershipRepository.delete(membership);
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

    private Cookie loginAndReturnAuthCookie(String email) throws Exception {
        String loginPayload = objectMapper.writeValueAsString(new LoginPayload(email, "StrongPass123"));
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andReturn();

        return authCookieFrom(loginResult);
    }

    private record LoginPayload(String email, String password) {
    }

    private record CreateAssignmentPayload(String title, String description, Instant dueAt, UUID spaceId) {
    }

    private record CreateSpacePayload(String name, String code, String description) {
    }

    private record AddStudentPayload(String studentEmail) {
    }


    private String submissionIdFrom(MvcResult result) throws Exception {
        JsonNode objectNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return objectNode.required("id").asString();
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

