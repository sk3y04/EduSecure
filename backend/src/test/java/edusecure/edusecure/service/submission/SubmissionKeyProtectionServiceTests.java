package edusecure.edusecure.service.submission;

import edusecure.edusecure.config.SubmissionStorageProperties;
import org.junit.jupiter.api.Test;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubmissionKeyProtectionServiceTests {

    @Test
    void wrapAndUnwrapRoundTripsContentEncryptionKey() throws Exception {
        SubmissionKeyProtectionService service = new SubmissionKeyProtectionService(properties());
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256);
        SecretKey contentEncryptionKey = keyGenerator.generateKey();

        SubmissionKeyProtectionService.WrappedKey wrappedKey = service.wrap(contentEncryptionKey);
        SecretKey unwrappedKey = service.unwrap(wrappedKey.wrappedKey(), wrappedKey.keyVersion());

        assertThat(wrappedKey.keyWrapAlgorithm()).isEqualTo("AESWrap");
        assertThat(wrappedKey.keyVersion()).isEqualTo("test-v1");
        assertThat(Base64.getEncoder().encodeToString(unwrappedKey.getEncoded()))
                .isEqualTo(Base64.getEncoder().encodeToString(contentEncryptionKey.getEncoded()));
    }

    @Test
    void wrongKeyVersionFailsUnwrap() throws Exception {
        SubmissionKeyProtectionService service = new SubmissionKeyProtectionService(properties());
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        SecretKey contentEncryptionKey = keyGenerator.generateKey();
        SubmissionKeyProtectionService.WrappedKey wrappedKey = service.wrap(contentEncryptionKey);

        assertThatThrownBy(() -> service.unwrap(wrappedKey.wrappedKey(), "wrong-version"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("key version");
    }

    @Test
    void malformedWrappedKeyFailsSafely() {
        SubmissionKeyProtectionService service = new SubmissionKeyProtectionService(properties());

        assertThatThrownBy(() -> service.unwrap("not-base64", "test-v1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("could not be restored");
    }

    private SubmissionStorageProperties properties() {
        SubmissionStorageProperties properties = new SubmissionStorageProperties();
        properties.setCipherAlgorithm("AES/GCM/NoPadding");
        properties.setBasePath("./build/test-submission-storage");
        properties.setKeyWrapAlgorithm("AESWrap");
        properties.setMasterKey("c3VibWlzc2lvbi1zdG9yYWdlLW1hc3Rlci1rZXktMDI=");
        properties.setMasterKeyVersion("test-v1");
        return properties;
    }
}

