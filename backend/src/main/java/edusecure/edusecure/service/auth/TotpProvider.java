package edusecure.edusecure.service.auth;

import java.time.Instant;

public interface TotpProvider {

    GeneratedSecret generateSecret(String accountName);

    boolean looksLikeTotpCode(String verificationCode);

    boolean isValidCode(byte[] secret, String verificationCode);

    boolean isValidCodeAt(byte[] secret, String verificationCode, Instant instant);

    String generateCodeAt(byte[] secret, Instant instant);

    String generateCurrentCodeFromBase32(String manualEntryKey);

    String generateCodeAtFromBase32(String manualEntryKey, Instant instant);

    record GeneratedSecret(byte[] secretBytes, String manualEntryKey, String otpauthUri) {
        public GeneratedSecret {
            secretBytes = secretBytes.clone();
        }

        @Override
        public byte[] secretBytes() {
            return secretBytes.clone();
        }
    }
}
