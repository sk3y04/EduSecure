package edusecure.edusecure.service.auth;

import edusecure.edusecure.dto.auth.AuthResponse;
import edusecure.edusecure.dto.auth.AuthStatus;
import edusecure.edusecure.dto.auth.MfaDisableRequest;
import edusecure.edusecure.dto.auth.MfaEnableRequest;
import edusecure.edusecure.dto.auth.MfaEnableResponse;
import edusecure.edusecure.dto.auth.MfaSetupResponse;
import edusecure.edusecure.dto.auth.MfaStatusResponse;
import edusecure.edusecure.dto.auth.MfaVerifyRequest;
import edusecure.edusecure.entity.auth.MfaChallenge;
import edusecure.edusecure.entity.auth.MfaMethod;
import edusecure.edusecure.entity.auth.MfaRecoveryCode;
import edusecure.edusecure.entity.auth.User;
import edusecure.edusecure.repository.auth.MfaChallengeRepository;
import edusecure.edusecure.repository.auth.MfaRecoveryCodeRepository;
import edusecure.edusecure.repository.auth.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class MfaService {

    private static final String RECOVERY_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int RECOVERY_CODE_SEGMENT_LENGTH = 4;

    private final UserRepository userRepository;
    private final MfaChallengeRepository mfaChallengeRepository;
    private final MfaRecoveryCodeRepository mfaRecoveryCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final MfaSecretCryptoService mfaSecretCryptoService;
    private final TotpProvider totpProvider;
    private final AuthTokenService authTokenService;

    @Value("${mfa.challenge-ttl-seconds:300}")
    private long challengeTtlSeconds;

    @Value("${mfa.challenge-max-attempts:5}")
    private int challengeMaxAttempts;

    @Value("${mfa.recovery-code-count:8}")
    private int recoveryCodeCount;

    @Transactional(readOnly = true)
    public MfaStatusResponse status(String email) {
        User user = getUserByEmail(email);
        return new MfaStatusResponse(
                user.isMfaEnabled(),
                user.getMfaMethod(),
                user.isMfaEnabled() ? mfaRecoveryCodeRepository.countByUserAndUsedAtIsNull(user) : 0,
                user.getMfaEnabledAt()
        );
    }

    @Transactional
    public MfaSetupResponse setup(String email) {
        User user = getUserByEmail(email);
        if (user.isMfaEnabled()) {
            throw new AuthApiException(HttpStatus.CONFLICT, "MFA is already enabled for this account");
        }

        TotpProvider.GeneratedSecret generatedSecret = totpProvider.generateSecret(user.getEmail());
        MfaSecretCryptoService.EncryptedSecret encryptedSecret = mfaSecretCryptoService.encrypt(generatedSecret.secretBytes());

        user.setMfaEnabled(false);
        user.setMfaMethod(MfaMethod.TOTP);
        user.setMfaSecretCiphertext(encryptedSecret.ciphertext());
        user.setMfaSecretNonce(encryptedSecret.nonce());
        user.setMfaSecretKeyVersion(encryptedSecret.keyVersion());
        user.setMfaEnabledAt(null);
        mfaChallengeRepository.deleteByUser(user);
        mfaRecoveryCodeRepository.deleteByUser(user);

        userRepository.save(user);
        return new MfaSetupResponse(MfaMethod.TOTP, generatedSecret.manualEntryKey(), generatedSecret.otpauthUri());
    }

    @Transactional
    public MfaEnableResponse enable(String email, MfaEnableRequest request) {
        User user = getUserByEmail(email);
        if (user.isMfaEnabled()) {
            throw new AuthApiException(HttpStatus.CONFLICT, "MFA is already enabled for this account");
        }

        byte[] secret = getStoredSecret(user);
        if (!totpProvider.isValidCode(secret, request.verificationCode())) {
            throw new AuthApiException(HttpStatus.UNAUTHORIZED, "Invalid MFA verification code");
        }

        user.setMfaEnabled(true);
        user.setMfaMethod(MfaMethod.TOTP);
        user.setMfaEnabledAt(Instant.now());
        userRepository.save(user);

        mfaRecoveryCodeRepository.deleteByUser(user);
        List<String> recoveryCodes = generateRecoveryCodes();
        List<MfaRecoveryCode> recoveryCodeEntities = recoveryCodes.stream()
                .map(code -> MfaRecoveryCode.builder()
                        .user(user)
                        .codeHash(passwordEncoder.encode(normalizeRecoveryCode(code)))
                        .build())
                .toList();
        mfaRecoveryCodeRepository.saveAll(recoveryCodeEntities);

        return new MfaEnableResponse(true, MfaMethod.TOTP, recoveryCodes);
    }

    @Transactional
    public AuthResponse createLoginChallenge(User user) {
        if (!user.isMfaEnabled()) {
            throw new AuthApiException(HttpStatus.BAD_REQUEST, "MFA is not enabled for this account");
        }

        ensureStoredSecretExists(user);
        mfaChallengeRepository.deleteByExpiresAtBefore(Instant.now());
        mfaChallengeRepository.deleteByUser(user);

        Instant expiresAt = Instant.now().plusSeconds(challengeTtlSeconds);
        MfaChallenge challenge = mfaChallengeRepository.save(MfaChallenge.builder()
                .user(user)
                .mfaMethod(user.getMfaMethod() == null ? MfaMethod.TOTP : user.getMfaMethod())
                .expiresAt(expiresAt)
                .attemptCount(0)
                .maxAttempts(challengeMaxAttempts)
                .build());

        return new AuthResponse(
                AuthStatus.MFA_REQUIRED,
                null,
                null,
                null,
                null,
                null,
                true,
                null,
                challenge.getId(),
                challenge.getMfaMethod(),
                challenge.getExpiresAt(),
                challenge.getMaxAttempts() - challenge.getAttemptCount()
        );
    }
 
    @Transactional
    public AuthResponse verify(MfaVerifyRequest request) {
        MfaChallenge challenge = mfaChallengeRepository.findById(request.challengeId())
                .orElseThrow(() -> new AuthApiException(HttpStatus.UNAUTHORIZED, "Invalid MFA challenge"));

        Instant now = Instant.now();
        if (challenge.getConsumedAt() != null || challenge.getExpiresAt().isBefore(now)) {
            throw new AuthApiException(HttpStatus.GONE, "MFA challenge has expired or is no longer valid");
        }

        if (challenge.getAttemptCount() >= challenge.getMaxAttempts()) {
            throw new AuthApiException(HttpStatus.TOO_MANY_REQUESTS, "Too many MFA verification attempts");
        }

        User user = challenge.getUser();
        boolean valid = verifyTotpOrRecoveryCode(user, request.verificationCode(), now);
        if (!valid) {
            int updatedAttempts = challenge.getAttemptCount() + 1;
            challenge.setAttemptCount(updatedAttempts);
            mfaChallengeRepository.save(challenge);
            if (updatedAttempts >= challenge.getMaxAttempts()) {
                throw new AuthApiException(HttpStatus.TOO_MANY_REQUESTS, "Too many MFA verification attempts");
            }
            throw new AuthApiException(HttpStatus.UNAUTHORIZED, "Invalid MFA verification code");
        }

        challenge.setConsumedAt(now);
        mfaChallengeRepository.save(challenge);
        return authTokenService.issueMfaAuthenticatedResponse(user);
    }

    @Transactional
    public void disable(String email, MfaDisableRequest request) {
        User user = getUserByEmail(email);
        if (!user.isMfaEnabled()) {
            throw new AuthApiException(HttpStatus.CONFLICT, "MFA is not enabled for this account");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new AuthApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        if (!verifyTotpOrRecoveryCode(user, request.verificationCode(), Instant.now())) {
            throw new AuthApiException(HttpStatus.UNAUTHORIZED, "Invalid MFA verification code");
        }

        mfaChallengeRepository.deleteByUser(user);
        mfaRecoveryCodeRepository.deleteByUser(user);
        user.setMfaEnabled(false);
        user.setMfaMethod(null);
        user.setMfaSecretCiphertext(null);
        user.setMfaSecretNonce(null);
        user.setMfaSecretKeyVersion(null);
        user.setMfaEnabledAt(null);
        userRepository.save(user);
    }
 
    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthApiException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private byte[] getStoredSecret(User user) {
        ensureStoredSecretExists(user);
        return mfaSecretCryptoService.decrypt(user.getMfaSecretNonce(), user.getMfaSecretCiphertext());
    }

    private void ensureStoredSecretExists(User user) {
        if (user.getMfaSecretCiphertext() == null || user.getMfaSecretNonce() == null) {
            throw new AuthApiException(HttpStatus.CONFLICT, "MFA setup has not been started for this account");
        }
    }

    private boolean verifyTotpOrRecoveryCode(User user, String verificationCode, Instant now) {
        if (totpProvider.looksLikeTotpCode(verificationCode)) {
            return totpProvider.isValidCodeAt(getStoredSecret(user), verificationCode, now);
        }
        return consumeRecoveryCodeIfValid(user, verificationCode, now);
    }

    private boolean consumeRecoveryCodeIfValid(User user, String verificationCode, Instant now) {
        String normalizedCode = normalizeRecoveryCode(verificationCode);
        if (normalizedCode.isBlank()) {
            return false;
        }

        for (MfaRecoveryCode recoveryCode : mfaRecoveryCodeRepository.findAllByUserAndUsedAtIsNull(user)) {
            if (passwordEncoder.matches(normalizedCode, recoveryCode.getCodeHash())) {
                recoveryCode.setUsedAt(now);
                mfaRecoveryCodeRepository.save(recoveryCode);
                return true;
            }
        }
        return false;
    }

    private List<String> generateRecoveryCodes() {
        List<String> recoveryCodes = new ArrayList<>(recoveryCodeCount);
        for (int i = 0; i < recoveryCodeCount; i++) {
            recoveryCodes.add(generateRecoveryCode());
        }
        return recoveryCodes;
    }

    private String generateRecoveryCode() {
        return randomCodeSegment() + "-" + randomCodeSegment();
    }

    private String randomCodeSegment() {
        StringBuilder segment = new StringBuilder(RECOVERY_CODE_SEGMENT_LENGTH);
        for (int i = 0; i < RECOVERY_CODE_SEGMENT_LENGTH; i++) {
            int index = ThreadLocalRandom.current().nextInt(RECOVERY_CODE_ALPHABET.length());
            segment.append(RECOVERY_CODE_ALPHABET.charAt(index));
        }
        return segment.toString();
    }

    private String normalizeRecoveryCode(String code) {
        return code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
    }
}

