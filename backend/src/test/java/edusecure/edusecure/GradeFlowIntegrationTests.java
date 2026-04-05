package edusecure.edusecure;

import edusecure.edusecure.entity.assignment.Assignment;
import edusecure.edusecure.entity.audit.AuditLog;
import edusecure.edusecure.entity.auth.Role;
import edusecure.edusecure.entity.auth.RoleName;
import edusecure.edusecure.entity.space.Space;
import edusecure.edusecure.entity.space.SpaceMembership;
import edusecure.edusecure.entity.submission.Submission;
import edusecure.edusecure.entity.submission.SubmissionVerificationStatus;
import edusecure.edusecure.entity.auth.User;
import edusecure.edusecure.repository.assignment.AssignmentRepository;
import edusecure.edusecure.repository.audit.AuditLogRepository;
import edusecure.edusecure.repository.auth.RoleRepository;
import edusecure.edusecure.repository.space.SpaceMembershipRepository;
import edusecure.edusecure.repository.space.SpaceRepository;
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
import tools.jackson.databind.JsonNode;
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
    private SpaceRepository spaceRepository;

    @Autowired
    private SpaceMembershipRepository spaceMembershipRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    void lecturerCanCreateAndUpdateGradeAndStudentCanRetrieveOwnGrade() throws Exception {
        User lecturer = ensureUser("lecturer-grade-" + UUID.randomUUID() + "@example.com", "Lecturer Grade", RoleName.LECTURER);
        User student = ensureUser("student-grade-" + UUID.randomUUID() + "@example.com", "Student Grade", RoleName.STUDENT);
        Submission submission = ensureSubmissionForStudent(student, SubmissionVerificationStatus.VERIFIED, lecturer);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail(), "StrongPass123");
        Cookie studentCookie = loginAndReturnAuthCookie(student.getEmail(), "StrongPass123");

        String createPayload = objectMapper.writeValueAsString(new CreateGradePayload(78, "Strong work overall."));
        MvcResult createResult = mockMvc.perform(post("/api/submissions/{submissionId}/grade", submission.getId())
                        .cookie(lecturerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.submissionId").value(submission.getId().toString()))
                .andExpect(jsonPath("$.value").value(78))
                .andReturn();

        String gradeId = textField(objectMapper.readTree(createResult.getResponse().getContentAsString()), "id");

        String updatePayload = objectMapper.writeValueAsString(new UpdateGradePayload(81, "Updated after remarking."));
        mockMvc.perform(put("/api/grades/{gradeId}", gradeId)
                        .cookie(lecturerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value(81))
                .andExpect(jsonPath("$.lastModifiedAt").isNotEmpty());

        mockMvc.perform(get("/api/my/grades/{gradeId}", gradeId)
                        .cookie(studentCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(gradeId))
                .andExpect(jsonPath("$.value").value(81));

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
        Submission submission = ensureSubmissionForStudent(student, SubmissionVerificationStatus.VERIFIED, lecturer);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail(), "StrongPass123");
        Cookie studentCookie = loginAndReturnAuthCookie(student.getEmail(), "StrongPass123");

        String createPayload = objectMapper.writeValueAsString(new CreateGradePayload(70, "Initial feedback."));
        MvcResult createResult = mockMvc.perform(post("/api/submissions/{submissionId}/grade", submission.getId())
                        .cookie(lecturerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andReturn();
        String gradeId = textField(objectMapper.readTree(createResult.getResponse().getContentAsString()), "id");

        mockMvc.perform(post("/api/submissions/{submissionId}/grade", submission.getId())
                        .cookie(studentCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isForbidden());

        String updatePayload = objectMapper.writeValueAsString(new UpdateGradePayload(75, "Student should not update grades."));
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
        Submission submission = ensureSubmissionForStudent(student, SubmissionVerificationStatus.VERIFIED, lecturer);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail(), "StrongPass123");
        String createPayload = objectMapper.writeValueAsString(new CreateGradePayload(65, "First grade."));

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
        Submission failedSubmission = ensureSubmissionForStudent(student, SubmissionVerificationStatus.FAILED_VERIFICATION, lecturer);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail(), "StrongPass123");
        String createPayload = objectMapper.writeValueAsString(new CreateGradePayload(55, "Should fail because submission was not verified."));

        mockMvc.perform(post("/api/submissions/{submissionId}/grade", failedSubmission.getId())
                        .cookie(lecturerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void studentCannotReadAnotherStudentsGrade() throws Exception {
        User lecturer = ensureUser("lecturer-grade-owner-" + UUID.randomUUID() + "@example.com", "Lecturer Owner", RoleName.LECTURER);
        User owner = ensureUser("student-grade-owner-" + UUID.randomUUID() + "@example.com", "Student Owner Grade", RoleName.STUDENT);
        User otherStudent = ensureUser("student-grade-other-" + UUID.randomUUID() + "@example.com", "Student Other Grade", RoleName.STUDENT);
        Submission submission = ensureSubmissionForStudent(owner, SubmissionVerificationStatus.VERIFIED, lecturer);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail(), "StrongPass123");
        Cookie otherStudentCookie = loginAndReturnAuthCookie(otherStudent.getEmail(), "StrongPass123");

        String createPayload = objectMapper.writeValueAsString(new CreateGradePayload(88, "Visible only to owner."));
        MvcResult createResult = mockMvc.perform(post("/api/submissions/{submissionId}/grade", submission.getId())
                        .cookie(lecturerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andReturn();
        String gradeId = textField(objectMapper.readTree(createResult.getResponse().getContentAsString()), "id");

        mockMvc.perform(get("/api/my/grades/{gradeId}", gradeId)
                        .cookie(otherStudentCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentCannotUsePrivilegedGradeReadEndpoints() throws Exception {
        User lecturer = ensureUser("lecturer-grade-priv-read-" + UUID.randomUUID() + "@example.com", "Lecturer Privileged Grade Read", RoleName.LECTURER);
        User student = ensureUser("student-grade-priv-read-" + UUID.randomUUID() + "@example.com", "Student Privileged Grade Read", RoleName.STUDENT);
        Submission submission = ensureSubmissionForStudent(student, SubmissionVerificationStatus.VERIFIED, lecturer);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail(), "StrongPass123");
        Cookie studentCookie = loginAndReturnAuthCookie(student.getEmail(), "StrongPass123");

        String gradeId = createGradeAndReturnId(lecturerCookie, submission.getId(), 73, "Privileged read target.");

        mockMvc.perform(get("/api/grades/{gradeId}", gradeId)
                        .cookie(studentCookie))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/submissions/{submissionId}/grade", submission.getId())
                        .cookie(studentCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentCannotReadAnotherStudentsGradeBySubmissionId() throws Exception {
        User lecturer = ensureUser("lect-grade-sub-own-" + UUID.randomUUID() + "@example.com", "Lecturer Grade Submission Owner", RoleName.LECTURER);
        User owner = ensureUser("stu-grade-own-sub-" + UUID.randomUUID() + "@example.com", "Student Grade Own Submission", RoleName.STUDENT);
        User otherStudent = ensureUser("stu-grade-other-sub-" + UUID.randomUUID() + "@example.com", "Student Grade Other Submission", RoleName.STUDENT);
        Submission submission = ensureSubmissionForStudent(owner, SubmissionVerificationStatus.VERIFIED, lecturer);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail(), "StrongPass123");
        Cookie otherStudentCookie = loginAndReturnAuthCookie(otherStudent.getEmail(), "StrongPass123");

        createGradeAndReturnId(lecturerCookie, submission.getId(), 84, "Owner-only grade by submission.");

        mockMvc.perform(get("/api/my/submissions/{submissionId}/grade", submission.getId())
                        .cookie(otherStudentCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void lecturerCanRetrieveGradeBySubmissionId() throws Exception {
        User lecturer = ensureUser("lecturer-grade-submission-" + UUID.randomUUID() + "@example.com", "Lecturer Submission Grade", RoleName.LECTURER);
        User student = ensureUser("student-grade-submission-" + UUID.randomUUID() + "@example.com", "Student Submission Grade", RoleName.STUDENT);
        Submission submission = ensureSubmissionForStudent(student, SubmissionVerificationStatus.VERIFIED, lecturer);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail(), "StrongPass123");

        String createPayload = objectMapper.writeValueAsString(new CreateGradePayload(100, "Excellent verified submission."));
        mockMvc.perform(post("/api/submissions/{submissionId}/grade", submission.getId())
                        .cookie(lecturerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/submissions/{submissionId}/grade", submission.getId())
                        .cookie(lecturerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.submissionId").value(submission.getId().toString()))
                .andExpect(jsonPath("$.value").value(100));
    }

    @Test
    void studentCanRetrieveOwnGradeBySubmissionId() throws Exception {
        User lecturer = ensureUser("lect-grade-stu-sub-" + UUID.randomUUID() + "@example.com", "Lecturer Student Submission Grade", RoleName.LECTURER);
        User student = ensureUser("stu-grade-own-sub-" + UUID.randomUUID() + "@example.com", "Student Own Submission Grade", RoleName.STUDENT);
        Submission submission = ensureSubmissionForStudent(student, SubmissionVerificationStatus.VERIFIED, lecturer);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail(), "StrongPass123");
        Cookie studentCookie = loginAndReturnAuthCookie(student.getEmail(), "StrongPass123");

        String createPayload = objectMapper.writeValueAsString(new CreateGradePayload(92, "Excellent cryptographic evaluation."));
        mockMvc.perform(post("/api/submissions/{submissionId}/grade", submission.getId())
                        .cookie(lecturerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/my/submissions/{submissionId}/grade", submission.getId())
                        .cookie(studentCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.submissionId").value(submission.getId().toString()))
                .andExpect(jsonPath("$.value").value(92))
                .andExpect(jsonPath("$.feedback").value("Excellent cryptographic evaluation."));
    }

    @Test
    void studentCannotReadOwnGradeAfterLosingAssignmentSpaceMembership() throws Exception {
        User lecturer = ensureUser("lect-grade-space-loss-" + UUID.randomUUID() + "@example.com", "Lecturer Grade Space Loss", RoleName.LECTURER);
        User student = ensureUser("stu-grade-space-loss-" + UUID.randomUUID() + "@example.com", "Student Grade Space Loss", RoleName.STUDENT);
        Submission submission = ensureSubmissionForStudent(student, SubmissionVerificationStatus.VERIFIED, lecturer);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail(), "StrongPass123");
        Cookie studentCookie = loginAndReturnAuthCookie(student.getEmail(), "StrongPass123");

        String gradeId = createGradeAndReturnId(lecturerCookie, submission.getId(), 86, "Grade before space removal.");
        Assignment assignment = assignmentRepository.findById(submission.getAssignmentId()).orElseThrow();
        removeStudentFromAssignmentSpace(assignment.getSpaceId(), student.getId());

        mockMvc.perform(get("/api/my/grades/{gradeId}", gradeId)
                        .cookie(studentCookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You cannot view this grade"));

        mockMvc.perform(get("/api/my/submissions/{submissionId}/grade", submission.getId())
                        .cookie(studentCookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You cannot view this grade"));
    }

    @Test
    void lecturerCannotReadOrUpdateAnotherLecturersGrade() throws Exception {
        User ownerLecturer = ensureUser("lecturer-grade-policy-owner-" + UUID.randomUUID() + "@example.com", "Lecturer Grade Policy Owner", RoleName.LECTURER);
        User otherLecturer = ensureUser("lecturer-grade-policy-other-" + UUID.randomUUID() + "@example.com", "Lecturer Grade Policy Other", RoleName.LECTURER);
        User student = ensureUser("student-grade-policy-" + UUID.randomUUID() + "@example.com", "Student Grade Policy", RoleName.STUDENT);
        Submission submission = ensureSubmissionForStudent(student, SubmissionVerificationStatus.VERIFIED, ownerLecturer);

        Cookie ownerLecturerCookie = loginAndReturnAuthCookie(ownerLecturer.getEmail(), "StrongPass123");
        Cookie otherLecturerCookie = loginAndReturnAuthCookie(otherLecturer.getEmail(), "StrongPass123");

        String gradeId = createGradeAndReturnId(ownerLecturerCookie, submission.getId(), 77, "Original owner grade.");

        mockMvc.perform(get("/api/grades/{gradeId}", gradeId)
                        .cookie(otherLecturerCookie))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/submissions/{submissionId}/grade", submission.getId())
                        .cookie(otherLecturerCookie))
                .andExpect(status().isForbidden());

        String updatePayload = objectMapper.writeValueAsString(new UpdateGradePayload(79, "Updated by another lecturer under current policy."));
        mockMvc.perform(put("/api/grades/{gradeId}", gradeId)
                        .cookie(otherLecturerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isForbidden());
    }

    @Test
    void lecturerCannotCreateGradeForAnotherLecturersSubmission() throws Exception {
        User ownerLecturer = ensureUser("lecturer-grade-create-owner-" + UUID.randomUUID() + "@example.com", "Lecturer Grade Create Owner", RoleName.LECTURER);
        User otherLecturer = ensureUser("lecturer-grade-create-other-" + UUID.randomUUID() + "@example.com", "Lecturer Grade Create Other", RoleName.LECTURER);
        User student = ensureUser("student-grade-create-policy-" + UUID.randomUUID() + "@example.com", "Student Grade Create Policy", RoleName.STUDENT);
        Submission submission = ensureSubmissionForStudent(student, SubmissionVerificationStatus.VERIFIED, ownerLecturer);

        Cookie otherLecturerCookie = loginAndReturnAuthCookie(otherLecturer.getEmail(), "StrongPass123");

        mockMvc.perform(post("/api/submissions/{submissionId}/grade", submission.getId())
                        .cookie(otherLecturerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateGradePayload(91, "Created by another lecturer under current policy."))))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateReadAndUpdateGradeAcrossLecturerOwnershipBoundaries() throws Exception {
        User ownerLecturer = ensureUser("lecturer-grade-admin-owner-" + UUID.randomUUID() + "@example.com", "Lecturer Grade Admin Owner", RoleName.LECTURER);
        User admin = ensureUser("admin-grade-policy-" + UUID.randomUUID() + "@example.com", "Admin Grade Policy", RoleName.ADMIN);
        User student = ensureUser("student-grade-admin-policy-" + UUID.randomUUID() + "@example.com", "Student Grade Admin Policy", RoleName.STUDENT);
        Submission submission = ensureSubmissionForStudent(student, SubmissionVerificationStatus.VERIFIED, ownerLecturer);

        Cookie adminCookie = loginAndReturnAuthCookie(admin.getEmail(), "StrongPass123");

        MvcResult createResult = mockMvc.perform(post("/api/submissions/{submissionId}/grade", submission.getId())
                        .cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateGradePayload(91, "Created by admin across lecturer ownership boundaries."))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.submissionId").value(submission.getId().toString()))
                .andExpect(jsonPath("$.value").value(91))
                .andReturn();

        String gradeId = textField(objectMapper.readTree(createResult.getResponse().getContentAsString()), "id");

        mockMvc.perform(get("/api/grades/{gradeId}", gradeId)
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(gradeId))
                .andExpect(jsonPath("$.submissionId").value(submission.getId().toString()))
                .andExpect(jsonPath("$.value").value(91));

        mockMvc.perform(get("/api/submissions/{submissionId}/grade", submission.getId())
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(gradeId))
                .andExpect(jsonPath("$.value").value(91));

        mockMvc.perform(put("/api/grades/{gradeId}", gradeId)
                        .cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateGradePayload(94, "Updated by admin across lecturer ownership boundaries."))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value(94))
                .andExpect(jsonPath("$.feedback").value("Updated by admin across lecturer ownership boundaries."));
    }

    @Test
    void gradedSubmissionAppearsAsGradedInLecturerAssignmentSubmissionList() throws Exception {
        User lecturer = ensureUser("lecturer-grade-badge-" + UUID.randomUUID() + "@example.com", "Lecturer Grade Badge", RoleName.LECTURER);
        User student = ensureUser("student-grade-badge-" + UUID.randomUUID() + "@example.com", "Student Grade Badge", RoleName.STUDENT);
        Submission submission = ensureSubmissionForStudent(student, SubmissionVerificationStatus.VERIFIED, lecturer);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail(), "StrongPass123");

        String createPayload = objectMapper.writeValueAsString(new CreateGradePayload(67, "Marked submission."));
        MvcResult createResult = mockMvc.perform(post("/api/submissions/{submissionId}/grade", submission.getId())
                        .cookie(lecturerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andReturn();

        String gradeId = textField(objectMapper.readTree(createResult.getResponse().getContentAsString()), "id");

        mockMvc.perform(get("/api/assignments/{assignmentId}/submissions", submission.getAssignmentId())
                        .cookie(lecturerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(submission.getId().toString()))
                .andExpect(jsonPath("$[0].graded").value(true))
                .andExpect(jsonPath("$[0].gradeId").value(gradeId));
    }

    @Test
    void gradePercentageMustBeBetweenZeroAndHundred() throws Exception {
        User lecturer = ensureUser("lecturer-grade-bounds-" + UUID.randomUUID() + "@example.com", "Lecturer Bounds", RoleName.LECTURER);
        User student = ensureUser("student-grade-bounds-" + UUID.randomUUID() + "@example.com", "Student Bounds", RoleName.STUDENT);
        Submission submission = ensureSubmissionForStudent(student, SubmissionVerificationStatus.VERIFIED, lecturer);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail(), "StrongPass123");

        String negativePayload = objectMapper.writeValueAsString(new CreateGradePayload(-1, "Below range."));
        mockMvc.perform(post("/api/submissions/{submissionId}/grade", submission.getId())
                        .cookie(lecturerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(negativePayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.value[0]").value("Grade percentage must be between 0 and 100"));

        String oversizedPayload = objectMapper.writeValueAsString(new CreateGradePayload(101, "Above range."));
        mockMvc.perform(post("/api/submissions/{submissionId}/grade", submission.getId())
                        .cookie(lecturerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(oversizedPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.value[0]").value("Grade percentage must be between 0 and 100"));

        String zeroPayload = objectMapper.writeValueAsString(new CreateGradePayload(0, "Valid lower boundary."));
        mockMvc.perform(post("/api/submissions/{submissionId}/grade", submission.getId())
                        .cookie(lecturerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(zeroPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.value").value(0));
    }

    private Submission ensureSubmissionForStudent(User student, SubmissionVerificationStatus verificationStatus, User assignmentOwner) {
        Instant now = Instant.now();
        Space space = spaceRepository.save(Space.builder()
                .name("Space " + UUID.randomUUID())
                .code("SPACE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .description("Test grading space")
                .createdByUserId(assignmentOwner.getId())
                .createdAt(now)
                .updatedAt(now)
                .archived(false)
                .build());

        spaceMembershipRepository.save(SpaceMembership.builder()
                .spaceId(space.getId())
                .studentUserId(student.getId())
                .addedByUserId(assignmentOwner.getId())
                .addedAt(now)
                .build());

        Assignment assignment = assignmentRepository.save(Assignment.builder()
                .title("Assignment " + UUID.randomUUID())
                .description("Test assignment")
                .dueAt(Instant.now().plusSeconds(86400))
                .createdByLecturerId(assignmentOwner.getId())
                .spaceId(space.getId())
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
                .signatureAlgorithm("SHA256withECDSA")
                .verificationStatus(verificationStatus)
                .verificationMessage(verificationStatus.name())
                .build());
    }

    private void removeStudentFromAssignmentSpace(UUID spaceId, UUID studentUserId) {
        SpaceMembership membership = spaceMembershipRepository.findBySpaceIdAndStudentUserId(spaceId, studentUserId)
                .orElseThrow();
        spaceMembershipRepository.delete(membership);
    }

    private String createGradeAndReturnId(Cookie lecturerCookie, UUID submissionId, Integer value, String feedback) throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/submissions/{submissionId}/grade", submissionId)
                        .cookie(lecturerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateGradePayload(value, feedback))))
                .andExpect(status().isCreated())
                .andReturn();

        return textField(objectMapper.readTree(createResult.getResponse().getContentAsString()), "id");
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

    private static String textField(JsonNode objectNode, String fieldName) {
        return objectNode.required(fieldName).asString();
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

    private record CreateGradePayload(Integer value, String feedback) {
    }

    private record UpdateGradePayload(Integer value, String feedback) {
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
