package edusecure.edusecure.dto.spacechat;

import jakarta.validation.constraints.NotBlank;

public record CreateSpaceChatMessageRequest(
        @NotBlank(message = "Message body is required")
        String body
) {
}

