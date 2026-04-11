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
class FeedbackFormFlowIntegrationTests {

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
    void lecturerCanCreateUpdateAndReviewFeedbackFormAfterStudentSubmission() throws Exception {
        User lecturer = ensureUser("lecturer-feedback-" + UUID.randomUUID() + "@example.com", "Lecturer Feedback", RoleName.LECTURER);
        User student = ensureUser("student-feedback-" + UUID.randomUUID() + "@example.com", "Student Feedback", RoleName.STUDENT);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail(), "StrongPass123");
        Cookie studentCookie = loginAndReturnAuthCookie(student.getEmail(), "StrongPass123");

        String spaceId = createSpace(lecturerCookie, "Applied Security Feedback", "SEC-FEEDBACK", "Space for feedback-form integration tests.");
        addStudentToSpace(lecturerCookie, spaceId, student.getEmail());
        String examId = createExam(lecturerCookie, spaceId, "Applied Security Final", true);

        MvcResult createResult = mockMvc.perform(post("/api/exams/{examId}/feedback-forms", examId)
                        .cookie(lecturerCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateFeedbackFormPayload(
                                "Exam experience feedback",
                                "Help us improve the assessment.",
                                true,
                                List.of(
                                        new FeedbackQuestionPayload("How clear were the exam instructions?", "RATING", true, 1),
                                        new FeedbackQuestionPayload("What should change for future cohorts?", "TEXT", false, 2)
                                )
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Exam experience feedback"))
                .andExpect(jsonPath("$.responseCount").value(0))
                .andReturn();

        String formId = textField(objectMapper.readTree(createResult.getResponse().getContentAsString()), "id");

        mockMvc.perform(put("/api/feedback-forms/{formId}", formId)
                        .cookie(lecturerCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateFeedbackFormPayload(
                                "Exam experience feedback",
                                "Updated wording before responses arrive.",
                                true,
                                List.of(
                                        new FeedbackQuestionPayload("How clear were the exam instructions?", "RATING", true, 1),
                                        new FeedbackQuestionPayload("What should change for future cohorts?", "TEXT", false, 2)
                                )
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Updated wording before responses arrive."));

        MvcResult studentFormsResult = mockMvc.perform(get("/api/exams/{examId}/feedback-forms", examId)
                        .cookie(studentCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].alreadySubmitted").value(false))
                .andReturn();

        JsonNode studentFormNode = objectMapper.readTree(studentFormsResult.getResponse().getContentAsString()).get(0);
        String ratingQuestionId = textField(studentFormNode.required("questions").get(0), "id");
        String textQuestionId = textField(studentFormNode.required("questions").get(1), "id");

        mockMvc.perform(post("/api/feedback-forms/{formId}/responses", formId)
                        .cookie(studentCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubmitFeedbackPayload(List.of(
                                new SubmitFeedbackAnswerPayload(UUID.fromString(ratingQuestionId), 4, null),
                                new SubmitFeedbackAnswerPayload(UUID.fromString(textQuestionId), null, "More mock papers would help.")
                        )))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.formId").value(formId));

        mockMvc.perform(get("/api/feedback-forms/{formId}", formId)
                        .cookie(studentCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alreadySubmitted").value(true));

        mockMvc.perform(get("/api/feedback-forms/{formId}/responses", formId)
                        .cookie(lecturerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCount").value(1))
                .andExpect(jsonPath("$.questionSummaries[0].averageRating").value(4.0))
                .andExpect(jsonPath("$.submissions[0].studentEmail").value(student.getEmail()))
                .andExpect(jsonPath("$.submissions[0].answers.length()").value(2));

        List<AuditLog> auditLogs = auditLogRepository.findByEntityTypeAndEntityIdOrderByEventTimestampAsc("FeedbackForm", UUID.fromString(formId));
        assertThat(auditLogs)
                .extracting(log -> log.getActionType().name())
                .contains("FEEDBACK_FORM_CREATED", "FEEDBACK_FORM_UPDATED");
        assertThat(auditLogs).allSatisfy(log -> {
            assertThat(log.getIntegrityValue()).isNotBlank();
            assertThat(log.getDetailsJson())
                    .contains("questionCount=2")
                    .doesNotContain("More mock papers would help.");
        });
    }

    @Test
    void duplicateSubmissionAndDraftFormSubmissionAreRejected() throws Exception {
        User lecturer = ensureUser("lecturer-feedback-guard-" + UUID.randomUUID() + "@example.com", "Lecturer Feedback Guard", RoleName.LECTURER);
        User student = ensureUser("student-feedback-guard-" + UUID.randomUUID() + "@example.com", "Student Feedback Guard", RoleName.STUDENT);

        Cookie lecturerCookie = loginAndReturnAuthCookie(lecturer.getEmail(), "StrongPass123");
        Cookie studentCookie = loginAndReturnAuthCookie(student.getEmail(), "StrongPass123");

        String spaceId = createSpace(lecturerCookie, "Defence Feedback", "DEF-FEEDBACK", "Space for feedback submission guards.");
        addStudentToSpace(lecturerCookie, spaceId, student.getEmail());
        String examId = createExam(lecturerCookie, spaceId, "Defence Final", true);

        String publishedFormId = createFeedbackForm(lecturerCookie, examId, true);
        String draftFormId = createFeedbackForm(lecturerCookie, examId, false);

        JsonNode publishedFormNode = objectMapper.readTree(mockMvc.perform(get("/api/feedback-forms/{formId}", publishedFormId)
                        .cookie(lecturerCookie))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
        String ratingQuestionId = textField(publishedFormNode.required("questions").get(0), "id");
        JsonNode draftFormNode = objectMapper.readTree(mockMvc.perform(get("/api/feedback-forms/{formId}", draftFormId)
                        .cookie(lecturerCookie))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
        String draftRatingQuestionId = textField(draftFormNode.required("questions").get(0), "id");

        mockMvc.perform(post("/api/feedback-forms/{formId}/responses", draftFormId)
                        .cookie(studentCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubmitFeedbackPayload(List.of(
                                new SubmitFeedbackAnswerPayload(UUID.fromString(draftRatingQuestionId), 4, null)
                        )))))
                .andExpect(status().isForbidden());

        String submissionPayload = objectMapper.writeValueAsString(new SubmitFeedbackPayload(List.of(
                new SubmitFeedbackAnswerPayload(UUID.fromString(ratingQuestionId), 5, null)
        )));

        mockMvc.perform(post("/api/feedback-forms/{formId}/responses", publishedFormId)
                        .cookie(studentCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submissionPayload))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/feedback-forms/{formId}/responses", publishedFormId)
                        .cookie(studentCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submissionPayload))
                .andExpect(status().isConflict());
    }

    @Test
    void crossLecturerAccessIsDeniedAndStructuralQuestionChangesAreBlockedAfterResponses() throws Exception {
        User owner = ensureUser("lecturer-feedback-owner-" + UUID.randomUUID() + "@example.com", "Lecturer Feedback Owner", RoleName.LECTURER);
        User otherLecturer = ensureUser("lecturer-feedback-other-" + UUID.randomUUID() + "@example.com", "Lecturer Feedback Other", RoleName.LECTURER);
        User admin = ensureUser("admin-feedback-" + UUID.randomUUID() + "@example.com", "Admin Feedback", RoleName.ADMIN);
        User student = ensureUser("student-feedback-other-" + UUID.randomUUID() + "@example.com", "Student Feedback Other", RoleName.STUDENT);

        Cookie ownerCookie = loginAndReturnAuthCookie(owner.getEmail(), "StrongPass123");
        Cookie otherLecturerCookie = loginAndReturnAuthCookie(otherLecturer.getEmail(), "StrongPass123");
        Cookie adminCookie = loginAndReturnAuthCookie(admin.getEmail(), "StrongPass123");
        Cookie studentCookie = loginAndReturnAuthCookie(student.getEmail(), "StrongPass123");

        String spaceId = createSpace(ownerCookie, "Systems Feedback", "SYS-FEEDBACK", "Space for authorization checks.");
        addStudentToSpace(ownerCookie, spaceId, student.getEmail());
        String examId = createExam(ownerCookie, spaceId, "Systems Final", true);
        String formId = createFeedbackForm(ownerCookie, examId, true);

        JsonNode formNode = objectMapper.readTree(mockMvc.perform(get("/api/feedback-forms/{formId}", formId)
                        .cookie(ownerCookie))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
        String ratingQuestionId = textField(formNode.required("questions").get(0), "id");

        mockMvc.perform(post("/api/feedback-forms/{formId}/responses", formId)
                        .cookie(studentCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubmitFeedbackPayload(List.of(
                                new SubmitFeedbackAnswerPayload(UUID.fromString(ratingQuestionId), 3, null)
                        )))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/feedback-forms/{formId}/responses", formId)
                        .cookie(otherLecturerCookie))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/feedback-forms/{formId}", formId)
                        .cookie(ownerCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateFeedbackFormPayload(
                                "Single-question feedback",
                                "Trying to change the prompt after responses.",
                                true,
                                List.of(new FeedbackQuestionPayload("Changed prompt", "RATING", true, 1))
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Submitted feedback prevents structural question changes"));

        mockMvc.perform(get("/api/feedback-forms/{formId}/responses", formId)
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCount").value(1));
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
                                "Room F1",
                                startsAt,
                                endsAt,
                                published
                        ))))
                .andExpect(status().isCreated())
                .andReturn();

        return textField(objectMapper.readTree(result.getResponse().getContentAsString()), "id");
    }

    private String createFeedbackForm(Cookie lecturerCookie, String examId, boolean published) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/exams/{examId}/feedback-forms", examId)
                        .cookie(lecturerCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateFeedbackFormPayload(
                                published ? "Single-question feedback" : "Draft feedback",
                                "Feedback form helper.",
                                published,
                                List.of(new FeedbackQuestionPayload("How was the assessment experience?", "RATING", true, 1))
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

    private record FeedbackQuestionPayload(String prompt, String questionType, boolean required, Integer displayOrder) {
    }

    private record CreateFeedbackFormPayload(
            String title,
            String description,
            boolean published,
            List<FeedbackQuestionPayload> questions
    ) {
    }

    private record SubmitFeedbackAnswerPayload(UUID questionId, Integer ratingValue, String textValue) {
    }

    private record SubmitFeedbackPayload(List<SubmitFeedbackAnswerPayload> answers) {
    }
}