package edusecure.edusecure.dto.space;

import java.util.UUID;

public record SpaceSummaryResponse(
        UUID id,
        String name,
        String code,
        String description,
        boolean archived,
        long memberCount,
        boolean canManage,
        boolean isMember
) {
}