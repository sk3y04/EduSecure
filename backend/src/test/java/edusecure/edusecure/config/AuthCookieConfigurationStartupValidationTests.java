package edusecure.edusecure.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

class AuthCookieConfigurationStartupValidationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
            .withUserConfiguration(TestConfig.class);

    @Test
    void prodProfileRejectsInsecureCookieConfigurationAtStartup() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "auth.cookie.secure=false"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(IllegalStateException.class)
                            .hasRootCauseMessage("The prod profile requires auth.cookie.secure=true");
                });
    }

    @Test
    void sameSiteNoneWithoutSecureRejectsContextStartup() {
        contextRunner
                .withPropertyValues(
                        "auth.cookie.same-site=None",
                        "auth.cookie.secure=false"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(IllegalStateException.class)
                            .hasRootCauseMessage("auth.cookie.same-site=None requires auth.cookie.secure=true");
                });
    }

    @Test
    void secureProductionCookieConfigurationStartsSuccessfully() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "auth.cookie.secure=true",
                        "auth.cookie.same-site=Lax"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(AuthCookieConfigurationValidator.class);
                    assertThat(context).hasSingleBean(AuthCookieProperties.class);
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(AuthCookieProperties.class)
    static class TestConfig {

        @Bean
        AuthCookieConfigurationValidator authCookieConfigurationValidator(
                AuthCookieProperties authCookieProperties,
                Environment environment
        ) {
            return new AuthCookieConfigurationValidator(authCookieProperties, environment);
        }
    }
}

