package edusecure.edusecure.controller;

import edusecure.edusecure.dto.AssignmentResponse;
import edusecure.edusecure.dto.AssignmentSummaryResponse;
import edusecure.edusecure.dto.CreateAssignmentRequest;
import edusecure.edusecure.service.submission.AssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/assignments")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('LECTURER', 'ADMIN')")
    public ResponseEntity<AssignmentResponse> createAssignment(
            @Valid @RequestBody CreateAssignmentRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(assignmentService.createAssignment(authentication.getName(), request));
    }

    @GetMapping
    public ResponseEntity<List<AssignmentSummaryResponse>> listAssignments() {
        return ResponseEntity.ok(assignmentService.listAssignments());
    }
}

