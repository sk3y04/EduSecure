package edusecure.edusecure.controller.space;

import edusecure.edusecure.dto.spacechat.CreateSpaceChatMessageRequest;
import edusecure.edusecure.dto.spacechat.SpaceChatMessagePageResponse;
import edusecure.edusecure.dto.spacechat.SpaceChatMessageResponse;
import edusecure.edusecure.service.spacechat.SpaceChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/spaces/{spaceId}/chat")
@RequiredArgsConstructor
public class SpaceChatController {

    private final SpaceChatService spaceChatService;

    @GetMapping("/messages")
    public ResponseEntity<SpaceChatMessagePageResponse> listMessages(
            @PathVariable UUID spaceId,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String beforeCreatedAt,
            @RequestParam(required = false) String beforeMessageId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(spaceChatService.listMessages(
                authentication.getName(),
                spaceId,
                limit,
                beforeCreatedAt,
                beforeMessageId
        ));
    }

    @PostMapping("/messages")
    public ResponseEntity<SpaceChatMessageResponse> createMessage(
            @PathVariable UUID spaceId,
            @Valid @RequestBody CreateSpaceChatMessageRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(spaceChatService.createMessage(authentication.getName(), spaceId, request));
    }
}

