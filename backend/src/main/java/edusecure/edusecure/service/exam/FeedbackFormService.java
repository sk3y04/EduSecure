package edusecure.edusecure.service.exam;

import edusecure.edusecure.audit.AuditService;
import edusecure.edusecure.dto.exam.CreateFeedbackFormQuestionRequest;
import edusecure.edusecure.dto.exam.CreateFeedbackFormRequest;
import edusecure.edusecure.dto.exam.FeedbackFormAnswerResponse;
import edusecure.edusecure.dto.exam.FeedbackFormQuestionResponse;
import edusecure.edusecure.dto.exam.FeedbackFormQuestionSummaryResponse;
import edusecure.edusecure.dto.exam.FeedbackFormResponse;
import edusecure.edusecure.dto.exam.FeedbackFormReviewResponse;
import edusecure.edusecure.dto.exam.FeedbackFormSubmissionReceiptResponse;
import edusecure.edusecure.dto.exam.FeedbackFormSubmissionResponse;
import edusecure.edusecure.dto.exam.SubmitFeedbackFormAnswerRequest;
import edusecure.edusecure.dto.exam.SubmitFeedbackFormRequest;
import edusecure.edusecure.dto.exam.UpdateFeedbackFormRequest;
import edusecure.edusecure.entity.audit.AuditActionType;
import edusecure.edusecure.entity.auth.RoleName;
import edusecure.edusecure.entity.auth.User;
import edusecure.edusecure.entity.exam.Exam;
import edusecure.edusecure.entity.exam.FeedbackForm;
import edusecure.edusecure.entity.exam.FeedbackFormAnswer;
import edusecure.edusecure.entity.exam.FeedbackFormQuestion;
import edusecure.edusecure.entity.exam.FeedbackFormSubmission;
import edusecure.edusecure.entity.exam.FeedbackQuestionType;
import edusecure.edusecure.entity.space.Space;
import edusecure.edusecure.repository.auth.UserRepository;
import edusecure.edusecure.repository.exam.ExamRepository;
import edusecure.edusecure.repository.exam.FeedbackFormAnswerRepository;
import edusecure.edusecure.repository.exam.FeedbackFormQuestionRepository;
import edusecure.edusecure.repository.exam.FeedbackFormRepository;
import edusecure.edusecure.repository.exam.FeedbackFormSubmissionRepository;
import edusecure.edusecure.repository.space.SpaceMembershipRepository;
import edusecure.edusecure.repository.space.SpaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedbackFormService {

    private final FeedbackFormRepository feedbackFormRepository;
    private final FeedbackFormQuestionRepository feedbackFormQuestionRepository;
    private final FeedbackFormSubmissionRepository feedbackFormSubmissionRepository;
    private final FeedbackFormAnswerRepository feedbackFormAnswerRepository;
    private final ExamRepository examRepository;
    private final SpaceRepository spaceRepository;
    private final SpaceMembershipRepository spaceMembershipRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Transactional
    public FeedbackFormResponse createForm(String currentUserEmail, UUID examId, CreateFeedbackFormRequest request) {
        User actor = findUserByEmail(currentUserEmail);
        Exam exam = findExam(examId);
        Space space = findSpace(exam.getSpaceId());
        requireStaffExamAccess(actor, space, "You cannot manage feedback forms for this exam");
        requireExamVisibleForAssessment(exam, space);

        List<CreateFeedbackFormQuestionRequest> normalizedQuestions = validateQuestionPayload(request.questions());
        Instant now = Instant.now();
        FeedbackForm savedForm = feedbackFormRepository.save(FeedbackForm.builder()
                .examId(examId)
                .title(request.title().trim())
                .description(normalizeOptionalText(request.description()))
                .published(request.published())
                .createdByUserId(actor.getId())
                .createdAt(now)
                .updatedAt(now)
                .build());

        List<FeedbackFormQuestion> savedQuestions = feedbackFormQuestionRepository.saveAll(toQuestionEntities(savedForm.getId(), normalizedQuestions));
        auditService.record(
                AuditActionType.FEEDBACK_FORM_CREATED,
                actor.getId(),
                FeedbackForm.class.getSimpleName(),
                savedForm.getId(),
                buildAuditDetail(true, examId, savedQuestions.size(), savedForm.isPublished())
        );

        return toFormResponse(savedForm, savedQuestions, exam, space, true, false, 0L);
    }

    @Transactional
    public FeedbackFormResponse updateForm(String currentUserEmail, UUID formId, UpdateFeedbackFormRequest request) {
        User actor = findUserByEmail(currentUserEmail);
        FeedbackForm form = findForm(formId);
        Exam exam = findExam(form.getExamId());
        Space space = findSpace(exam.getSpaceId());
        requireStaffExamAccess(actor, space, "You cannot manage this feedback form");
        requireExamVisibleForAssessment(exam, space);

        List<FeedbackFormQuestion> existingQuestions = feedbackFormQuestionRepository.findAllByFormIdOrderByDisplayOrderAsc(formId);
        List<CreateFeedbackFormQuestionRequest> normalizedQuestions = validateQuestionPayload(request.questions());
        boolean hasResponses = feedbackFormSubmissionRepository.existsByFormId(formId);

        if (hasResponses && hasStructuralQuestionChange(existingQuestions, normalizedQuestions)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Submitted feedback prevents structural question changes");
        }

        form.setTitle(request.title().trim());
        form.setDescription(normalizeOptionalText(request.description()));
        form.setPublished(request.published());
        form.setUpdatedAt(Instant.now());
        FeedbackForm savedForm = feedbackFormRepository.save(form);

        List<FeedbackFormQuestion> effectiveQuestions = existingQuestions;
        if (!hasResponses) {
            feedbackFormQuestionRepository.deleteAllByFormId(formId);
            effectiveQuestions = feedbackFormQuestionRepository.saveAll(toQuestionEntities(formId, normalizedQuestions));
        }

        auditService.record(
                AuditActionType.FEEDBACK_FORM_UPDATED,
                actor.getId(),
                FeedbackForm.class.getSimpleName(),
                savedForm.getId(),
                buildAuditDetail(false, savedForm.getId(), effectiveQuestions.size(), savedForm.isPublished())
        );

        return toFormResponse(
                savedForm,
                effectiveQuestions,
                exam,
                space,
                true,
                false,
                feedbackFormSubmissionRepository.countByFormId(formId)
        );
    }

    @Transactional(readOnly = true)
    public List<FeedbackFormResponse> listFormsForExam(UUID examId, String currentUserEmail) {
        User actor = findUserByEmail(currentUserEmail);
        Exam exam = findExam(examId);
        Space space = findSpace(exam.getSpaceId());

        if (canManageSpace(actor, space)) {
            return toFormResponses(
                    feedbackFormRepository.findAllByExamIdOrderByCreatedAtDesc(examId),
                    exam,
                    space,
                    true,
                    null
            );
        }

        requireStudentVisibleAccess(actor, exam, space, "You cannot access feedback forms for this exam");
        return toFormResponses(
                feedbackFormRepository.findAllByExamIdAndPublishedTrueOrderByCreatedAtDesc(examId),
                exam,
                space,
                false,
                actor
        );
    }

    @Transactional(readOnly = true)
    public FeedbackFormResponse getForm(UUID formId, String currentUserEmail) {
        User actor = findUserByEmail(currentUserEmail);
        FeedbackForm form = findForm(formId);
        Exam exam = findExam(form.getExamId());
        Space space = findSpace(exam.getSpaceId());
        List<FeedbackFormQuestion> questions = feedbackFormQuestionRepository.findAllByFormIdOrderByDisplayOrderAsc(formId);

        if (canManageSpace(actor, space)) {
            return toFormResponse(
                    form,
                    questions,
                    exam,
                    space,
                    true,
                    false,
                    feedbackFormSubmissionRepository.countByFormId(formId)
            );
        }

        requireStudentVisibleFormAccess(actor, form, exam, space, "You cannot access this feedback form");
        return toFormResponse(
                form,
                questions,
                exam,
                space,
                false,
                feedbackFormSubmissionRepository.findByFormIdAndStudentUserId(formId, actor.getId()).isPresent(),
                null
        );
    }

    @Transactional
    public FeedbackFormSubmissionReceiptResponse submitResponse(UUID formId, String currentUserEmail, SubmitFeedbackFormRequest request) {
        User student = findUserByEmail(currentUserEmail);
        FeedbackForm form = findForm(formId);
        Exam exam = findExam(form.getExamId());
        Space space = findSpace(exam.getSpaceId());
        requireStudentVisibleFormAccess(student, form, exam, space, "You cannot submit feedback for this form");

        if (feedbackFormSubmissionRepository.findByFormIdAndStudentUserId(formId, student.getId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already submitted feedback for this form");
        }

        List<FeedbackFormQuestion> questions = feedbackFormQuestionRepository.findAllByFormIdOrderByDisplayOrderAsc(formId);
        Map<UUID, SubmitFeedbackFormAnswerRequest> answersByQuestionId = validateAnswerPayload(questions, request.answers());

        Instant submittedAt = Instant.now();
        FeedbackFormSubmission submission = feedbackFormSubmissionRepository.save(FeedbackFormSubmission.builder()
                .formId(formId)
                .studentUserId(student.getId())
                .submittedAt(submittedAt)
                .build());

        feedbackFormAnswerRepository.saveAll(questions.stream()
                .map(question -> toAnswerEntity(submission.getId(), question, answersByQuestionId.get(question.getId())))
                .toList());

        return new FeedbackFormSubmissionReceiptResponse(submission.getId(), formId, submittedAt);
    }

    @Transactional(readOnly = true)
    public FeedbackFormReviewResponse getReview(UUID formId, String currentUserEmail) {
        User actor = findUserByEmail(currentUserEmail);
        FeedbackForm form = findForm(formId);
        Exam exam = findExam(form.getExamId());
        Space space = findSpace(exam.getSpaceId());
        requireStaffExamAccess(actor, space, "You cannot review responses for this feedback form");

        List<FeedbackFormQuestion> questions = feedbackFormQuestionRepository.findAllByFormIdOrderByDisplayOrderAsc(formId);
        List<FeedbackFormSubmission> submissions = feedbackFormSubmissionRepository.findAllByFormIdOrderBySubmittedAtAsc(formId);
        Map<UUID, List<FeedbackFormAnswer>> answersBySubmissionId = loadAnswersBySubmissionId(submissions);
        Map<UUID, User> studentsById = loadUsersById(submissions.stream().map(FeedbackFormSubmission::getStudentUserId).toList());

        List<FeedbackFormSubmissionResponse> submissionResponses = submissions.stream()
                .map(submission -> {
                    User student = studentsById.get(submission.getStudentUserId());
                    if (student == null) {
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found for feedback submission");
                    }
                    return new FeedbackFormSubmissionResponse(
                            submission.getId(),
                            student.getId(),
                            student.getEmail(),
                            student.getFullName(),
                            submission.getSubmittedAt(),
                            answersBySubmissionId.getOrDefault(submission.getId(), List.of()).stream()
                                    .sorted(Comparator.comparing(FeedbackFormAnswer::getQuestionId))
                                    .map(answer -> new FeedbackFormAnswerResponse(answer.getQuestionId(), answer.getRatingValue(), answer.getTextValue()))
                                    .toList()
                    );
                })
                .toList();

        List<FeedbackFormQuestionSummaryResponse> summaries = questions.stream()
                .map(question -> summarizeQuestion(question, submissions, answersBySubmissionId))
                .toList();

        return new FeedbackFormReviewResponse(
                form.getId(),
                form.getTitle(),
                exam.getTitle(),
                space.getCode(),
                space.getName(),
                submissions.size(),
                summaries,
                submissionResponses
        );
    }

    private List<FeedbackFormResponse> toFormResponses(
            List<FeedbackForm> forms,
            Exam exam,
            Space space,
            boolean canManage,
            User currentStudent
    ) {
        if (forms.isEmpty()) {
            return List.of();
        }

        Map<UUID, List<FeedbackFormQuestion>> questionsByFormId = feedbackFormQuestionRepository
                .findAllByFormIdInOrderByDisplayOrderAsc(forms.stream().map(FeedbackForm::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(FeedbackFormQuestion::getFormId, LinkedHashMap::new, Collectors.toList()));

        return forms.stream()
                .map(form -> toFormResponse(
                        form,
                        questionsByFormId.getOrDefault(form.getId(), List.of()),
                        exam,
                        space,
                        canManage,
                        currentStudent != null && feedbackFormSubmissionRepository.findByFormIdAndStudentUserId(form.getId(), currentStudent.getId()).isPresent(),
                        canManage ? feedbackFormSubmissionRepository.countByFormId(form.getId()) : null
                ))
                .toList();
    }

    private FeedbackFormResponse toFormResponse(
            FeedbackForm form,
            List<FeedbackFormQuestion> questions,
            Exam exam,
            Space space,
            boolean canManage,
            boolean alreadySubmitted,
            Long responseCount
    ) {
        return new FeedbackFormResponse(
                form.getId(),
                exam.getId(),
                exam.getTitle(),
                space.getId(),
                space.getCode(),
                space.getName(),
                form.getTitle(),
                form.getDescription(),
                form.isPublished(),
                form.getCreatedAt(),
                form.getUpdatedAt(),
                canManage,
                alreadySubmitted,
                responseCount,
                questions.stream()
                        .map(question -> new FeedbackFormQuestionResponse(
                                question.getId(),
                                question.getPrompt(),
                                question.getQuestionType(),
                                question.isRequired(),
                                question.getDisplayOrder()
                        ))
                        .toList()
        );
    }

    private List<CreateFeedbackFormQuestionRequest> validateQuestionPayload(List<CreateFeedbackFormQuestionRequest> questions) {
        if (questions.size() > 10) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A form may contain at most 10 questions");
        }

        Set<Integer> displayOrders = questions.stream().map(CreateFeedbackFormQuestionRequest::displayOrder).collect(Collectors.toSet());
        if (displayOrders.size() != questions.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question display order must be unique within a form");
        }

        return questions.stream()
                .sorted(Comparator.comparing(CreateFeedbackFormQuestionRequest::displayOrder))
                .toList();
    }

    private boolean hasStructuralQuestionChange(
            List<FeedbackFormQuestion> existingQuestions,
            List<CreateFeedbackFormQuestionRequest> requestedQuestions
    ) {
        if (existingQuestions.size() != requestedQuestions.size()) {
            return true;
        }

        List<FeedbackFormQuestion> sortedExisting = existingQuestions.stream()
                .sorted(Comparator.comparing(FeedbackFormQuestion::getDisplayOrder))
                .toList();

        for (int index = 0; index < sortedExisting.size(); index++) {
            FeedbackFormQuestion existing = sortedExisting.get(index);
            CreateFeedbackFormQuestionRequest requested = requestedQuestions.get(index);
            if (!existing.getPrompt().equals(requested.prompt().trim())
                    || existing.getQuestionType() != requested.questionType()
                    || existing.isRequired() != requested.required()
                    || !existing.getDisplayOrder().equals(requested.displayOrder())) {
                return true;
            }
        }

        return false;
    }

    private List<FeedbackFormQuestion> toQuestionEntities(UUID formId, List<CreateFeedbackFormQuestionRequest> questions) {
        return questions.stream()
                .map(question -> FeedbackFormQuestion.builder()
                        .formId(formId)
                        .prompt(question.prompt().trim())
                        .questionType(question.questionType())
                        .required(question.required())
                        .displayOrder(question.displayOrder())
                        .build())
                .toList();
    }

    private Map<UUID, SubmitFeedbackFormAnswerRequest> validateAnswerPayload(
            List<FeedbackFormQuestion> questions,
            List<SubmitFeedbackFormAnswerRequest> answers
    ) {
        Map<UUID, SubmitFeedbackFormAnswerRequest> answersByQuestionId = new HashMap<>();
        for (SubmitFeedbackFormAnswerRequest answer : answers) {
            if (answersByQuestionId.put(answer.questionId(), answer) != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Each question may be answered only once");
            }
        }

        Set<UUID> expectedQuestionIds = questions.stream().map(FeedbackFormQuestion::getId).collect(Collectors.toSet());
        if (!answersByQuestionId.keySet().equals(expectedQuestionIds)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Answers must cover each form question exactly once");
        }

        for (FeedbackFormQuestion question : questions) {
            normalizeAnswer(question, answersByQuestionId.get(question.getId()));
        }

        return answersByQuestionId;
    }

    private FeedbackFormAnswer toAnswerEntity(UUID submissionId, FeedbackFormQuestion question, SubmitFeedbackFormAnswerRequest request) {
        NormalizedAnswer normalizedAnswer = normalizeAnswer(question, request);
        return FeedbackFormAnswer.builder()
                .submissionId(submissionId)
                .questionId(question.getId())
                .ratingValue(normalizedAnswer.ratingValue())
                .textValue(normalizedAnswer.textValue())
                .build();
    }

    private NormalizedAnswer normalizeAnswer(FeedbackFormQuestion question, SubmitFeedbackFormAnswerRequest request) {
        if (question.getQuestionType() == FeedbackQuestionType.RATING) {
            if (request.ratingValue() == null) {
                if (question.isRequired()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A required rating answer is missing");
                }
                return new NormalizedAnswer(null, null);
            }
            if (request.ratingValue() < 1 || request.ratingValue() > 5) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rating answers must be between 1 and 5");
            }
            String unexpectedText = normalizeOptionalText(request.textValue());
            if (unexpectedText != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rating questions cannot include text answers");
            }
            return new NormalizedAnswer(request.ratingValue(), null);
        }

        if (request.ratingValue() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Text questions cannot include rating answers");
        }
        String normalizedText = normalizeOptionalText(request.textValue());
        if (question.isRequired() && normalizedText == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A required text answer is missing");
        }
        return new NormalizedAnswer(null, normalizedText);
    }

    private FeedbackFormQuestionSummaryResponse summarizeQuestion(
            FeedbackFormQuestion question,
            List<FeedbackFormSubmission> submissions,
            Map<UUID, List<FeedbackFormAnswer>> answersBySubmissionId
    ) {
        List<FeedbackFormAnswer> answers = submissions.stream()
                .flatMap(submission -> answersBySubmissionId.getOrDefault(submission.getId(), List.of()).stream())
                .filter(answer -> answer.getQuestionId().equals(question.getId()))
                .toList();

        if (question.getQuestionType() == FeedbackQuestionType.RATING) {
            Map<Integer, Long> counts = new LinkedHashMap<>();
            for (int rating = 1; rating <= 5; rating++) {
                final int targetRating = rating;
                counts.put(rating, answers.stream().filter(answer -> Integer.valueOf(targetRating).equals(answer.getRatingValue())).count());
            }
            double average = answers.stream()
                    .map(FeedbackFormAnswer::getRatingValue)
                    .filter(value -> value != null)
                    .mapToInt(Integer::intValue)
                    .average()
                    .orElse(0.0d);
            return new FeedbackFormQuestionSummaryResponse(
                    question.getId(),
                    question.getPrompt(),
                    question.getQuestionType(),
                    answers.stream().filter(answer -> answer.getRatingValue() != null).count(),
                    average == 0.0d && answers.stream().noneMatch(answer -> answer.getRatingValue() != null) ? null : average,
                    counts
            );
        }

        return new FeedbackFormQuestionSummaryResponse(
                question.getId(),
                question.getPrompt(),
                question.getQuestionType(),
                answers.stream().filter(answer -> answer.getTextValue() != null).count(),
                null,
                Map.of()
        );
    }

    private Map<UUID, List<FeedbackFormAnswer>> loadAnswersBySubmissionId(List<FeedbackFormSubmission> submissions) {
        if (submissions.isEmpty()) {
            return Map.of();
        }

        return feedbackFormAnswerRepository.findAllBySubmissionIdIn(submissions.stream().map(FeedbackFormSubmission::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(FeedbackFormAnswer::getSubmissionId));
    }

    private Map<UUID, User> loadUsersById(List<UUID> userIds) {
        return userRepository.findAllById(userIds.stream().distinct().toList())
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private void requireStaffExamAccess(User actor, Space space, String forbiddenMessage) {
        if (!canManageSpace(actor, space)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, forbiddenMessage);
        }
    }

    private void requireStudentVisibleAccess(User student, Exam exam, Space space, String forbiddenMessage) {
        if (!hasRole(student, RoleName.STUDENT)
                || !exam.isPublished()
                || !spaceMembershipRepository.existsBySpaceIdAndStudentUserId(space.getId(), student.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, forbiddenMessage);
        }
    }

    private void requireStudentVisibleFormAccess(
            User student,
            FeedbackForm form,
            Exam exam,
            Space space,
            String forbiddenMessage
    ) {
        if (!form.isPublished()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, forbiddenMessage);
        }
        requireStudentVisibleAccess(student, exam, space, forbiddenMessage);
    }

    private void requireExamVisibleForAssessment(Exam exam, Space space) {
        if (space.isArchived()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Archived spaces cannot accept feedback-form changes");
        }
        if (!exam.isPublished()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Feedback forms require a published exam");
        }
    }

    private boolean canManageSpace(User user, Space space) {
        return hasRole(user, RoleName.ADMIN)
                || hasRole(user, RoleName.LECTURER) && space.getCreatedByUserId().equals(user.getId());
    }

    private boolean hasRole(User user, RoleName roleName) {
        return user.getRoles().stream().anyMatch(role -> role.getName() == roleName);
    }

    private String buildAuditDetail(boolean create, UUID entityId, int questionCount, boolean published) {
        return (create ? "examId=" : "formId=")
                + entityId
                + ",questionCount=" + questionCount
                + ",published=" + published;
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private Exam findExam(UUID examId) {
        return examRepository.findById(examId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam not found"));
    }

    private Space findSpace(UUID spaceId) {
        return spaceRepository.findById(spaceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Space not found"));
    }

    private FeedbackForm findForm(UUID formId) {
        return feedbackFormRepository.findById(formId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Feedback form not found"));
    }

    private record NormalizedAnswer(Integer ratingValue, String textValue) {
    }
}