package edusecure.edusecure.controller.space;

import edusecure.edusecure.dto.spacechat.e2ee.CurrentUserChatKeyResponse;
import edusecure.edusecure.dto.spacechat.e2ee.PublishSpaceChatKeyVersionRequest;
import edusecure.edusecure.dto.spacechat.e2ee.SpaceChatE2eeRecipientResponse;
import edusecure.edusecure.dto.spacechat.e2ee.SpaceChatE2eeStateResponse;
import edusecure.edusecure.dto.spacechat.e2ee.SpaceChatKeyVersionPublishResponse;
import edusecure.edusecure.dto.spacechat.e2ee.UpsertCurrentUserChatKeyRequest;
import edusecure.edusecure.service.spacechatkey.SpaceChatE2eeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api")
@RequiredArgsConstructor
public class SpaceChatE2eeController {

    private final SpaceChatE2eeService spaceChatE2eeService;

    @GetMapping("/chat/e2ee/me")
    public ResponseEntity<CurrentUserChatKeyResponse> currentUserKey(Authentication authentication) {
        return ResponseEntity.ok(spaceChatE2eeService.getCurrentUserKey(authentication.getName()));
    }

    @PutMapping("/chat/e2ee/me")
    public ResponseEntity<CurrentUserChatKeyResponse> upsertCurrentUserKey(
            Authentication authentication,
            @Valid @RequestBody UpsertCurrentUserChatKeyRequest request
    ) {
        return ResponseEntity.ok(spaceChatE2eeService.upsertCurrentUserKey(authentication.getName(), request));
    }

    @GetMapping("/spaces/{spaceId}/chat/e2ee/state")
    public ResponseEntity<SpaceChatE2eeStateResponse> spaceState(
            @PathVariable UUID spaceId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(spaceChatE2eeService.getSpaceState(authentication.getName(), spaceId));
    }

    @GetMapping("/spaces/{spaceId}/chat/e2ee/recipients")
    public ResponseEntity<List<SpaceChatE2eeRecipientResponse>> recipients(
            @PathVariable UUID spaceId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(spaceChatE2eeService.listRecipients(authentication.getName(), spaceId));
    }

    @PostMapping("/spaces/{spaceId}/chat/e2ee/key-versions")
    public ResponseEntity<SpaceChatKeyVersionPublishResponse> publishKeyVersion(
            @PathVariable UUID spaceId,
            Authentication authentication,
            @Valid @RequestBody PublishSpaceChatKeyVersionRequest request
    ) {
        return ResponseEntity.ok(spaceChatE2eeService.publishKeyVersion(authentication.getName(), spaceId, request));
    }
}

