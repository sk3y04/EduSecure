package edusecure.edusecure.dto.auth;

import edusecure.edusecure.entity.auth.MfaMethod;

public record MfaSetupResponse(
        MfaMethod mfaMethod,
        String manualEntryKey,
        String otpauthUri
) {
}

