package edusecure.edusecure.service.space;

import edusecure.edusecure.entity.auth.User;
import edusecure.edusecure.entity.space.Space;

public record SpaceAccessContext(
        User currentUser,
        Space space,
        boolean canManage,
        boolean isMember
) {
}

