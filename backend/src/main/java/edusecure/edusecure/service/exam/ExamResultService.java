package edusecure.edusecure.service.exam;

import edusecure.edusecure.audit.AuditService;
import edusecure.edusecure.dto.exam.CreateExamResultRequest;
import edusecure.edusecure.dto.exam.ExamResultResponse;
import edusecure.edusecure.dto.exam.MyExamResultResponse;
import edusecure.edusecure.dto.exam.UpdateExamResultRequest;
import edusecure.edusecure.entity.audit.AuditActionType;
import edusecure.edusecure.entity.auth.RoleName;
import edusecure.edusecure.entity.auth.User;
import edusecure.edusecure.entity.exam.Exam;
import edusecure.edusecure.entity.exam.ExamResult;
import edusecure.edusecure.entity.space.Space;
import edusecure.edusecure.repository.auth.UserRepository;
import edusecure.edusecure.repository.exam.ExamRepository;
import edusecure.edusecure.repository.exam.ExamResultRepository;
import edusecure.edusecure.repository.space.SpaceMembershipRepository;
import edusecure.edusecure.repository.space.SpaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExamResultService {

    private final ExamResultRepository examResultRepository;
    private final ExamRepository examRepository;
    private final SpaceRepository spaceRepository;
    private final SpaceMembershipRepository spaceMembershipRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Transactional
    public ExamResultResponse createExamResult(
            String currentUserEmail,
            UUID examId,
            CreateExamResultRequest request,
            Authentication authentication
    ) {
        User actor = findUserByEmail(currentUserEmail);
        Exam exam = findExam(examId);
        Space space = findSpace(exam.getSpaceId());
        requireStaffExamAccess(exam, space, actor, authentication, "You cannot manage exam results for this exam");

        User student = findUserByEmail(request.studentEmail().trim());
        ensureStudentTarget(student, space);

        if (examResultRepository.findByExamIdAndStudentUserId(examId, student.getId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An exam result already exists for this student");
        }

        Instant now = Instant.now();
        ExamResult saved = examResultRepository.save(ExamResult.builder()
                .examId(examId)
                .studentUserId(student.getId())
                .value(request.value())
                .feedback(normalizeOptionalText(request.feedback()))
                .published(request.published())
                .gradedByUserId(actor.getId())
                .gradedAt(now)
                .publishedAt(request.published() ? now : null)
                .build());

        auditService.record(
                AuditActionType.EXAM_RESULT_CREATED,
                actor.getId(),
                ExamResult.class.getSimpleName(),
                saved.getId(),
                buildAuditDetail(saved)
        );

        return toStaffResponse(saved, exam, space, student);
    }

    @Transactional
    public ExamResultResponse updateExamResult(
            String currentUserEmail,
            UUID examResultId,
            UpdateExamResultRequest request,
            Authentication authentication
    ) {
        User actor = findUserByEmail(currentUserEmail);
        ExamResult examResult = examResultRepository.findById(examResultId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam result not found"));
        Exam exam = findExam(examResult.getExamId());
        Space space = findSpace(exam.getSpaceId());
        requireStaffExamAccess(exam, space, actor, authentication, "You cannot manage this exam result");
        User student = findUserById(examResult.getStudentUserId());

        examResult.setValue(request.value());
        examResult.setFeedback(normalizeOptionalText(request.feedback()));
        examResult.setPublished(request.published());
        examResult.setGradedByUserId(actor.getId());
        examResult.setLastModifiedAt(Instant.now());
        examResult.setPublishedAt(request.published() ? Instant.now() : null);

        ExamResult saved = examResultRepository.save(examResult);
        auditService.record(
                AuditActionType.EXAM_RESULT_UPDATED,
                actor.getId(),
                ExamResult.class.getSimpleName(),
                saved.getId(),
                buildAuditDetail(saved)
        );

        return toStaffResponse(saved, exam, space, student);
    }

    @Transactional(readOnly = true)
    public List<ExamResultResponse> listExamResults(UUID examId, Authentication authentication) {
        User actor = findUserByEmail(authentication.getName());
        Exam exam = findExam(examId);
        Space space = findSpace(exam.getSpaceId());
        requireStaffExamAccess(exam, space, actor, authentication, "You cannot access exam results for this exam");

        List<ExamResult> examResults = examResultRepository.findAllByExamIdOrderByGradedAtAsc(examId);
        Map<UUID, User> studentsById = loadUsersById(examResults.stream().map(ExamResult::getStudentUserId).toList());

        return examResults.stream()
                .map(examResult -> {
                    User student = studentsById.get(examResult.getStudentUserId());
                    if (student == null) {
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found for exam result");
                    }
                    return toStaffResponse(examResult, exam, space, student);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public ExamResultResponse getExamResult(UUID examResultId, Authentication authentication) {
        User actor = findUserByEmail(authentication.getName());
        ExamResult examResult = examResultRepository.findById(examResultId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam result not found"));
        Exam exam = findExam(examResult.getExamId());
        Space space = findSpace(exam.getSpaceId());
        requireStaffExamAccess(exam, space, actor, authentication, "You cannot access this exam result");
        User student = findUserById(examResult.getStudentUserId());

        return toStaffResponse(examResult, exam, space, student);
    }

    @Transactional(readOnly = true)
    public List<MyExamResultResponse> listMyExamResults(Authentication authentication) {
        User student = findUserByEmail(authentication.getName());
        List<ExamResult> examResults = examResultRepository.findAllByStudentUserIdAndPublishedTrueOrderByPublishedAtDescGradedAtDesc(student.getId());

        Map<UUID, Exam> examsById = loadExamsById(examResults.stream().map(ExamResult::getExamId).toList());
        Map<UUID, Space> spacesById = loadSpacesById(examsById.values().stream().map(Exam::getSpaceId).toList());

        return examResults.stream()
                .filter(examResult -> isVisibleToStudent(student, examResult, examsById, spacesById))
                .map(examResult -> toStudentResponse(examResult, examsById.get(examResult.getExamId()), spacesById))
                .toList();
    }

    @Transactional(readOnly = true)
    public MyExamResultResponse getMyExamResultForExam(UUID examId, Authentication authentication) {
        User student = findUserByEmail(authentication.getName());
        Exam exam = findExam(examId);
        ExamResult examResult = examResultRepository.findByExamIdAndStudentUserId(examId, student.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam result not found for this exam"));
        Space space = findSpace(exam.getSpaceId());

        ensureStudentCanView(student, examResult, exam, space);
        return toStudentResponse(examResult, exam, space);
    }

    private ExamResultResponse toStaffResponse(ExamResult examResult, Exam exam, Space space, User student) {
        return new ExamResultResponse(
                examResult.getId(),
                examResult.getExamId(),
                exam.getTitle(),
                space.getId(),
                space.getCode(),
                space.getName(),
                student.getId(),
                student.getEmail(),
                student.getFullName(),
                examResult.getValue(),
                examResult.getFeedback(),
                examResult.isPublished(),
                examResult.getGradedByUserId(),
                examResult.getGradedAt(),
                examResult.getLastModifiedAt(),
                examResult.getPublishedAt()
        );
    }

    private MyExamResultResponse toStudentResponse(ExamResult examResult, Exam exam, Map<UUID, Space> spacesById) {
        Space space = spacesById.get(exam.getSpaceId());
        if (space == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Space not found for exam result");
        }
        return toStudentResponse(examResult, exam, space);
    }

    private MyExamResultResponse toStudentResponse(ExamResult examResult, Exam exam, Space space) {
        return new MyExamResultResponse(
                examResult.getId(),
                examResult.getExamId(),
                exam.getTitle(),
                space.getCode(),
                space.getName(),
                examResult.getValue(),
                examResult.getFeedback(),
                examResult.getPublishedAt(),
                examResult.getLastModifiedAt()
        );
    }

    private boolean isVisibleToStudent(
            User student,
            ExamResult examResult,
            Map<UUID, Exam> examsById,
            Map<UUID, Space> spacesById
    ) {
        Exam exam = examsById.get(examResult.getExamId());
        if (exam == null) {
            return false;
        }
        Space space = spacesById.get(exam.getSpaceId());
        if (space == null) {
            return false;
        }
        return canStudentView(student, examResult, exam, space);
    }

    private void ensureStudentCanView(User student, ExamResult examResult, Exam exam, Space space) {
        if (!canStudentView(student, examResult, exam, space)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot view this exam result");
        }
    }

    private boolean canStudentView(User student, ExamResult examResult, Exam exam, Space space) {
        return examResult.getStudentUserId().equals(student.getId())
                && examResult.isPublished()
                && exam.isPublished()
                && spaceMembershipRepository.existsBySpaceIdAndStudentUserId(space.getId(), student.getId());
    }

    private void ensureStudentTarget(User student, Space space) {
        if (!hasRole(student, RoleName.STUDENT)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT, "User is not a student and cannot receive an exam result");
        }
        if (!spaceMembershipRepository.existsBySpaceIdAndStudentUserId(space.getId(), student.getId())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT, "Student is not assigned to the exam's space");
        }
    }

    private void requireStaffExamAccess(
            Exam exam,
            Space space,
            User actor,
            Authentication authentication,
            String forbiddenMessage
    ) {
        if (hasRole(actor, RoleName.ADMIN)) {
            return;
        }

        boolean lecturerRole = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_LECTURER".equals(authority.getAuthority()));
        if (lecturerRole && space.getCreatedByUserId().equals(actor.getId())) {
            return;
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, forbiddenMessage);
    }

    private Map<UUID, User> loadUsersById(List<UUID> userIds) {
        Map<UUID, User> usersById = new HashMap<>();
        userRepository.findAllById(userIds.stream().distinct().toList())
                .forEach(user -> usersById.put(user.getId(), user));
        return usersById;
    }

    private Map<UUID, Exam> loadExamsById(List<UUID> examIds) {
        Map<UUID, Exam> examsById = new HashMap<>();
        examRepository.findAllById(examIds.stream().distinct().toList())
                .forEach(exam -> examsById.put(exam.getId(), exam));
        return examsById;
    }

    private Map<UUID, Space> loadSpacesById(List<UUID> spaceIds) {
        Map<UUID, Space> spacesById = new HashMap<>();
        spaceRepository.findAllById(spaceIds.stream().distinct().toList())
                .forEach(space -> spacesById.put(space.getId(), space));
        return spacesById;
    }

    private String buildAuditDetail(ExamResult examResult) {
        return "examId=" + examResult.getExamId()
                + ",studentUserId=" + examResult.getStudentUserId()
                + ",value=" + examResult.getValue()
                + ",published=" + examResult.isPublished();
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean hasRole(User user, RoleName roleName) {
        return user.getRoles().stream().anyMatch(role -> role.getName() == roleName);
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private User findUserById(UUID userId) {
        return userRepository.findById(userId)
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
}