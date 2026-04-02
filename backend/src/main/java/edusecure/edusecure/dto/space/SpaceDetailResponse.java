package edusecure.edusecure.dto.space;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SpaceDetailResponse(
        UUID id,
        String name,
        String code,
        String description,
        boolean archived,
        long memberCount,
        boolean canManage,
        boolean isMember,
        UUID createdByUserId,
        Instant createdAt,
        Instant updatedAt,
        List<SpaceStudentResponse> memberships
) {
}