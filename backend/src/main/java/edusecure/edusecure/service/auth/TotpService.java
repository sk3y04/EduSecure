package edusecure.edusecure.service.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;

@Service
public class TotpService {

    private static final String HMAC_ALGORITHM = "HmacSHA1";
    private static final int SECRET_LENGTH_BYTES = 20;
    private static final int CODE_DIGITS = 6;
    private static final char[] BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();

    private final SecureRandom secureRandom = new SecureRandom();
    private final String issuer;
    private final long timeStepSeconds;
    private final int allowedWindow;

    public TotpService(
            @Value("${mfa.issuer:EduSecure}") String issuer,
            @Value("${mfa.totp-time-step-seconds:30}") long timeStepSeconds,
            @Value("${mfa.totp-allowed-window:1}") int allowedWindow
    ) {
        this.issuer = issuer;
        this.timeStepSeconds = timeStepSeconds;
        this.allowedWindow = allowedWindow;
    }

    public GeneratedSecret generateSecret(String accountName) {
        byte[] secret = new byte[SECRET_LENGTH_BYTES];
        secureRandom.nextBytes(secret);

        String manualEntryKey = encodeBase32(secret);
        String label = URLEncoder.encode(issuer + ":" + accountName, StandardCharsets.UTF_8);
        String encodedIssuer = URLEncoder.encode(issuer, StandardCharsets.UTF_8);
        String otpauthUri = "otpauth://totp/" + label
                + "?secret=" + manualEntryKey
                + "&issuer=" + encodedIssuer
                + "&digits=" + CODE_DIGITS
                + "&period=" + timeStepSeconds;

        return new GeneratedSecret(secret, manualEntryKey, otpauthUri);
    }

    public boolean looksLikeTotpCode(String verificationCode) {
        return normalizeVerificationCode(verificationCode).matches("\\d{" + CODE_DIGITS + "}");
    }

    public boolean isValidCode(byte[] secret, String verificationCode) {
        String normalizedCode = normalizeVerificationCode(verificationCode);
        if (!looksLikeTotpCode(normalizedCode)) {
            return false;
        }

        long currentCounter = Instant.now().getEpochSecond() / timeStepSeconds;
        for (int offset = -allowedWindow; offset <= allowedWindow; offset++) {
            if (generateCode(secret, currentCounter + offset).equals(normalizedCode)) {
                return true;
            }
        }
        return false;
    }

    public String generateCurrentCode(byte[] secret) {
        long currentCounter = Instant.now().getEpochSecond() / timeStepSeconds;
        return generateCode(secret, currentCounter);
    }

    public String generateCurrentCodeFromBase32(String manualEntryKey) {
        return generateCurrentCode(decodeBase32(manualEntryKey));
    }

    private String generateCode(byte[] secret, long counter) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            byte[] hash = mac.doFinal(ByteBuffer.allocate(Long.BYTES).putLong(counter).array());

            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);

            int otp = binary % (int) Math.pow(10, CODE_DIGITS);
            return String.format("%0" + CODE_DIGITS + "d", otp);
        } catch (Exception ex) {
            throw new IllegalStateException("TOTP generation failed", ex);
        }
    }

    private String normalizeVerificationCode(String verificationCode) {
        return verificationCode == null ? "" : verificationCode.trim();
    }

    private String encodeBase32(byte[] value) {
        StringBuilder encoded = new StringBuilder((value.length * 8 + 4) / 5);
        int buffer = 0;
        int bitsLeft = 0;

        for (byte currentByte : value) {
            buffer = (buffer << 8) | (currentByte & 0xFF);
            bitsLeft += 8;

            while (bitsLeft >= 5) {
                encoded.append(BASE32_ALPHABET[(buffer >> (bitsLeft - 5)) & 0x1F]);
                bitsLeft -= 5;
            }
        }

        if (bitsLeft > 0) {
            encoded.append(BASE32_ALPHABET[(buffer << (5 - bitsLeft)) & 0x1F]);
        }

        return encoded.toString();
    }

    private byte[] decodeBase32(String value) {
        String normalized = value == null ? "" : value.trim().replace("=", "").toUpperCase();
        if (normalized.isEmpty()) {
            return new byte[0];
        }

        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream(normalized.length() * 5 / 8);
        int buffer = 0;
        int bitsLeft = 0;

        for (char currentChar : normalized.toCharArray()) {
            int decoded = indexOfBase32(currentChar);
            if (decoded < 0) {
                throw new IllegalArgumentException("Invalid Base32 value");
            }
            buffer = (buffer << 5) | decoded;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                output.write((buffer >> (bitsLeft - 8)) & 0xFF);
                bitsLeft -= 8;
            }
        }

        return output.toByteArray();
    }

    private int indexOfBase32(char value) {
        for (int i = 0; i < BASE32_ALPHABET.length; i++) {
            if (BASE32_ALPHABET[i] == value) {
                return i;
            }
        }
        return -1;
    }

    public record GeneratedSecret(byte[] secretBytes, String manualEntryKey, String otpauthUri) {
        public GeneratedSecret {
            secretBytes = secretBytes.clone();
        }

        @Override
        public byte[] secretBytes() {
            return secretBytes.clone();
        }
    }
}


