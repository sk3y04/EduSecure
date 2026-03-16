package edusecure.edusecure.service.submission;

import edusecure.edusecure.config.SubmissionStorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class SubmissionKeyProtectionService {

    private final SubmissionStorageProperties properties;

    public WrappedKey wrap(SecretKey contentEncryptionKey) {
        try {
            Cipher cipher = Cipher.getInstance(properties.getKeyWrapAlgorithm());
            cipher.init(Cipher.WRAP_MODE, masterKey());
            byte[] wrappedKey = cipher.wrap(contentEncryptionKey);
            return new WrappedKey(
                    Base64.getEncoder().encodeToString(wrappedKey),
                    properties.getKeyWrapAlgorithm(),
                    properties.getMasterKeyVersion()
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Submission content encryption key could not be protected", ex);
        }
    }

    public SecretKey unwrap(String wrappedKey, String keyVersion) {
        if (!properties.getMasterKeyVersion().equals(keyVersion)) {
            throw new IllegalStateException("Submission storage key version is not supported");
        }

        try {
            Cipher cipher = Cipher.getInstance(properties.getKeyWrapAlgorithm());
            cipher.init(Cipher.UNWRAP_MODE, masterKey());
            Key unwrappedKey = cipher.unwrap(Base64.getDecoder().decode(wrappedKey), "AES", Cipher.SECRET_KEY);
            return (SecretKey) unwrappedKey;
        } catch (Exception ex) {
            throw new IllegalStateException("Submission content encryption key could not be restored", ex);
        }
    }

    private SecretKey masterKey() {
        byte[] keyBytes = Base64.getDecoder().decode(properties.getMasterKey());
        if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
            throw new IllegalStateException("Submission storage master key must be 128, 192, or 256 bits");
        }
        return new SecretKeySpec(keyBytes, "AES");
    }

    public record WrappedKey(String wrappedKey, String keyWrapAlgorithm, String keyVersion) {
    }
}

