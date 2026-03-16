package edusecure.edusecure.dto;

import jakarta.validation.constraints.NotBlank;

public record MfaEnableRequest(
        @NotBlank(message = "Verification code is required")
        String verificationCode
) {
}

