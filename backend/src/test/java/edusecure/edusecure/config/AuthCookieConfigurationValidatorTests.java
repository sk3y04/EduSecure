package edusecure.edusecure.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthCookieConfigurationValidatorTests {

    @Test
    void allowsLocalDevelopmentDefaults() {
        AuthCookieProperties properties = new AuthCookieProperties();
        MockEnvironment environment = new MockEnvironment();

        AuthCookieConfigurationValidator validator = new AuthCookieConfigurationValidator(properties, environment);

        assertThatCode(validator::validate).doesNotThrowAnyException();
    }

    @Test
    void rejectsSameSiteNoneWithoutSecure() {
        AuthCookieProperties properties = new AuthCookieProperties();
        properties.setSameSite("None");
        properties.setSecure(false);

        AuthCookieConfigurationValidator validator = new AuthCookieConfigurationValidator(properties, new MockEnvironment());

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("auth.cookie.same-site=None requires auth.cookie.secure=true");
    }

    @Test
    void allowsSameSiteNoneWhenSecureIsEnabled() {
        AuthCookieProperties properties = new AuthCookieProperties();
        properties.setSameSite("None");
        properties.setSecure(true);

        AuthCookieConfigurationValidator validator = new AuthCookieConfigurationValidator(properties, new MockEnvironment());

        assertThatCode(validator::validate).doesNotThrowAnyException();
    }

    @Test
    void rejectsInsecureCookieInProdProfile() {
        AuthCookieProperties properties = new AuthCookieProperties();
        properties.setSecure(false);
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        AuthCookieConfigurationValidator validator = new AuthCookieConfigurationValidator(properties, environment);

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("The prod profile requires auth.cookie.secure=true");
    }

    @Test
    void rejectsCookiePathThatDoesNotStartWithSlash() {
        AuthCookieProperties properties = new AuthCookieProperties();
        properties.setPath("api");

        AuthCookieConfigurationValidator validator = new AuthCookieConfigurationValidator(properties, new MockEnvironment());

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("auth.cookie.path must start with '/'");
    }
}

