package edusecure.edusecure.controller.exam;

import edusecure.edusecure.dto.exam.CreateFeedbackFormRequest;
import edusecure.edusecure.dto.exam.FeedbackFormResponse;
import edusecure.edusecure.dto.exam.FeedbackFormReviewResponse;
import edusecure.edusecure.dto.exam.FeedbackFormSubmissionReceiptResponse;
import edusecure.edusecure.dto.exam.SubmitFeedbackFormRequest;
import edusecure.edusecure.dto.exam.UpdateFeedbackFormRequest;
import edusecure.edusecure.service.exam.FeedbackFormService;
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
public class FeedbackFormController {

    private final FeedbackFormService feedbackFormService;

    @PostMapping("/api/exams/{examId}/feedback-forms")
    @PreAuthorize("hasAnyRole('LECTURER', 'ADMIN')")
    public ResponseEntity<FeedbackFormResponse> createForm(
            @PathVariable UUID examId,
            @Valid @RequestBody CreateFeedbackFormRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(feedbackFormService.createForm(authentication.getName(), examId, request));
    }

    @GetMapping("/api/exams/{examId}/feedback-forms")
    public ResponseEntity<List<FeedbackFormResponse>> listFormsForExam(
            @PathVariable UUID examId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(feedbackFormService.listFormsForExam(examId, authentication.getName()));
    }

    @GetMapping("/api/feedback-forms/{formId}")
    public ResponseEntity<FeedbackFormResponse> getForm(
            @PathVariable UUID formId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(feedbackFormService.getForm(formId, authentication.getName()));
    }

    @PutMapping("/api/feedback-forms/{formId}")
    @PreAuthorize("hasAnyRole('LECTURER', 'ADMIN')")
    public ResponseEntity<FeedbackFormResponse> updateForm(
            @PathVariable UUID formId,
            @Valid @RequestBody UpdateFeedbackFormRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(feedbackFormService.updateForm(authentication.getName(), formId, request));
    }

    @PostMapping("/api/feedback-forms/{formId}/responses")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<FeedbackFormSubmissionReceiptResponse> submitResponse(
            @PathVariable UUID formId,
            @Valid @RequestBody SubmitFeedbackFormRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(feedbackFormService.submitResponse(formId, authentication.getName(), request));
    }

    @GetMapping("/api/feedback-forms/{formId}/responses")
    @PreAuthorize("hasAnyRole('LECTURER', 'ADMIN')")
    public ResponseEntity<FeedbackFormReviewResponse> getReview(
            @PathVariable UUID formId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(feedbackFormService.getReview(formId, authentication.getName()));
    }
}