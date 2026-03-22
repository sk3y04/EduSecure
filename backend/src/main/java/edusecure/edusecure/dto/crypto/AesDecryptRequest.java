package edusecure.edusecure.dto.crypto;

import jakarta.validation.constraints.NotBlank;

public record AesDecryptRequest(
        @NotBlank(message = "Nonce is required")
        String nonce,
        @NotBlank(message = "Ciphertext is required")
        String ciphertext
) {
}

