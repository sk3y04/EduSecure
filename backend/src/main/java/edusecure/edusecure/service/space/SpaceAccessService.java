package edusecure.edusecure.service.space;

import edusecure.edusecure.entity.auth.RoleName;
import edusecure.edusecure.entity.auth.User;
import edusecure.edusecure.entity.space.Space;
import edusecure.edusecure.repository.auth.UserRepository;
import edusecure.edusecure.repository.space.SpaceMembershipRepository;
import edusecure.edusecure.repository.space.SpaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SpaceAccessService {

    private final SpaceRepository spaceRepository;
    private final SpaceMembershipRepository spaceMembershipRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public User findCurrentUserOrThrow(String currentUserEmail) {
        return userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    @Transactional(readOnly = true)
    public SpaceAccessContext requireReadableSpace(String currentUserEmail, UUID spaceId) {
        User currentUser = findCurrentUserOrThrow(currentUserEmail);
        Space space = getSpaceOrThrow(spaceId);
        boolean canManage = canManageSpace(currentUser, space);
        boolean isMember = spaceMembershipRepository.existsBySpaceIdAndStudentUserId(spaceId, currentUser.getId());

        if (!canManage && !isMember) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to view this space");
        }

        return new SpaceAccessContext(currentUser, space, canManage, isMember);
    }

    @Transactional(readOnly = true)
    public SpaceAccessContext requireManageableSpace(String currentUserEmail, UUID spaceId) {
        SpaceAccessContext accessContext = requireReadableSpace(currentUserEmail, spaceId);

        if (!accessContext.canManage()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to manage this space");
        }

        return accessContext;
    }

    @Transactional(readOnly = true)
    public SpaceAccessContext requireWritableChatSpace(String currentUserEmail, UUID spaceId) {
        SpaceAccessContext accessContext = requireReadableSpace(currentUserEmail, spaceId);

        if (accessContext.space().isArchived()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Archived spaces are read-only");
        }

        return accessContext;
    }

    @Transactional(readOnly = true)
    public Space getSpaceOrThrow(UUID spaceId) {
        return spaceRepository.findById(spaceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Space not found"));
    }

    public boolean canManageSpace(User currentUser, Space space) {
        return hasRole(currentUser, RoleName.ADMIN)
                || hasRole(currentUser, RoleName.LECTURER) && space.getCreatedByUserId().equals(currentUser.getId());
    }

    public boolean hasRole(User user, RoleName roleName) {
        return user.getRoles().stream().anyMatch(role -> role.getName() == roleName);
    }
}

