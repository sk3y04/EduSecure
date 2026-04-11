package edusecure.edusecure.service.attendance;

import edusecure.edusecure.audit.AuditService;
import edusecure.edusecure.dto.attendance.AttendanceRecordRequest;
import edusecure.edusecure.dto.attendance.AttendanceSessionRecordsResponse;
import edusecure.edusecure.dto.attendance.AttendanceSessionResponse;
import edusecure.edusecure.dto.attendance.AttendanceStudentRecordResponse;
import edusecure.edusecure.dto.attendance.CreateAttendanceSessionRequest;
import edusecure.edusecure.dto.attendance.UpdateAttendanceRecordsRequest;
import edusecure.edusecure.dto.attendance.UpdateAttendanceSessionRequest;
import edusecure.edusecure.entity.attendance.AttendanceRecord;
import edusecure.edusecure.entity.attendance.AttendanceSession;
import edusecure.edusecure.entity.attendance.AttendanceStatus;
import edusecure.edusecure.entity.audit.AuditActionType;
import edusecure.edusecure.entity.auth.RoleName;
import edusecure.edusecure.entity.auth.User;
import edusecure.edusecure.entity.space.Space;
import edusecure.edusecure.entity.space.SpaceMembership;
import edusecure.edusecure.repository.attendance.AttendanceRecordRepository;
import edusecure.edusecure.repository.attendance.AttendanceSessionRepository;
import edusecure.edusecure.repository.auth.UserRepository;
import edusecure.edusecure.repository.space.SpaceMembershipRepository;
import edusecure.edusecure.repository.space.SpaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceSessionRepository attendanceSessionRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final SpaceRepository spaceRepository;
    private final SpaceMembershipRepository spaceMembershipRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Transactional
    public AttendanceSessionResponse createSession(String currentUserEmail, CreateAttendanceSessionRequest request) {
        User currentUser = findUserByEmail(currentUserEmail);
        Space space = getSpaceOrThrow(request.spaceId());
        requireManagePermission(currentUser, space);
        requireSpaceWritable(space);
        validateScheduleWindow(request.startsAt(), request.endsAt());

        Instant now = Instant.now();
        AttendanceSession savedSession = attendanceSessionRepository.save(AttendanceSession.builder()
                .spaceId(space.getId())
                .title(request.title().trim())
                .description(normalizeOptionalText(request.description()))
                .startsAt(request.startsAt())
                .endsAt(request.endsAt())
                .createdByUserId(currentUser.getId())
                .createdAt(now)
                .updatedAt(now)
                .build());

        List<AttendanceRecord> snapshotRecords = spaceMembershipRepository.findAllBySpaceIdOrderByAddedAtAsc(space.getId())
                .stream()
                .map(membership -> AttendanceRecord.builder()
                        .sessionId(savedSession.getId())
                        .studentUserId(membership.getStudentUserId())
                        .build())
                .toList();
        attendanceRecordRepository.saveAll(snapshotRecords);

        SummaryCounts counts = summarize(snapshotRecords);
        auditService.record(
                AuditActionType.ATTENDANCE_SESSION_CREATED,
                currentUser.getId(),
                AttendanceSession.class.getSimpleName(),
                savedSession.getId(),
                buildSessionAuditDetail(space, counts.memberCount(), savedSession)
        );

        return toSessionResponse(savedSession, space, true, null, counts);
    }

    @Transactional(readOnly = true)
    public List<AttendanceSessionResponse> listSessions(String currentUserEmail) {
        User currentUser = findUserByEmail(currentUserEmail);

        List<AttendanceSession> sessions;
        boolean canManage;
        UUID currentStudentId = null;

        if (hasRole(currentUser, RoleName.ADMIN)) {
            sessions = attendanceSessionRepository.findAllByOrderByStartsAtDesc();
            canManage = true;
        } else if (hasRole(currentUser, RoleName.LECTURER)) {
            List<UUID> ownedSpaceIds = spaceRepository.findAllByCreatedByUserId(currentUser.getId()).stream()
                    .map(Space::getId)
                    .toList();
            sessions = ownedSpaceIds.isEmpty()
                    ? List.of()
                    : attendanceSessionRepository.findAllBySpaceIdInOrderByStartsAtDesc(ownedSpaceIds);
            canManage = true;
        } else if (hasRole(currentUser, RoleName.STUDENT)) {
            List<UUID> memberSpaceIds = spaceMembershipRepository.findAllByStudentUserId(currentUser.getId()).stream()
                    .map(SpaceMembership::getSpaceId)
                    .distinct()
                    .toList();
            sessions = memberSpaceIds.isEmpty()
                    ? List.of()
                    : attendanceSessionRepository.findAllBySpaceIdInOrderByStartsAtDesc(memberSpaceIds);
            canManage = false;
            currentStudentId = currentUser.getId();
        } else {
            return List.of();
        }

        if (sessions.isEmpty()) {
            return List.of();
        }

        Map<UUID, Space> spacesById = loadSpacesById(sessions);
        List<UUID> sessionIds = sessions.stream().map(AttendanceSession::getId).toList();
        Map<UUID, List<AttendanceRecord>> recordsBySessionId = attendanceRecordRepository.findAllBySessionIdIn(sessionIds).stream()
                .collect(Collectors.groupingBy(AttendanceRecord::getSessionId));
        Map<UUID, AttendanceStatus> myStatusesBySessionId = new HashMap<>();
        if (currentStudentId != null) {
            attendanceRecordRepository.findAllByStudentUserIdAndSessionIdIn(currentStudentId, sessionIds)
                    .forEach(record -> myStatusesBySessionId.put(record.getSessionId(), record.getStatus()));
        }

        UUID effectiveStudentId = currentStudentId;
        return sessions.stream()
                .map(session -> {
                    Space space = requireSpace(spacesById, session.getSpaceId());
                    List<AttendanceRecord> records = recordsBySessionId.getOrDefault(session.getId(), List.of());
                    SummaryCounts counts = summarize(records);
                    return toSessionResponse(
                            session,
                            space,
                            canManage,
                            effectiveStudentId == null ? null : myStatusesBySessionId.get(session.getId()),
                            counts
                    );
                })
                .toList();
    }

    @Transactional
    public AttendanceSessionResponse updateSession(String currentUserEmail, UUID sessionId, UpdateAttendanceSessionRequest request) {
        User currentUser = findUserByEmail(currentUserEmail);
        AttendanceSession session = getSessionOrThrow(sessionId);
        Space space = getSpaceOrThrow(session.getSpaceId());
        requireManagePermission(currentUser, space);
        requireSpaceWritable(space);
        validateScheduleWindow(request.startsAt(), request.endsAt());

        session.setTitle(request.title().trim());
        session.setDescription(normalizeOptionalText(request.description()));
        session.setStartsAt(request.startsAt());
        session.setEndsAt(request.endsAt());
        session.setUpdatedAt(Instant.now());

        AttendanceSession saved = attendanceSessionRepository.save(session);
        List<AttendanceRecord> records = attendanceRecordRepository.findAllBySessionId(sessionId);
        SummaryCounts counts = summarize(records);

        auditService.record(
                AuditActionType.ATTENDANCE_SESSION_UPDATED,
                currentUser.getId(),
                AttendanceSession.class.getSimpleName(),
                saved.getId(),
                buildSessionAuditDetail(space, counts.memberCount(), saved)
        );

        return toSessionResponse(saved, space, true, null, counts);
    }

    @Transactional(readOnly = true)
    public AttendanceSessionRecordsResponse getSessionRecords(String currentUserEmail, UUID sessionId) {
        User currentUser = findUserByEmail(currentUserEmail);
        AttendanceSession session = getSessionOrThrow(sessionId);
        Space space = getSpaceOrThrow(session.getSpaceId());
        requireManagePermission(currentUser, space);

        return buildSessionRecordsResponse(session, space);
    }

    @Transactional
    public AttendanceSessionRecordsResponse updateSessionRecords(
            String currentUserEmail,
            UUID sessionId,
            UpdateAttendanceRecordsRequest request
    ) {
        User currentUser = findUserByEmail(currentUserEmail);
        AttendanceSession session = getSessionOrThrow(sessionId);
        Space space = getSpaceOrThrow(session.getSpaceId());
        requireManagePermission(currentUser, space);
        requireSpaceWritable(space);

        List<AttendanceRecord> existingRecords = attendanceRecordRepository.findAllBySessionId(sessionId);
        Map<UUID, AttendanceRecord> recordsByStudentId = existingRecords.stream()
                .collect(Collectors.toMap(AttendanceRecord::getStudentUserId, Function.identity()));

        ensureNoDuplicateStudentIds(request.records());

        Instant now = Instant.now();
        for (AttendanceRecordRequest recordRequest : request.records()) {
            AttendanceRecord existingRecord = recordsByStudentId.get(recordRequest.studentUserId());
            if (existingRecord == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Attendance record does not exist for this student in this session");
            }

            if (recordRequest.status() == null) {
                existingRecord.setStatus(null);
                existingRecord.setRecordedByUserId(null);
                existingRecord.setRecordedAt(null);
            } else {
                existingRecord.setStatus(recordRequest.status());
                existingRecord.setRecordedByUserId(currentUser.getId());
                existingRecord.setRecordedAt(now);
            }
        }

        attendanceRecordRepository.saveAll(existingRecords);
        auditService.record(
                AuditActionType.ATTENDANCE_RECORDS_UPDATED,
                currentUser.getId(),
                AttendanceSession.class.getSimpleName(),
                session.getId(),
                buildRecordsAuditDetail(space, session.getId(), request.records().size())
        );

        return buildSessionRecordsResponse(session, space);
    }

    private AttendanceSessionRecordsResponse buildSessionRecordsResponse(AttendanceSession session, Space space) {
        List<AttendanceRecord> records = attendanceRecordRepository.findAllBySessionId(session.getId());
        Map<UUID, User> usersById = userRepository.findAllById(records.stream().map(AttendanceRecord::getStudentUserId).toList())
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<AttendanceStudentRecordResponse> recordResponses = records.stream()
                .sorted(Comparator.comparing((AttendanceRecord record) -> requireUser(usersById, record.getStudentUserId()).getFullName(), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(record -> requireUser(usersById, record.getStudentUserId()).getEmail(), String.CASE_INSENSITIVE_ORDER))
                .map(record -> {
                    User student = requireUser(usersById, record.getStudentUserId());
                    return new AttendanceStudentRecordResponse(
                            student.getId(),
                            student.getEmail(),
                            student.getFullName(),
                            record.getStatus(),
                            record.getRecordedByUserId(),
                            record.getRecordedAt()
                    );
                })
                .toList();

        SummaryCounts counts = summarize(records);
        return new AttendanceSessionRecordsResponse(
                toSessionResponse(session, space, true, null, counts),
                recordResponses
        );
    }

    private SummaryCounts summarize(List<AttendanceRecord> records) {
        long memberCount = records.size();
        long recordedCount = records.stream().filter(record -> record.getStatus() != null).count();
        long presentCount = countByStatus(records, AttendanceStatus.PRESENT);
        long lateCount = countByStatus(records, AttendanceStatus.LATE);
        long absentCount = countByStatus(records, AttendanceStatus.ABSENT);
        long excusedCount = countByStatus(records, AttendanceStatus.EXCUSED);
        return new SummaryCounts(memberCount, recordedCount, presentCount, lateCount, absentCount, excusedCount);
    }

    private long countByStatus(List<AttendanceRecord> records, AttendanceStatus status) {
        return records.stream().filter(record -> record.getStatus() == status).count();
    }

    private AttendanceSessionResponse toSessionResponse(
            AttendanceSession session,
            Space space,
            boolean canManage,
            AttendanceStatus myStatus,
            SummaryCounts counts
    ) {
        return new AttendanceSessionResponse(
                session.getId(),
                session.getSpaceId(),
                space.getCode(),
                space.getName(),
                session.getTitle(),
                session.getDescription(),
                session.getStartsAt(),
                session.getEndsAt(),
                session.getCreatedByUserId(),
                session.getCreatedAt(),
                session.getUpdatedAt(),
                canManage,
                myStatus,
                counts.memberCount(),
                counts.recordedCount(),
                counts.presentCount(),
                counts.lateCount(),
                counts.absentCount(),
                counts.excusedCount()
        );
    }

    private Map<UUID, Space> loadSpacesById(List<AttendanceSession> sessions) {
        Map<UUID, Space> spacesById = new HashMap<>();
        spaceRepository.findAllById(sessions.stream().map(AttendanceSession::getSpaceId).distinct().toList())
                .forEach(space -> spacesById.put(space.getId(), space));
        return spacesById;
    }

    private void ensureNoDuplicateStudentIds(List<AttendanceRecordRequest> requests) {
        Set<UUID> uniqueIds = new HashSet<>();
        for (AttendanceRecordRequest request : requests) {
            if (!uniqueIds.add(request.studentUserId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Each student may appear only once in an attendance update request");
            }
        }
    }

    private void validateScheduleWindow(Instant startsAt, Instant endsAt) {
        if (!endsAt.isAfter(startsAt)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End time must be after start time");
        }
    }

    private void requireSpaceWritable(Space space) {
        if (space.isArchived()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Archived spaces cannot accept attendance changes");
        }
    }

    private void requireManagePermission(User currentUser, Space space) {
        if (!canManageSpace(currentUser, space)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to manage attendance for this space");
        }
    }

    private boolean canManageSpace(User currentUser, Space space) {
        return hasRole(currentUser, RoleName.ADMIN)
                || hasRole(currentUser, RoleName.LECTURER) && space.getCreatedByUserId().equals(currentUser.getId());
    }

    private boolean hasRole(User user, RoleName roleName) {
        return user.getRoles().stream().anyMatch(role -> role.getName() == roleName);
    }

    private String buildSessionAuditDetail(Space space, long memberCount, AttendanceSession session) {
        return "spaceCode=" + space.getCode()
                + ",memberCount=" + memberCount
                + ",startsAt=" + session.getStartsAt()
                + ",endsAt=" + session.getEndsAt();
    }

    private String buildRecordsAuditDetail(Space space, UUID sessionId, int updatedCount) {
        return "spaceCode=" + space.getCode()
                + ",sessionId=" + sessionId
                + ",updatedCount=" + updatedCount;
    }

    private Space requireSpace(Map<UUID, Space> spacesById, UUID spaceId) {
        Space space = spacesById.get(spaceId);
        if (space == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Space not found for attendance session");
        }
        return space;
    }

    private User requireUser(Map<UUID, User> usersById, UUID userId) {
        User user = usersById.get(userId);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found for attendance record");
        }
        return user;
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

    private Space getSpaceOrThrow(UUID spaceId) {
        return spaceRepository.findById(spaceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Space not found"));
    }

    private AttendanceSession getSessionOrThrow(UUID sessionId) {
        return attendanceSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attendance session not found"));
    }

    private record SummaryCounts(
            long memberCount,
            long recordedCount,
            long presentCount,
            long lateCount,
            long absentCount,
            long excusedCount
    ) {
    }
}


