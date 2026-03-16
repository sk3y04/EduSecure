package edusecure.edusecure.service.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class MfaSecretCryptoService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int NONCE_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final byte[] keyBytes;
    private final String keyVersion;
    private final SecureRandom secureRandom = new SecureRandom();

    public MfaSecretCryptoService(
            @Value("${mfa.secret-encryption-key}") String encodedKey,
            @Value("${mfa.secret-key-version:v1}") String keyVersion
    ) {
        this.keyBytes = Base64.getDecoder().decode(encodedKey);
        if (this.keyBytes.length != 16 && this.keyBytes.length != 24 && this.keyBytes.length != 32) {
            throw new IllegalStateException("MFA secret encryption key must be 128, 192, or 256 bits");
        }
        this.keyVersion = keyVersion;
    }

    public EncryptedSecret encrypt(byte[] plaintext) {
        try {
            byte[] nonce = new byte[NONCE_LENGTH_BYTES];
            secureRandom.nextBytes(nonce);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            byte[] ciphertext = cipher.doFinal(plaintext);

            return new EncryptedSecret(
                    Base64.getEncoder().encodeToString(ciphertext),
                    Base64.getEncoder().encodeToString(nonce),
                    keyVersion
            );
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "MFA secret encryption failed");
        }
    }

    public byte[] decrypt(String nonce, String ciphertext) {
        try {
            byte[] nonceBytes = Base64.getDecoder().decode(nonce);
            byte[] ciphertextBytes = Base64.getDecoder().decode(ciphertext);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new GCMParameterSpec(TAG_LENGTH_BITS, nonceBytes));
            return cipher.doFinal(ciphertextBytes);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Stored MFA secret could not be decrypted");
        }
    }

    public record EncryptedSecret(String ciphertext, String nonce, String keyVersion) {
    }
}

