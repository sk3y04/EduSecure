package edusecure.edusecure.dto;

import edusecure.edusecure.entity.MfaMethod;

import java.util.List;

public record MfaEnableResponse(
        boolean mfaEnabled,
        MfaMethod mfaMethod,
        List<String> recoveryCodes
) {
}

