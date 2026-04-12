package edusecure.edusecure.dto.spacechat.e2ee;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record PublishSpaceChatKeyVersionRequest(
        @Min(value = 1, message = "Chat key version must be at least 1")
        int keyVersion,

        @NotBlank(message = "Rotation reason is required")
        @Size(max = 64, message = "Rotation reason must not exceed 64 characters")
        String rotationReason,

        @NotEmpty(message = "At least one wrapped chat key recipient is required")
        List<@Valid Recipient> recipients
) {
    public record Recipient(
            @NotNull(message = "Recipient user id is required")
            UUID recipientUserId,

            @NotBlank(message = "Wrap algorithm is required")
            @Size(max = 64, message = "Wrap algorithm must not exceed 64 characters")
            String wrapAlgorithm,

            @NotBlank(message = "Wrap nonce is required")
            @Size(max = 255, message = "Wrap nonce must not exceed 255 characters")
            String wrapNonce,

            @NotBlank(message = "Wrapped key ciphertext is required")
            @Size(max = 16000, message = "Wrapped key ciphertext payload is too large")
            String wrappedKeyCiphertext
    ) {
    }
}

