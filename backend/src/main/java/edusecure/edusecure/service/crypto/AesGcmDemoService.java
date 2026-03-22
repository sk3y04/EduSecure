package edusecure.edusecure.service.crypto;

import edusecure.edusecure.dto.crypto.AesDecryptRequest;
import edusecure.edusecure.dto.crypto.AesDecryptResponse;
import edusecure.edusecure.dto.crypto.AesEncryptRequest;
import edusecure.edusecure.dto.crypto.AesEncryptResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class AesGcmDemoService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int NONCE_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final byte[] keyBytes;
    private final SecureRandom secureRandom = new SecureRandom();

    public AesGcmDemoService(@Value("${aes.demo-key}") String encodedKey) {
        this.keyBytes = Base64.getDecoder().decode(encodedKey);
        if (this.keyBytes.length != 16 && this.keyBytes.length != 24 && this.keyBytes.length != 32) {
            throw new IllegalStateException("AES demo key must be 128, 192, or 256 bits");
        }
    }

    public AesEncryptResponse encrypt(AesEncryptRequest request) {
        try {
            byte[] nonce = new byte[NONCE_LENGTH_BYTES];
            secureRandom.nextBytes(nonce);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            byte[] ciphertext = cipher.doFinal(request.plaintext().getBytes(StandardCharsets.UTF_8));

            return new AesEncryptResponse(
                    ALGORITHM,
                    Base64.getEncoder().encodeToString(nonce),
                    Base64.getEncoder().encodeToString(ciphertext)
            );
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Encryption failed");
        }
    }

    public AesDecryptResponse decrypt(AesDecryptRequest request) {
        try {
            byte[] nonce = Base64.getDecoder().decode(request.nonce());
            byte[] ciphertext = Base64.getDecoder().decode(request.ciphertext());

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            byte[] plaintext = cipher.doFinal(ciphertext);
            return new AesDecryptResponse(new String(plaintext, StandardCharsets.UTF_8));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nonce or ciphertext is not valid Base64");
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ciphertext could not be decrypted");
        }
    }
}

