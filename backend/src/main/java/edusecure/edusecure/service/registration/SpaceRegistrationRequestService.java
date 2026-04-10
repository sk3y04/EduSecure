package edusecure.edusecure.service.registration;

import edusecure.edusecure.audit.AuditService;
import edusecure.edusecure.dto.registration.CreateSpaceRegistrationRequest;
import edusecure.edusecure.dto.registration.ReviewSpaceRegistrationRequest;
import edusecure.edusecure.dto.registration.ReviewSpaceRegistrationRequestResponse;
import edusecure.edusecure.dto.registration.StudentSpaceRegistrationRequestResponse;
import edusecure.edusecure.entity.audit.AuditActionType;
import edusecure.edusecure.entity.auth.RoleName;
import edusecure.edusecure.entity.auth.User;
import edusecure.edusecure.entity.registration.RegistrationRequestStatus;
import edusecure.edusecure.entity.registration.SpaceRegistrationRequest;
import edusecure.edusecure.entity.space.Space;
import edusecure.edusecure.entity.space.SpaceMembership;
import edusecure.edusecure.repository.auth.UserRepository;
import edusecure.edusecure.repository.registration.SpaceRegistrationRequestRepository;
import edusecure.edusecure.repository.space.SpaceMembershipRepository;
import edusecure.edusecure.repository.space.SpaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SpaceRegistrationRequestService {

    private final SpaceRegistrationRequestRepository requestRepository;
    private final SpaceRepository spaceRepository;
    private final SpaceMembershipRepository spaceMembershipRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Transactional
    public StudentSpaceRegistrationRequestResponse createRequest(
            String currentUserEmail,
            CreateSpaceRegistrationRequest request
    ) {
        User currentUser = findUserByEmail(currentUserEmail);
        requireRole(currentUser, RoleName.STUDENT, "Only students can create registration requests");

        Space space = spaceRepository.findByCode(normalizeCode(request.spaceCode()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Space code not found"));

        if (space.isArchived()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Archived spaces cannot accept registration requests");
        }

        if (spaceMembershipRepository.existsBySpaceIdAndStudentUserId(space.getId(), currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You are already assigned to this space");
        }

        if (requestRepository.existsBySpaceIdAndStudentUserIdAndStatus(
                space.getId(),
                currentUser.getId(),
                RegistrationRequestStatus.PENDING
        )) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A pending registration request already exists for this space");
        }

        SpaceRegistrationRequest saved = requestRepository.save(SpaceRegistrationRequest.builder()
                .spaceId(space.getId())
                .studentUserId(currentUser.getId())
                .status(RegistrationRequestStatus.PENDING)
                .requestMessage(normalizeOptionalText(request.requestMessage()))
                .requestedAt(Instant.now())
                .build());

        auditService.record(
                AuditActionType.SPACE_REGISTRATION_REQUEST_CREATED,
                currentUser.getId(),
                SpaceRegistrationRequest.class.getSimpleName(),
                saved.getId(),
                "spaceCode=" + space.getCode() + ",studentUserId=" + currentUser.getId()
        );

        return toStudentResponse(saved, space);
    }

    @Transactional(readOnly = true)
    public List<StudentSpaceRegistrationRequestResponse> listOwnRequests(String currentUserEmail) {
        User currentUser = findUserByEmail(currentUserEmail);
        requireRole(currentUser, RoleName.STUDENT, "Only students can view their registration requests");

        List<SpaceRegistrationRequest> requests = requestRepository.findAllByStudentUserIdOrderByRequestedAtDesc(currentUser.getId());
        Map<UUID, Space> spacesById = getSpacesById(requests);

        return requests.stream()
                .map(request -> toStudentResponse(request, requireSpace(spacesById, request.getSpaceId())))
                .toList();
    }

    @Transactional
    public StudentSpaceRegistrationRequestResponse cancelOwnRequest(String currentUserEmail, UUID requestId) {
        User currentUser = findUserByEmail(currentUserEmail);
        requireRole(currentUser, RoleName.STUDENT, "Only students can cancel registration requests");

        SpaceRegistrationRequest request = getRequestOrThrow(requestId);
        if (!request.getStudentUserId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to cancel this registration request");
        }
        ensurePending(request, "Only pending registration requests can be cancelled");

        request.setStatus(RegistrationRequestStatus.CANCELLED);
        request.setReviewNote(null);
        request.setReviewedAt(null);
        request.setReviewedByUserId(null);
        SpaceRegistrationRequest saved = requestRepository.save(request);
        Space space = getSpaceOrThrow(request.getSpaceId());

        auditService.record(
                AuditActionType.SPACE_REGISTRATION_REQUEST_CANCELLED,
                currentUser.getId(),
                SpaceRegistrationRequest.class.getSimpleName(),
                saved.getId(),
                "spaceCode=" + space.getCode() + ",studentUserId=" + currentUser.getId()
        );

        return toStudentResponse(saved, space);
    }

    @Transactional(readOnly = true)
    public List<ReviewSpaceRegistrationRequestResponse> listReviewQueue(String currentUserEmail) {
        User currentUser = findUserByEmail(currentUserEmail);
        List<SpaceRegistrationRequest> requests = listReviewableRequests(currentUser);
        Map<UUID, Space> spacesById = getSpacesById(requests);
        Map<UUID, User> usersById = getUsersById(requests.stream().map(SpaceRegistrationRequest::getStudentUserId).toList());

        return requests.stream()
                .map(request -> toReviewResponse(
                        request,
                        requireSpace(spacesById, request.getSpaceId()),
                        requireUser(usersById, request.getStudentUserId()),
                        true
                ))
                .toList();
    }

    @Transactional
    public ReviewSpaceRegistrationRequestResponse approveRequest(
            String currentUserEmail,
            UUID requestId,
            ReviewSpaceRegistrationRequest reviewRequest
    ) {
        User currentUser = findUserByEmail(currentUserEmail);
        SpaceRegistrationRequest request = getRequestOrThrow(requestId);
        Space space = getSpaceOrThrow(request.getSpaceId());
        requireReviewPermission(currentUser, space);
        ensurePending(request, "Only pending registration requests can be approved");

        if (space.isArchived()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Archived spaces cannot accept new students");
        }

        User student = findUserById(request.getStudentUserId());
        if (spaceMembershipRepository.existsBySpaceIdAndStudentUserId(space.getId(), student.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Student is already assigned to this space");
        }

        spaceMembershipRepository.save(SpaceMembership.builder()
                .spaceId(space.getId())
                .studentUserId(student.getId())
                .addedByUserId(currentUser.getId())
                .addedAt(Instant.now())
                .build());

        applyReviewDecision(request, currentUser.getId(), RegistrationRequestStatus.APPROVED, reviewRequest.reviewNote());
        SpaceRegistrationRequest saved = requestRepository.save(request);

        auditService.record(
                AuditActionType.SPACE_REGISTRATION_REQUEST_APPROVED,
                currentUser.getId(),
                SpaceRegistrationRequest.class.getSimpleName(),
                saved.getId(),
                "spaceCode=" + space.getCode() + ",studentUserId=" + student.getId()
        );

        return toReviewResponse(saved, space, student, true);
    }

    @Transactional
    public ReviewSpaceRegistrationRequestResponse rejectRequest(
            String currentUserEmail,
            UUID requestId,
            ReviewSpaceRegistrationRequest reviewRequest
    ) {
        User currentUser = findUserByEmail(currentUserEmail);
        SpaceRegistrationRequest request = getRequestOrThrow(requestId);
        Space space = getSpaceOrThrow(request.getSpaceId());
        requireReviewPermission(currentUser, space);
        ensurePending(request, "Only pending registration requests can be rejected");

        User student = findUserById(request.getStudentUserId());
        applyReviewDecision(request, currentUser.getId(), RegistrationRequestStatus.REJECTED, reviewRequest.reviewNote());
        SpaceRegistrationRequest saved = requestRepository.save(request);

        auditService.record(
                AuditActionType.SPACE_REGISTRATION_REQUEST_REJECTED,
                currentUser.getId(),
                SpaceRegistrationRequest.class.getSimpleName(),
                saved.getId(),
                "spaceCode=" + space.getCode() + ",studentUserId=" + student.getId()
        );

        return toReviewResponse(saved, space, student, true);
    }

    private void applyReviewDecision(
            SpaceRegistrationRequest request,
            UUID reviewerUserId,
            RegistrationRequestStatus status,
            String reviewNote
    ) {
        request.setStatus(status);
        request.setReviewedByUserId(reviewerUserId);
        request.setReviewedAt(Instant.now());
        request.setReviewNote(normalizeOptionalText(reviewNote));
    }

    private List<SpaceRegistrationRequest> listReviewableRequests(User currentUser) {
        if (hasRole(currentUser, RoleName.ADMIN)) {
            return requestRepository.findAllByStatusOrderByRequestedAtAsc(RegistrationRequestStatus.PENDING);
        }

        if (hasRole(currentUser, RoleName.LECTURER)) {
            return requestRepository.findReviewQueueByLecturerUserId(currentUser.getId(), RegistrationRequestStatus.PENDING);
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to review registration requests");
    }

    private void ensurePending(SpaceRegistrationRequest request, String message) {
        if (request.getStatus() != RegistrationRequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, message);
        }
    }

    private void requireReviewPermission(User currentUser, Space space) {
        if (!canReview(currentUser, space)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to review this registration request");
        }
    }

    private boolean canReview(User currentUser, Space space) {
        return hasRole(currentUser, RoleName.ADMIN)
                || hasRole(currentUser, RoleName.LECTURER) && space.getCreatedByUserId().equals(currentUser.getId());
    }

    private void requireRole(User user, RoleName roleName, String message) {
        if (!hasRole(user, roleName)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, message);
        }
    }

    private boolean hasRole(User user, RoleName roleName) {
        return user.getRoles().stream().anyMatch(role -> role.getName() == roleName);
    }

    private Map<UUID, Space> getSpacesById(List<SpaceRegistrationRequest> requests) {
        return spaceRepository.findAllById(requests.stream().map(SpaceRegistrationRequest::getSpaceId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Space::getId, Function.identity()));
    }

    private Map<UUID, User> getUsersById(List<UUID> userIds) {
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private Space requireSpace(Map<UUID, Space> spacesById, UUID spaceId) {
        Space space = spacesById.get(spaceId);
        if (space == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Space not found for registration request");
        }
        return space;
    }

    private User requireUser(Map<UUID, User> usersById, UUID userId) {
        User user = usersById.get(userId);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found for registration request");
        }
        return user;
    }

    private StudentSpaceRegistrationRequestResponse toStudentResponse(SpaceRegistrationRequest request, Space space) {
        return new StudentSpaceRegistrationRequestResponse(
                request.getId(),
                space.getId(),
                space.getCode(),
                space.getName(),
                request.getStatus(),
                request.getRequestMessage(),
                request.getRequestedAt(),
                request.getReviewedAt(),
                request.getReviewNote()
        );
    }

    private ReviewSpaceRegistrationRequestResponse toReviewResponse(
            SpaceRegistrationRequest request,
            Space space,
            User student,
            boolean canReview
    ) {
        return new ReviewSpaceRegistrationRequestResponse(
                request.getId(),
                space.getId(),
                space.getCode(),
                space.getName(),
                student.getId(),
                student.getEmail(),
                student.getFullName(),
                request.getStatus(),
                request.getRequestMessage(),
                request.getRequestedAt(),
                request.getReviewedAt(),
                request.getReviewNote(),
                canReview
        );
    }

    private SpaceRegistrationRequest getRequestOrThrow(UUID requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registration request not found"));
    }

    private Space getSpaceOrThrow(UUID spaceId) {
        return spaceRepository.findById(spaceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Space not found"));
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private User findUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private String normalizeCode(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}