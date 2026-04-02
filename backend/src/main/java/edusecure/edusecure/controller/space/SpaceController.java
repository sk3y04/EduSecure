package edusecure.edusecure.controller.space;

import edusecure.edusecure.dto.space.AddSpaceStudentRequest;
import edusecure.edusecure.dto.space.CreateSpaceRequest;
import edusecure.edusecure.dto.space.SpaceDetailResponse;
import edusecure.edusecure.dto.space.SpaceStudentResponse;
import edusecure.edusecure.dto.space.SpaceSummaryResponse;
import edusecure.edusecure.dto.space.UpdateSpaceRequest;
import edusecure.edusecure.service.space.SpaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/spaces")
@RequiredArgsConstructor
public class SpaceController {

    private final SpaceService spaceService;

    @GetMapping
    public ResponseEntity<List<SpaceSummaryResponse>> listSpaces(Authentication authentication) {
        return ResponseEntity.ok(spaceService.listSpaces(authentication.getName()));
    }

    @GetMapping("/{spaceId}")
    public ResponseEntity<SpaceDetailResponse> getSpace(
            @PathVariable UUID spaceId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(spaceService.getSpace(authentication.getName(), spaceId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('LECTURER', 'ADMIN')")
    public ResponseEntity<SpaceDetailResponse> createSpace(
            @Valid @RequestBody CreateSpaceRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(spaceService.createSpace(authentication.getName(), request));
    }

    @PutMapping("/{spaceId}")
    @PreAuthorize("hasAnyRole('LECTURER', 'ADMIN')")
    public ResponseEntity<SpaceDetailResponse> updateSpace(
            @PathVariable UUID spaceId,
            @Valid @RequestBody UpdateSpaceRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(spaceService.updateSpace(authentication.getName(), spaceId, request));
    }

    @PostMapping("/{spaceId}/students")
    @PreAuthorize("hasAnyRole('LECTURER', 'ADMIN')")
    public ResponseEntity<SpaceStudentResponse> addStudentToSpace(
            @PathVariable UUID spaceId,
            @Valid @RequestBody AddSpaceStudentRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(spaceService.addStudentToSpace(authentication.getName(), spaceId, request));
    }

    @DeleteMapping("/{spaceId}/students/{studentUserId}")
    @PreAuthorize("hasAnyRole('LECTURER', 'ADMIN')")
    public ResponseEntity<Void> removeStudentFromSpace(
            @PathVariable UUID spaceId,
            @PathVariable UUID studentUserId,
            Authentication authentication
    ) {
        spaceService.removeStudentFromSpace(authentication.getName(), spaceId, studentUserId);
        return ResponseEntity.noContent().build();
    }
}