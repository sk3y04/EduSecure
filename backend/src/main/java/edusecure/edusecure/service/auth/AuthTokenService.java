package edusecure.edusecure.service.auth;

import edusecure.edusecure.dto.AuthResponse;
import edusecure.edusecure.dto.AuthStatus;
import edusecure.edusecure.entity.User;
import edusecure.edusecure.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthTokenService {

    private final JwtService jwtService;

    public AuthResponse issuePasswordAuthenticatedResponse(User user) {
        return issueAuthenticatedResponse(user, List.of("pwd"), false);
    }

    public AuthResponse issueMfaAuthenticatedResponse(User user) {
        return issueAuthenticatedResponse(user, List.of("pwd", "otp"), true);
    }

    private AuthResponse issueAuthenticatedResponse(User user, List<String> amr, boolean mfaSatisfied) {
        String token = jwtService.generateToken(
                Map.of("mfa", mfaSatisfied, "amr", amr),
                toUserDetails(user)
        );

        return new AuthResponse(
                AuthStatus.AUTHENTICATED,
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRoles().stream().map(role -> role.getName().name()).collect(Collectors.toSet()),
                token,
                user.isMfaEnabled(),
                amr,
                null,
                null,
                null,
                null
        );
    }

    private UserDetails toUserDetails(User user) {
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(user.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName().name()))
                        .collect(Collectors.toSet()))
                .build();
    }
}

