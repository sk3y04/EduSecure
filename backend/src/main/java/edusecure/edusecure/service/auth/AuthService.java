package edusecure.edusecure.service.auth;

import edusecure.edusecure.audit.AuditService;
import edusecure.edusecure.dto.auth.AuthResponse;
import edusecure.edusecure.dto.auth.CreateManagedUserRequest;
import edusecure.edusecure.dto.auth.CurrentUserResponse;
import edusecure.edusecure.dto.auth.LoginRequest;
import edusecure.edusecure.dto.auth.RegisterRequest;
import edusecure.edusecure.entity.audit.AuditActionType;
import edusecure.edusecure.entity.auth.Role;
import edusecure.edusecure.entity.auth.RoleName;
import edusecure.edusecure.entity.auth.User;
import edusecure.edusecure.repository.spacechatkey.UserChatKeyRepository;
import edusecure.edusecure.config.chat.SpaceChatProperties;
import edusecure.edusecure.repository.auth.RoleRepository;
import edusecure.edusecure.repository.auth.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final AuthTokenService authTokenService;
    private final MfaService mfaService;
    private final AuditService auditService;
    private final UserChatKeyRepository userChatKeyRepository;
    private final SpaceChatProperties spaceChatProperties;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        User savedUser = createUserAccount(request.email(), request.password(), request.fullName(), RoleName.STUDENT);
        return authTokenService.issuePasswordAuthenticatedResponse(savedUser);
    }

        @Transactional
        public CurrentUserResponse createManagedUser(String creatorEmail, CreateManagedUserRequest request) {
        User creator = userRepository.findByEmail(normalizeEmail(creatorEmail))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        RoleName requestedRole = request.role();
        validateManagedRoleAssignment(creator, requestedRole);

        User savedUser = createUserAccount(request.email(), request.password(), request.fullName(), requestedRole);
        auditService.record(
            AuditActionType.USER_CREATED,
            creator.getId(),
            User.class.getSimpleName(),
            savedUser.getId(),
            "email=" + savedUser.getEmail() + ",role=" + requestedRole.name()
        );

        return new CurrentUserResponse(
            savedUser.getId(),
            savedUser.getEmail(),
            savedUser.getFullName(),
            savedUser.getRoles().stream().map(role -> role.getName().name()).collect(Collectors.toSet()),
            spaceChatProperties.getE2ee().isEnabled(),
            false
        );
        }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email().trim().toLowerCase(), request.password())
            );
        } catch (AuthenticationException ex) {
            throw new AuthApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        User user = userRepository.findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(() -> new AuthApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (user.isMfaEnabled()) {
            return mfaService.createLoginChallenge(user);
        }

        return authTokenService.issuePasswordAuthenticatedResponse(user);
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse currentUser(String email) {
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return new CurrentUserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRoles().stream().map(role -> role.getName().name()).collect(Collectors.toSet()),
                spaceChatProperties.getE2ee().isEnabled(),
                userChatKeyRepository.findFirstByUserIdAndRevokedAtIsNull(user.getId()).isPresent()
        );
    }

    private User createUserAccount(String email, String password, String fullName, RoleName roleName) {
        String normalizedEmail = normalizeEmail(email);
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new AuthApiException(HttpStatus.CONFLICT, "Email is already registered");
        }

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Required role is missing: " + roleName.name()));

        User user = User.builder()
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(password))
                .fullName(fullName.trim())
                .roles(Set.of(role))
                .build();

        return userRepository.save(user);
    }

    private void validateManagedRoleAssignment(User creator, RoleName requestedRole) {
        if (requestedRole == RoleName.ADMIN) {
            throw new AuthApiException(HttpStatus.FORBIDDEN, "Admin accounts cannot be created through this endpoint");
        }

        if (hasRole(creator, RoleName.ADMIN)) {
            return;
        }

        if (hasRole(creator, RoleName.LECTURER) && requestedRole == RoleName.STUDENT) {
            return;
        }

        throw new AuthApiException(HttpStatus.FORBIDDEN, "You are not allowed to create an account with that role");
    }

    private boolean hasRole(User user, RoleName roleName) {
        return user.getRoles().stream().anyMatch(role -> role.getName() == roleName);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

}

