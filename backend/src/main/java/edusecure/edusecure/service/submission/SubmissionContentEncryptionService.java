package edusecure.edusecure.service.submission;

import edusecure.edusecure.config.SubmissionStorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.security.SecureRandom;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class SubmissionContentEncryptionService {

    private static final int NONCE_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final int CONTENT_KEY_LENGTH_BITS = 256;

    private final SubmissionStorageProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public EncryptedSubmissionContent encrypt(byte[] plaintext) {
        try {
            SecretKey contentEncryptionKey = generateDataEncryptionKey();
            byte[] nonce = new byte[NONCE_LENGTH_BYTES];
            secureRandom.nextBytes(nonce);

            Cipher cipher = Cipher.getInstance(properties.getCipherAlgorithm());
            cipher.init(Cipher.ENCRYPT_MODE, contentEncryptionKey, new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            byte[] ciphertext = cipher.doFinal(plaintext);

            return new EncryptedSubmissionContent(
                    ciphertext,
                    Base64.getEncoder().encodeToString(nonce),
                    properties.getCipherAlgorithm(),
                    ciphertext.length,
                    contentEncryptionKey
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Submission content could not be encrypted", ex);
        }
    }

    public byte[] decrypt(byte[] ciphertext, String encodedNonce, SecretKey contentEncryptionKey) {
        try {
            byte[] nonce = Base64.getDecoder().decode(encodedNonce);
            Cipher cipher = Cipher.getInstance(properties.getCipherAlgorithm());
            cipher.init(Cipher.DECRYPT_MODE, contentEncryptionKey, new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            return cipher.doFinal(ciphertext);
        } catch (Exception ex) {
            throw new IllegalStateException("Submission ciphertext could not be decrypted", ex);
        }
    }

    private SecretKey generateDataEncryptionKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(CONTENT_KEY_LENGTH_BITS, secureRandom);
            return keyGenerator.generateKey();
        } catch (Exception ex) {
            throw new IllegalStateException("Submission content encryption key could not be generated", ex);
        }
    }

    public record EncryptedSubmissionContent(
            byte[] ciphertext,
            String nonce,
            String encryptionAlgorithm,
            long ciphertextLengthBytes,
            SecretKey contentEncryptionKey
    ) {
    }
}

