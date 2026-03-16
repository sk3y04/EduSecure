package edusecure.edusecure.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MfaVerifyRequest(
        @NotNull(message = "Challenge ID is required")
        UUID challengeId,
        @NotBlank(message = "Verification code is required")
        String verificationCode
) {
}

