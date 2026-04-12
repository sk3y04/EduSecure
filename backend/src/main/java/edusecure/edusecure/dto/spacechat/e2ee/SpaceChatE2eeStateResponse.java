package edusecure.edusecure.dto.spacechat.e2ee;

public record SpaceChatE2eeStateResponse(
        boolean e2eeEnabled,
        boolean keyRegistrationRequired,
        boolean currentUserKeyRegistered,
        Integer activeKeyVersion,
        boolean requiresRekey,
        SpaceChatWrappedKeyResponse currentUserWrappedKey
) {
}

