package edusecure.edusecure.service.submission;

import edusecure.edusecure.config.SubmissionStorageProperties;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubmissionContentEncryptionServiceTests {

    @Test
    void encryptAndDecryptRoundTripsPlaintext() {
        SubmissionContentEncryptionService service = new SubmissionContentEncryptionService(properties());
        byte[] plaintext = "Sensitive coursework content".getBytes(StandardCharsets.UTF_8);

        SubmissionContentEncryptionService.EncryptedSubmissionContent encrypted = service.encrypt(plaintext);
        byte[] decrypted = service.decrypt(encrypted.ciphertext(), encrypted.nonce(), encrypted.contentEncryptionKey());

        assertThat(encrypted.encryptionAlgorithm()).isEqualTo("AES/GCM/NoPadding");
        assertThat(encrypted.nonce()).isNotBlank();
        assertThat(encrypted.ciphertextLengthBytes()).isPositive();
        assertThat(encrypted.ciphertext()).isNotEqualTo(plaintext);
        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    void tamperedCiphertextFailsDecryption() {
        SubmissionContentEncryptionService service = new SubmissionContentEncryptionService(properties());
        byte[] plaintext = "Tamper-resistant ciphertext".getBytes(StandardCharsets.UTF_8);

        SubmissionContentEncryptionService.EncryptedSubmissionContent encrypted = service.encrypt(plaintext);
        byte[] tamperedCiphertext = encrypted.ciphertext().clone();
        tamperedCiphertext[0] = (byte) (tamperedCiphertext[0] ^ 0x01);

        assertThatThrownBy(() -> service.decrypt(tamperedCiphertext, encrypted.nonce(), encrypted.contentEncryptionKey()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("could not be decrypted");
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

