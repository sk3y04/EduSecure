package edusecure.edusecure.controller.grade;

import edusecure.edusecure.dto.grade.CreateGradeRequest;
import edusecure.edusecure.dto.grade.GradeResponse;
import edusecure.edusecure.dto.grade.MyGradeResponse;
import edusecure.edusecure.dto.grade.UpdateGradeRequest;
import edusecure.edusecure.service.grade.GradeService;
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

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping
public class GradeController {

    private final GradeService gradeService;

    @PostMapping("/api/submissions/{submissionId}/grade")
    @PreAuthorize("hasAnyRole('LECTURER', 'ADMIN')")
    public ResponseEntity<GradeResponse> createGrade(
            @PathVariable UUID submissionId,
            @Valid @RequestBody CreateGradeRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(gradeService.createGrade(authentication.getName(), submissionId, request));
    }

    @PutMapping("/api/grades/{gradeId}")
    @PreAuthorize("hasAnyRole('LECTURER', 'ADMIN')")
    public ResponseEntity<GradeResponse> updateGrade(
            @PathVariable UUID gradeId,
            @Valid @RequestBody UpdateGradeRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(gradeService.updateGrade(authentication.getName(), gradeId, request));
    }

    @GetMapping("/api/grades/{gradeId}")
    @PreAuthorize("hasAnyRole('LECTURER', 'ADMIN')")
    public ResponseEntity<GradeResponse> getGrade(
            @PathVariable UUID gradeId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(gradeService.getGrade(gradeId, authentication));
    }

    @GetMapping("/api/my/grades/{gradeId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<MyGradeResponse> getMyGrade(
            @PathVariable UUID gradeId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(gradeService.getMyGrade(gradeId, authentication));
    }
}

