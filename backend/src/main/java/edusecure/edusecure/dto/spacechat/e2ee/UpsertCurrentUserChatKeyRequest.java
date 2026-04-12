package edusecure.edusecure.dto.spacechat.e2ee;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpsertCurrentUserChatKeyRequest(
        @NotBlank(message = "Chat key algorithm is required")
        @Size(max = 32, message = "Chat key algorithm must not exceed 32 characters")
        String algorithm,

        @NotBlank(message = "Chat public key is required")
        @Size(max = 16000, message = "Chat public key payload is too large")
        String publicKeyJwk,

        @NotBlank(message = "Chat key fingerprint is required")
        @Size(max = 128, message = "Chat key fingerprint must not exceed 128 characters")
        String fingerprint
) {
}

