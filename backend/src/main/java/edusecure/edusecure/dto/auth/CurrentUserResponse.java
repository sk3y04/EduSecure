package edusecure.edusecure.dto.auth;

import java.util.Set;
import java.util.UUID;

public record CurrentUserResponse(
        UUID userId,
        String email,
        String fullName,
        Set<String> roles
) {
}

