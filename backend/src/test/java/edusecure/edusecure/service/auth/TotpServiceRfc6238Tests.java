package edusecure.edusecure.service.auth;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class TotpServiceRfc6238Tests {

    private static final byte[] RFC_SHA1_SECRET = "12345678901234567890".getBytes(StandardCharsets.US_ASCII);
    private static final String RFC_SHA1_SECRET_BASE32 = "gezdgnbvgy3tqojqgezdgnbvgy3tqojq====";

    private final TotpProvider eightDigitTotp = new TotpService("EduSecure", 30L, 0, 8);
    private final TotpProvider sixDigitWindowedTotp = new TotpService("EduSecure", 30L, 1, 6);

    @Test
    void generatesRfc6238Sha1Vectors() {
        assertThat(eightDigitTotp.generateCodeAt(RFC_SHA1_SECRET, Instant.ofEpochSecond(59))).isEqualTo("94287082");
        assertThat(eightDigitTotp.generateCodeAt(RFC_SHA1_SECRET, Instant.ofEpochSecond(1111111109L))).isEqualTo("07081804");
        assertThat(eightDigitTotp.generateCodeAt(RFC_SHA1_SECRET, Instant.ofEpochSecond(1111111111L))).isEqualTo("14050471");
        assertThat(eightDigitTotp.generateCodeAt(RFC_SHA1_SECRET, Instant.ofEpochSecond(1234567890L))).isEqualTo("89005924");
        assertThat(eightDigitTotp.generateCodeAt(RFC_SHA1_SECRET, Instant.ofEpochSecond(2000000000L))).isEqualTo("69279037");
        assertThat(eightDigitTotp.generateCodeAt(RFC_SHA1_SECRET, Instant.ofEpochSecond(20000000000L))).isEqualTo("65353130");
    }

    @Test
    void base32InputIsCaseInsensitiveAndPaddingTolerant() {
        assertThat(eightDigitTotp.generateCodeAtFromBase32(RFC_SHA1_SECRET_BASE32, Instant.ofEpochSecond(59)))
                .isEqualTo("94287082");
    }

    @Test
    void validationHonorsConfiguredDriftWindow() {
        Instant currentStep = Instant.ofEpochSecond(90);
        String previousStepCode = sixDigitWindowedTotp.generateCodeAt(RFC_SHA1_SECRET, Instant.ofEpochSecond(60));
        String twoStepsBackCode = sixDigitWindowedTotp.generateCodeAt(RFC_SHA1_SECRET, Instant.ofEpochSecond(30));

        assertThat(sixDigitWindowedTotp.isValidCodeAt(RFC_SHA1_SECRET, previousStepCode, currentStep)).isTrue();
        assertThat(sixDigitWindowedTotp.isValidCodeAt(RFC_SHA1_SECRET, twoStepsBackCode, currentStep)).isFalse();
    }
}
