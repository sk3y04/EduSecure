package edusecure.edusecure.controller.attendance;

import edusecure.edusecure.dto.attendance.AttendanceSessionRecordsResponse;
import edusecure.edusecure.dto.attendance.AttendanceSessionResponse;
import edusecure.edusecure.dto.attendance.CreateAttendanceSessionRequest;
import edusecure.edusecure.dto.attendance.UpdateAttendanceRecordsRequest;
import edusecure.edusecure.dto.attendance.UpdateAttendanceSessionRequest;
import edusecure.edusecure.service.attendance.AttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/attendance-sessions")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @GetMapping
    public ResponseEntity<List<AttendanceSessionResponse>> listSessions(Authentication authentication) {
        return ResponseEntity.ok(attendanceService.listSessions(authentication.getName()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('LECTURER', 'ADMIN')")
    public ResponseEntity<AttendanceSessionResponse> createSession(
            @Valid @RequestBody CreateAttendanceSessionRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(attendanceService.createSession(authentication.getName(), request));
    }

    @PutMapping("/{sessionId}")
    @PreAuthorize("hasAnyRole('LECTURER', 'ADMIN')")
    public ResponseEntity<AttendanceSessionResponse> updateSession(
            @PathVariable UUID sessionId,
            @Valid @RequestBody UpdateAttendanceSessionRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(attendanceService.updateSession(authentication.getName(), sessionId, request));
    }

    @GetMapping("/{sessionId}/records")
    @PreAuthorize("hasAnyRole('LECTURER', 'ADMIN')")
    public ResponseEntity<AttendanceSessionRecordsResponse> getSessionRecords(
            @PathVariable UUID sessionId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(attendanceService.getSessionRecords(authentication.getName(), sessionId));
    }

    @PutMapping("/{sessionId}/records")
    @PreAuthorize("hasAnyRole('LECTURER', 'ADMIN')")
    public ResponseEntity<AttendanceSessionRecordsResponse> updateSessionRecords(
            @PathVariable UUID sessionId,
            @Valid @RequestBody UpdateAttendanceRecordsRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(attendanceService.updateSessionRecords(authentication.getName(), sessionId, request));
    }
}

