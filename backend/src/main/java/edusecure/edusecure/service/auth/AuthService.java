package edusecure.edusecure.service.auth;

import edusecure.edusecure.dto.AuthResponse;
import edusecure.edusecure.dto.CurrentUserResponse;
import edusecure.edusecure.dto.LoginRequest;
import edusecure.edusecure.dto.RegisterRequest;
import edusecure.edusecure.entity.Role;
import edusecure.edusecure.entity.RoleName;
import edusecure.edusecure.entity.User;
import edusecure.edusecure.repository.RoleRepository;
import edusecure.edusecure.repository.UserRepository;
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

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new AuthApiException(HttpStatus.CONFLICT, "Email is already registered");
        }

        Role studentRole = roleRepository.findByName(RoleName.STUDENT)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Default student role is missing"));

        User user = User.builder()
                .email(request.email().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName().trim())
                .roles(Set.of(studentRole))
                .build();

        User savedUser = userRepository.save(user);
        return authTokenService.issuePasswordAuthenticatedResponse(savedUser);
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
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return new CurrentUserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRoles().stream().map(role -> role.getName().name()).collect(Collectors.toSet())
        );
    }

}

