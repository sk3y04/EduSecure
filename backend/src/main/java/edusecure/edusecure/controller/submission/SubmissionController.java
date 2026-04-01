package edusecure.edusecure.controller.submission;

import edusecure.edusecure.dto.submission.SubmissionContentResponse;
import edusecure.edusecure.dto.submission.SubmissionResponse;
import edusecure.edusecure.service.submission.SubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping
public class SubmissionController {

    private final SubmissionService submissionService;

    @PostMapping("/api/assignments/{assignmentId}/submissions")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<SubmissionResponse> createSubmission(
            @PathVariable UUID assignmentId,
            @RequestPart("file") MultipartFile file,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(submissionService.createSubmission(authentication.getName(), assignmentId, file));
    }

    @GetMapping("/api/submissions/{submissionId}")
    public ResponseEntity<SubmissionResponse> getSubmission(
            @PathVariable UUID submissionId,
            Authentication authentication
    ) {
        SubmissionResponse response = submissionService.getSubmission(submissionId, authentication);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/submissions/{submissionId}/content")
    public ResponseEntity<SubmissionContentResponse> getSubmissionContent(
            @PathVariable UUID submissionId,
            Authentication authentication
    ) {
        SubmissionContentResponse response = submissionService.getSubmissionContent(submissionId, authentication);
        return ResponseEntity.ok(response);
    }
}

