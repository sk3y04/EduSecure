package edusecure.edusecure.controller.registration;

import edusecure.edusecure.dto.registration.CreateSpaceRegistrationRequest;
import edusecure.edusecure.dto.registration.ReviewSpaceRegistrationRequest;
import edusecure.edusecure.dto.registration.ReviewSpaceRegistrationRequestResponse;
import edusecure.edusecure.dto.registration.StudentSpaceRegistrationRequestResponse;
import edusecure.edusecure.service.registration.SpaceRegistrationRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/space-registration-requests")
@RequiredArgsConstructor
public class SpaceRegistrationRequestController {

    private final SpaceRegistrationRequestService spaceRegistrationRequestService;

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<StudentSpaceRegistrationRequestResponse> createRequest(
            @Valid @RequestBody CreateSpaceRegistrationRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(spaceRegistrationRequestService.createRequest(authentication.getName(), request));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<StudentSpaceRegistrationRequestResponse>> listOwnRequests(Authentication authentication) {
        return ResponseEntity.ok(spaceRegistrationRequestService.listOwnRequests(authentication.getName()));
    }

    @PostMapping("/{requestId}/cancel")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<StudentSpaceRegistrationRequestResponse> cancelOwnRequest(
            @PathVariable UUID requestId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(spaceRegistrationRequestService.cancelOwnRequest(authentication.getName(), requestId));
    }

    @GetMapping("/review")
    @PreAuthorize("hasAnyRole('LECTURER', 'ADMIN')")
    public ResponseEntity<List<ReviewSpaceRegistrationRequestResponse>> listReviewQueue(Authentication authentication) {
        return ResponseEntity.ok(spaceRegistrationRequestService.listReviewQueue(authentication.getName()));
    }

    @PostMapping("/{requestId}/approve")
    @PreAuthorize("hasAnyRole('LECTURER', 'ADMIN')")
    public ResponseEntity<ReviewSpaceRegistrationRequestResponse> approveRequest(
            @PathVariable UUID requestId,
            @Valid @RequestBody ReviewSpaceRegistrationRequest reviewRequest,
            Authentication authentication
    ) {
        return ResponseEntity.ok(spaceRegistrationRequestService.approveRequest(
                authentication.getName(),
                requestId,
                reviewRequest
        ));
    }

    @PostMapping("/{requestId}/reject")
    @PreAuthorize("hasAnyRole('LECTURER', 'ADMIN')")
    public ResponseEntity<ReviewSpaceRegistrationRequestResponse> rejectRequest(
            @PathVariable UUID requestId,
            @Valid @RequestBody ReviewSpaceRegistrationRequest reviewRequest,
            Authentication authentication
    ) {
        return ResponseEntity.ok(spaceRegistrationRequestService.rejectRequest(
                authentication.getName(),
                requestId,
                reviewRequest
        ));
    }
}