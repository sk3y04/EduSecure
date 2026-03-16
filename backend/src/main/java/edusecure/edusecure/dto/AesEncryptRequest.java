package edusecure.edusecure.dto;

import jakarta.validation.constraints.NotBlank;

public record AesEncryptRequest(
        @NotBlank(message = "Plaintext is required")
        String plaintext
) {
}

