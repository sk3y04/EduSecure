package edusecure.edusecure.dto;

import edusecure.edusecure.entity.MfaMethod;

public record MfaSetupResponse(
        MfaMethod mfaMethod,
        String manualEntryKey,
        String otpauthUri
) {
}

