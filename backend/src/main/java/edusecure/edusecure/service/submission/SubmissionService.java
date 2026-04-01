package edusecure.edusecure.service.submission;

import edusecure.edusecure.audit.AuditService;
import edusecure.edusecure.config.SubmissionUploadProperties;
import edusecure.edusecure.dto.submission.SubmissionContentResponse;
import edusecure.edusecure.dto.submission.SubmissionResponse;
import edusecure.edusecure.entity.assignment.Assignment;
import edusecure.edusecure.entity.audit.AuditActionType;
import edusecure.edusecure.entity.auth.RoleName;
import edusecure.edusecure.entity.auth.User;
import edusecure.edusecure.entity.submission.Submission;
import edusecure.edusecure.entity.submission.SubmissionVerificationStatus;
import edusecure.edusecure.repository.auth.UserRepository;
import edusecure.edusecure.repository.submission.SubmissionRepository;
import edusecure.edusecure.service.assignment.AssignmentService;
import edusecure.edusecure.service.crypto.ICryptoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final AssignmentService assignmentService;
    private final AuditService auditService;
    private final ICryptoService cryptoService;
    private final SubmissionContentEncryptionService submissionContentEncryptionService;
    private final SubmissionKeyProtectionService submissionKeyProtectionService;
    private final SubmissionContentStore submissionContentStore;
    private final SubmissionUploadProperties submissionUploadProperties;

    @Transactional
    public SubmissionResponse createSubmission(String currentUserEmail, UUID assignmentId, MultipartFile file) {
        User student = findUserByEmail(currentUserEmail);
        Assignment assignment = assignmentService.getAssignmentOrThrow(assignmentId);
        if (!assignment.isOpen()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Assignment is closed");
        }

        UploadedSubmission upload = validateAndReadUpload(file);
        byte[] contentBytes = upload.contentBytes();
        String hashDigest = cryptoService.hash(contentBytes);
        byte[] digestBytes = hashDigest.getBytes(StandardCharsets.UTF_8);
        String digitalSignature = cryptoService.sign(digestBytes);
        boolean verified = cryptoService.verify(digestBytes, digitalSignature);
        SubmissionVerificationStatus status = verified
                ? SubmissionVerificationStatus.VERIFIED
                : SubmissionVerificationStatus.FAILED_VERIFICATION;
        String verificationMessage = verified
                ? "Signature verified successfully"
                : "Signature verification failed";

        String storedFileReference = null;
        try {
            SubmissionContentEncryptionService.EncryptedSubmissionContent encryptedContent = submissionContentEncryptionService.encrypt(contentBytes);
            SubmissionKeyProtectionService.WrappedKey wrappedKey = submissionKeyProtectionService.wrap(encryptedContent.contentEncryptionKey());
            storedFileReference = submissionContentStore.store(encryptedContent.ciphertext());

            Submission submission = Submission.builder()
                    .assignmentId(assignment.getId())
                    .studentUserId(student.getId())
                    .submittedAt(Instant.now())
                    .fileName(upload.fileName())
                    .contentType(upload.contentType())
                    .storedFileReference(storedFileReference)
                    .storageEncryptionAlgorithm(encryptedContent.encryptionAlgorithm())
                    .storageEncryptionNonce(encryptedContent.nonce())
                    .wrappedContentEncryptionKey(wrappedKey.wrappedKey())
                    .keyWrapAlgorithm(wrappedKey.keyWrapAlgorithm())
                    .storageKeyVersion(wrappedKey.keyVersion())
                    .ciphertextLengthBytes(encryptedContent.ciphertextLengthBytes())
                    .hashDigest(hashDigest)
                    .digitalSignature(digitalSignature)
                    .signatureAlgorithm("SHA256withRSA")
                    .verificationStatus(status)
                    .verificationMessage(verificationMessage)
                    .build();

            Submission saved = submissionRepository.save(submission);
            auditService.record(
                    AuditActionType.SUBMISSION_CREATED,
                    student.getId(),
                    Submission.class.getSimpleName(),
                    saved.getId(),
                    "assignmentId=" + assignment.getId()
                            + ",fileName=" + saved.getFileName()
                            + ",encryptedAtRest=true"
            );
            auditService.record(
                    verified ? AuditActionType.SUBMISSION_VERIFIED : AuditActionType.SUBMISSION_VERIFICATION_FAILED,
                    student.getId(),
                    Submission.class.getSimpleName(),
                    saved.getId(),
                    "verificationStatus=" + saved.getVerificationStatus()
            );

            return toResponse(saved);
        } catch (RuntimeException ex) {
            cleanupStoredContent(storedFileReference);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Submission content could not be protected for storage", ex);
        }
    }

    private UploadedSubmission validateAndReadUpload(MultipartFile file) {
        String fileName = resolveFileName(file);
        String contentType = resolveContentType(file, fileName);

        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Submission file must not be empty");
        }

        long maxBytes = submissionUploadProperties.getMaxFileSize().toBytes();
        if (file.getSize() > maxBytes) {
            throw new ResponseStatusException(
                    HttpStatusCode.valueOf(413),
                    "Submission file exceeds the current " + formatUploadLimit(submissionUploadProperties.getMaxFileSize().toBytes()) + " limit"
            );
        }

        if (!isSupportedContentType(contentType)) {
            throw new ResponseStatusException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Only " + submissionUploadProperties.getAllowedContentType() + " uploads are supported in the current submission flow"
            );
        }

        try {
            byte[] contentBytes = file.getBytes();
            ensureUtf8Text(contentBytes);
            return new UploadedSubmission(fileName, contentType, contentBytes);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Submission file could not be read", ex);
        }
    }

    private String resolveFileName(MultipartFile file) {
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "");
        if (!StringUtils.hasText(originalFileName) || originalFileName.contains("..")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Submission file name is invalid");
        }
        return originalFileName;
    }

    private String resolveContentType(MultipartFile file, String fileName) {
        String contentType = file.getContentType();
        if (contentType != null) {
            contentType = contentType.trim();
        }
        if (StringUtils.hasText(contentType) && !MediaType.APPLICATION_OCTET_STREAM_VALUE.equalsIgnoreCase(contentType)) {
            return contentType;
        }

        String guessedContentType = URLConnection.guessContentTypeFromName(fileName);
        if (StringUtils.hasText(guessedContentType)) {
            return guessedContentType;
        }

        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    private boolean isSupportedContentType(String contentType) {
        try {
            MediaType actualContentType = MediaType.parseMediaType(contentType);
            MediaType supportedContentType = MediaType.parseMediaType(submissionUploadProperties.getAllowedContentType());
            return supportedContentType.isCompatibleWith(actualContentType);
        } catch (InvalidMediaTypeException ex) {
            return false;
        }
    }

    private void ensureUtf8Text(byte[] contentBytes) {
        try {
            StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(contentBytes));
        } catch (CharacterCodingException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only UTF-8 text/plain files are supported", ex);
        }
    }

    private String formatUploadLimit(long sizeBytes) {
        if (sizeBytes % 1024 == 0) {
            return (sizeBytes / 1024) + "KB";
        }

        return sizeBytes + "B";
    }

    @Transactional(readOnly = true)
    public SubmissionResponse getSubmission(UUID submissionId, Authentication authentication) {
        Submission submission = requireAccessibleSubmission(submissionId, authentication);
        return toResponse(submission);
    }

    @Transactional
    public SubmissionContentResponse getSubmissionContent(UUID submissionId, Authentication authentication) {
        User currentUser = findUserByEmail(authentication.getName());
        Submission submission = requireAccessibleSubmission(submissionId, authentication, currentUser);

        try {
            byte[] ciphertext = submissionContentStore.read(submission.getStoredFileReference());
            byte[] plaintext = submissionContentEncryptionService.decrypt(
                    ciphertext,
                    submission.getStorageEncryptionNonce(),
                    submissionKeyProtectionService.unwrap(
                            submission.getWrappedContentEncryptionKey(),
                            submission.getStorageKeyVersion()
                    )
            );

            auditService.record(
                    AuditActionType.SUBMISSION_CONTENT_ACCESSED,
                    currentUser.getId(),
                    Submission.class.getSimpleName(),
                    submission.getId(),
                    "fileName=" + submission.getFileName() + ",contentRetrieved=true"
            );

            return new SubmissionContentResponse(
                    submission.getId(),
                    submission.getFileName(),
                    submission.getContentType(),
                    new String(plaintext, StandardCharsets.UTF_8)
            );
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Submission content could not be decrypted", ex);
        }
    }

    private SubmissionResponse toResponse(Submission submission) {
        return new SubmissionResponse(
                submission.getId(),
                submission.getAssignmentId(),
                submission.getStudentUserId(),
                submission.getSubmittedAt(),
                submission.getFileName(),
                submission.getContentType(),
                submission.getHashDigest(),
                submission.getDigitalSignature(),
                submission.getSignatureAlgorithm(),
                submission.getVerificationStatus(),
                submission.getVerificationMessage()
        );
    }

    private Submission requireAccessibleSubmission(UUID submissionId, Authentication authentication) {
        return requireAccessibleSubmission(submissionId, authentication, null);
    }

    private Submission requireAccessibleSubmission(UUID submissionId, Authentication authentication, User currentUser) {
        User resolvedUser = currentUser != null ? currentUser : findUserByEmail(authentication.getName());
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Submission not found"));

        boolean privileged = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> ("ROLE_" + RoleName.LECTURER.name()).equals(authority)
                        || ("ROLE_" + RoleName.ADMIN.name()).equals(authority));

        if (!privileged && !submission.getStudentUserId().equals(resolvedUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot view this submission");
        }

        return submission;
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private void cleanupStoredContent(String storedFileReference) {
        if (storedFileReference == null) {
            return;
        }

        try {
            submissionContentStore.delete(storedFileReference);
        } catch (RuntimeException ignored) {
            // Best-effort cleanup only; the original failure should still propagate.
        }
    }

    private record UploadedSubmission(String fileName, String contentType, byte[] contentBytes) {
    }
}


