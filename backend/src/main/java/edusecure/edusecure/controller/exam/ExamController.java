package edusecure.edusecure.controller.exam;

import edusecure.edusecure.dto.exam.CreateExamRequest;
import edusecure.edusecure.dto.exam.ExamResponse;
import edusecure.edusecure.dto.exam.UpdateExamRequest;
import edusecure.edusecure.service.exam.ExamService;
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
@RequestMapping("/api/exams")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;

    @GetMapping
    public ResponseEntity<List<ExamResponse>> listExams(Authentication authentication) {
        return ResponseEntity.ok(examService.listExams(authentication.getName()));
    }

    @GetMapping("/{examId}")
    public ResponseEntity<ExamResponse> getExam(@PathVariable UUID examId, Authentication authentication) {
        return ResponseEntity.ok(examService.getExam(authentication.getName(), examId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('LECTURER', 'ADMIN')")
    public ResponseEntity<ExamResponse> createExam(
            @Valid @RequestBody CreateExamRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(examService.createExam(authentication.getName(), request));
    }

    @PutMapping("/{examId}")
    @PreAuthorize("hasAnyRole('LECTURER', 'ADMIN')")
    public ResponseEntity<ExamResponse> updateExam(
            @PathVariable UUID examId,
            @Valid @RequestBody UpdateExamRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(examService.updateExam(authentication.getName(), examId, request));
    }
}