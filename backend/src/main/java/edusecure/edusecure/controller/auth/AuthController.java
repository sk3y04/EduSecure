package edusecure.edusecure.controller.auth;

import edusecure.edusecure.dto.auth.AuthResponse;
import edusecure.edusecure.dto.auth.CurrentUserResponse;
import edusecure.edusecure.dto.auth.LoginRequest;
import edusecure.edusecure.dto.auth.MfaDisableRequest;
import edusecure.edusecure.dto.auth.MfaEnableRequest;
import edusecure.edusecure.dto.auth.MfaEnableResponse;
import edusecure.edusecure.dto.auth.MfaSetupResponse;
import edusecure.edusecure.dto.auth.MfaStatusResponse;
import edusecure.edusecure.dto.auth.MfaVerifyRequest;
import edusecure.edusecure.dto.auth.RegisterRequest;
import edusecure.edusecure.service.auth.AuthService;
import edusecure.edusecure.service.auth.MfaService;
import edusecure.edusecure.security.AuthCookieService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final MfaService mfaService;
    private final AuthCookieService authCookieService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return buildAuthResponse(HttpStatus.CREATED, authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return buildAuthResponse(HttpStatus.OK, authService.login(request));
    }

    @GetMapping("/mfa/status")
    public ResponseEntity<MfaStatusResponse> mfaStatus(Authentication authentication) {
        return ResponseEntity.ok(mfaService.status(authentication.getName()));
    }

    @PostMapping("/mfa/setup")
    public ResponseEntity<MfaSetupResponse> setupMfa(Authentication authentication) {
        return ResponseEntity.ok(mfaService.setup(authentication.getName()));
    }

    @PostMapping("/mfa/enable")
    public ResponseEntity<MfaEnableResponse> enableMfa(
            Authentication authentication,
            @Valid @RequestBody MfaEnableRequest request
    ) {
        return ResponseEntity.ok(mfaService.enable(authentication.getName(), request));
    }

    @PostMapping("/mfa/verify")
    public ResponseEntity<AuthResponse> verifyMfa(@Valid @RequestBody MfaVerifyRequest request) {
        return buildAuthResponse(HttpStatus.OK, mfaService.verify(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, authCookieService.clearAuthenticationCookie().toString())
                .build();
    }

    @PostMapping("/mfa/disable")
    public ResponseEntity<Void> disableMfa(
            Authentication authentication,
            @Valid @RequestBody MfaDisableRequest request
    ) {
        mfaService.disable(authentication.getName(), request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> me(Authentication authentication) {
        return ResponseEntity.ok(authService.currentUser(authentication.getName()));
    }

    private ResponseEntity<AuthResponse> buildAuthResponse(HttpStatus status, AuthResponse response) {
        ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.status(status);

        if (response.token() != null && !response.token().isBlank()) {
            responseBuilder.header(HttpHeaders.SET_COOKIE, authCookieService.createAuthenticationCookie(response.token()).toString());
        } else {
            responseBuilder.header(HttpHeaders.SET_COOKIE, authCookieService.clearAuthenticationCookie().toString());
        }

        return responseBuilder.body(response.withoutToken());
    }
}

