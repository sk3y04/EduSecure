package edusecure.edusecure.security;

import edusecure.edusecure.config.AuthCookieProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class AuthCookieService {

    private final AuthCookieProperties authCookieProperties;

    @Value("${jwt.expiration}")
    private long jwtExpirationMillis;

    public String getCookieName() {
        return authCookieProperties.getName();
    }

    public ResponseCookie createAuthenticationCookie(String token) {
        return baseCookieBuilder(token)
                .maxAge(Duration.ofMillis(jwtExpirationMillis))
                .build();
    }

    public ResponseCookie clearAuthenticationCookie() {
        return baseCookieBuilder("")
                .maxAge(Duration.ZERO)
                .build();
    }

    public String extractToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length == 0) {
            return null;
        }

        return Arrays.stream(cookies)
                .filter(cookie -> getCookieName().equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(null);
    }

    private ResponseCookie.ResponseCookieBuilder baseCookieBuilder(String value) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(getCookieName(), value)
                .httpOnly(true)
                .secure(authCookieProperties.isSecure())
                .path(authCookieProperties.getPath());

        String domain = authCookieProperties.getDomain();
        if (domain != null && !domain.isBlank()) {
            builder.domain(domain);
        }

        String sameSite = authCookieProperties.getSameSite();
        if (sameSite != null && !sameSite.isBlank()) {
            builder.sameSite(sameSite);
        }

        return builder;
    }
}

