package edusecure.edusecure.dto.auth;

import edusecure.edusecure.entity.auth.MfaMethod;

import java.util.List;

public record MfaEnableResponse(
        boolean mfaEnabled,
        MfaMethod mfaMethod,
        List<String> recoveryCodes
) {
}

