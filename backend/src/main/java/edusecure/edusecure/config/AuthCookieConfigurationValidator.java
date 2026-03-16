package edusecure.edusecure.config;

import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class AuthCookieConfigurationValidator {

    private final AuthCookieProperties authCookieProperties;
    private final Environment environment;

    @jakarta.annotation.PostConstruct
    void validate() {
        String cookieName = trimToNull(authCookieProperties.getName());
        String path = trimToNull(authCookieProperties.getPath());
        String sameSite = trimToNull(authCookieProperties.getSameSite());

        if (cookieName == null) {
            throw new IllegalStateException("auth.cookie.name must not be blank");
        }

        if (path == null || !path.startsWith("/")) {
            throw new IllegalStateException("auth.cookie.path must start with '/'");
        }

        if (sameSite == null) {
            throw new IllegalStateException("auth.cookie.same-site must not be blank");
        }

        if ("none".equalsIgnoreCase(sameSite) && !authCookieProperties.isSecure()) {
            throw new IllegalStateException("auth.cookie.same-site=None requires auth.cookie.secure=true");
        }

        if (isProdProfileActive() && !authCookieProperties.isSecure()) {
            throw new IllegalStateException("The prod profile requires auth.cookie.secure=true");
        }
    }

    private boolean isProdProfileActive() {
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch("prod"::equalsIgnoreCase);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

