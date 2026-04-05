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
    private SubmissionContentStore submissionContentStore;

    @Test
    void lecturerCreatesAssignmentAndStudentSubmitsVerifiedWork() throws Exception {
        User lecturer = ensureUser("lecturer-" + UUID.randomUUID() + "@example.com", "Lecturer Example", RoleName.LECTURER);
        User student = ensureUser("student-submission-" + UUID.randomUUID() + "@example.com", "Student Submitter", RoleName.STUDENT);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail());
        Cookie studentCookie = loginAndReturnAuthCookie(student.getEmail());

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
                        .file(submissionFile))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assignmentId").value(assignmentId))
                .andExpect(jsonPath("$.studentUserId").value(student.getId().toString()))
                .andExpect(jsonPath("$.storedFileReference").doesNotExist())
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
                .allSatisfy(log -> org.assertj.core.api.Assertions.assertThat(log.getIntegrityValue()).isNotBlank());
    }

    @Test
    void studentCanRetrieveLatestSubmissionForAssignment() throws Exception {
        User lecturer = ensureUser("lecturer-latest-" + UUID.randomUUID() + "@example.com", "Lecturer Latest", RoleName.LECTURER);
        User student = ensureUser("student-latest-" + UUID.randomUUID() + "@example.com", "Student Latest", RoleName.STUDENT);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail());
        Cookie studentCookie = loginAndReturnAuthCookie(student.getEmail());

        String assignmentId = createAssignmentAndReturnId(lecturerCookie, "Latest Submission Coursework", "Latest submission should be returned.");

        mockMvc.perform(multipart("/api/assignments/{assignmentId}/submissions", assignmentId)
                        .cookie(studentCookie)
                        .file(new MockMultipartFile(
                                "file",
                                "attempt-one.txt",
                                "text/plain",
                                "first attempt".getBytes(StandardCharsets.UTF_8)
                        )))
                .andExpect(status().isCreated());

        mockMvc.perform(multipart("/api/assignments/{assignmentId}/submissions", assignmentId)
                        .cookie(studentCookie)
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

        String assignmentId = createAssignmentAndReturnId(lecturerCookie, "Listing Coursework", "Lecturers should see assignment submissions.");

        MvcResult submissionResult = mockMvc.perform(multipart("/api/assignments/{assignmentId}/submissions", assignmentId)
                        .cookie(studentCookie)
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
    void studentAssignmentListIncludesLatestSubmissionMetadata() throws Exception {
        User lecturer = ensureUser("lecturer-assignment-list-" + UUID.randomUUID() + "@example.com", "Lecturer Assignment List", RoleName.LECTURER);
        User student = ensureUser("student-assignment-list-" + UUID.randomUUID() + "@example.com", "Student Assignment List", RoleName.STUDENT);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail());
        Cookie studentCookie = loginAndReturnAuthCookie(student.getEmail());

        String assignmentId = createAssignmentAndReturnId(lecturerCookie, "Visible Submission Coursework", "Student should see latest submission metadata on assignment listing.");

        MvcResult submissionResult = mockMvc.perform(multipart("/api/assignments/{assignmentId}/submissions", assignmentId)
                        .cookie(studentCookie)
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

        String assignmentId = createAssignmentAndReturnId(lecturerCookie, "No Submission Coursework", "No submission exists yet.");

        mockMvc.perform(get("/api/assignments/{assignmentId}/submissions/me", assignmentId)
                        .cookie(studentCookie))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No submission found for this assignment"))
                .andExpect(jsonPath("$.errors._global[0]").value("No submission found for this assignment"));
    }

    @Test
    void studentCannotReadAnotherStudentsSubmission() throws Exception {
        User lecturer = ensureUser("lecturer-guard-" + UUID.randomUUID() + "@example.com", "Lecturer Guard", RoleName.LECTURER);
        User owner = ensureUser("student-owner-" + UUID.randomUUID() + "@example.com", "Student Owner", RoleName.STUDENT);
        User otherStudent = ensureUser("student-other-" + UUID.randomUUID() + "@example.com", "Student Other", RoleName.STUDENT);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail());
        Cookie ownerCookie = loginAndReturnAuthCookie(owner.getEmail());
        Cookie otherStudentCookie = loginAndReturnAuthCookie(otherStudent.getEmail());

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

        String assignmentId = submissionIdFrom(assignmentResult);

        MockMultipartFile submissionFile = new MockMultipartFile(
                "file",
                "guarded.txt",
                "text/plain",
                "Owner-only submission content".getBytes(StandardCharsets.UTF_8)
        );

        MvcResult submissionResult = mockMvc.perform(multipart("/api/assignments/{assignmentId}/submissions", assignmentId)
                        .cookie(ownerCookie)
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
    void studentCannotCreateAssignment() throws Exception {
        User student = ensureUser("student-noassign-" + UUID.randomUUID() + "@example.com", "Student No Assign", RoleName.STUDENT);
        Cookie studentCookie = loginAndReturnAuthCookie(student.getEmail());

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

    @Test
    void studentCanUploadAndDownloadPdfSubmission() throws Exception {
        User lecturer = ensureUser("lecturer-pdf-" + UUID.randomUUID() + "@example.com", "Lecturer PDF", RoleName.LECTURER);
        User student = ensureUser("student-pdf-" + UUID.randomUUID() + "@example.com", "Student PDF", RoleName.STUDENT);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail());
        Cookie studentCookie = loginAndReturnAuthCookie(student.getEmail());

        String assignmentId = createAssignmentAndReturnId(lecturerCookie, "PDF Coursework", "PDF uploads should be accepted.");

        byte[] pdfBytes = "%PDF-1.4\n1 0 obj\n<< /Type /Catalog >>\nendobj\ntrailer\n<<>>\n%%EOF".getBytes(StandardCharsets.US_ASCII);

        MvcResult submissionResult = mockMvc.perform(multipart("/api/assignments/{assignmentId}/submissions", assignmentId)
                        .cookie(studentCookie)
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

        String assignmentPayload = objectMapper.writeValueAsString(new CreateAssignmentPayload(
                "Supported Upload Coursework",
                "Only text and validated PDF uploads are supported in the current implementation.",
                Instant.now().plusSeconds(86400)
        ));

        MvcResult assignmentResult = mockMvc.perform(post("/api/assignments")
                        .cookie(lecturerCookie)
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

        String assignmentId = createAssignmentAndReturnId(lecturerCookie, "PDF Validation Coursework", "Malformed PDF uploads should be rejected.");

        MockMultipartFile invalidPdfUpload = new MockMultipartFile(
                "file",
                "coursework.pdf",
                "application/pdf",
                "not-a-real-pdf".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/assignments/{assignmentId}/submissions", assignmentId)
                        .cookie(studentCookie)
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

        String assignmentId = createAssignmentAndReturnId(lecturerCookie, "Empty Upload Coursework", "Empty uploads should be rejected.");

        MockMultipartFile emptyUpload = new MockMultipartFile(
                "file",
                "empty.txt",
                "text/plain",
                new byte[0]
        );

        mockMvc.perform(multipart("/api/assignments/{assignmentId}/submissions", assignmentId)
                        .cookie(studentCookie)
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

        String assignmentId = createAssignmentAndReturnId(lecturerCookie, "UTF-8 Upload Coursework", "Invalid UTF-8 uploads should be rejected.");

        MockMultipartFile invalidUtf8Upload = new MockMultipartFile(
                "file",
                "invalid.txt",
                "text/plain",
                new byte[]{(byte) 0xC3, (byte) 0x28}
        );

        mockMvc.perform(multipart("/api/assignments/{assignmentId}/submissions", assignmentId)
                        .cookie(studentCookie)
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

        String assignmentId = createAssignmentAndReturnId(lecturerCookie, "Oversized Upload Coursework", "Oversized uploads should be rejected.");

        MockMultipartFile oversizedUpload = new MockMultipartFile(
                "file",
                "large.txt",
                "text/plain",
                new byte[6 * 1024 * 1024]
        );

        mockMvc.perform(multipart("/api/assignments/{assignmentId}/submissions", assignmentId)
                        .cookie(studentCookie)
                        .file(oversizedUpload))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.message").value("Submission file exceeds the current 5MB limit"))
                .andExpect(jsonPath("$.errors._global[0]").value("Submission file exceeds the current 5MB limit"));
    }

    private String createAssignmentAndReturnId(Cookie lecturerCookie, String title, String description) throws Exception {
        String assignmentPayload = objectMapper.writeValueAsString(new CreateAssignmentPayload(
                title,
                description,
                Instant.now().plusSeconds(86400)
        ));

        MvcResult assignmentResult = mockMvc.perform(post("/api/assignments")
                        .cookie(lecturerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignmentPayload))
                .andExpect(status().isCreated())
                .andReturn();

        return submissionIdFrom(assignmentResult);
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

