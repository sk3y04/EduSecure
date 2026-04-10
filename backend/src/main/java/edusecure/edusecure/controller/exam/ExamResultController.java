package edusecure.edusecure.controller.exam;

import edusecure.edusecure.dto.exam.CreateExamResultRequest;
import edusecure.edusecure.dto.exam.ExamResultResponse;
import edusecure.edusecure.dto.exam.MyExamResultResponse;
import edusecure.edusecure.dto.exam.UpdateExamResultRequest;
import edusecure.edusecure.service.exam.ExamResultService;
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
@RequiredArgsConstructor
@RequestMapping
public class ExamResultController {

    private final ExamResultService examResultService;

    @PostMapping("/api/exams/{examId}/results")
    @PreAuthorize("hasAnyRole('LECTURER', 'ADMIN')")
    public ResponseEntity<ExamResultResponse> createExamResult(
            @PathVariable UUID examId,
            @Valid @RequestBody CreateExamResultRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(examResultService.createExamResult(authentication.getName(), examId, request, authentication));
    }

    @GetMapping("/api/exams/{examId}/results")
    @PreAuthorize("hasAnyRole('LECTURER', 'ADMIN')")
    public ResponseEntity<List<ExamResultResponse>> listExamResults(
            @PathVariable UUID examId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(examResultService.listExamResults(examId, authentication));
    }

    @PutMapping("/api/exam-results/{examResultId}")
    @PreAuthorize("hasAnyRole('LECTURER', 'ADMIN')")
    public ResponseEntity<ExamResultResponse> updateExamResult(
            @PathVariable UUID examResultId,
            @Valid @RequestBody UpdateExamResultRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(examResultService.updateExamResult(authentication.getName(), examResultId, request, authentication));
    }

    @GetMapping("/api/exam-results/{examResultId}")
    @PreAuthorize("hasAnyRole('LECTURER', 'ADMIN')")
    public ResponseEntity<ExamResultResponse> getExamResult(
            @PathVariable UUID examResultId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(examResultService.getExamResult(examResultId, authentication));
    }

    @GetMapping("/api/my/exam-results")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<MyExamResultResponse>> listMyExamResults(Authentication authentication) {
        return ResponseEntity.ok(examResultService.listMyExamResults(authentication));
    }

    @GetMapping("/api/my/exams/{examId}/result")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<MyExamResultResponse> getMyExamResultForExam(
            @PathVariable UUID examId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(examResultService.getMyExamResultForExam(examId, authentication));
    }
}