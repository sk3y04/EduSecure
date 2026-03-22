package edusecure.edusecure.dto.crypto;

import jakarta.validation.constraints.NotBlank;

public record AesEncryptRequest(
        @NotBlank(message = "Plaintext is required")
        String plaintext
) {
}

