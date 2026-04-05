package edusecure.edusecure.service.submission;

import edusecure.edusecure.audit.AuditService;
import edusecure.edusecure.config.SubmissionUploadProperties;
import edusecure.edusecure.dto.submission.SubmissionContentResponse;
import edusecure.edusecure.dto.submission.SubmissionResponse;
import edusecure.edusecure.entity.assignment.Assignment;
import edusecure.edusecure.entity.audit.AuditActionType;
import edusecure.edusecure.entity.auth.RoleName;
import edusecure.edusecure.entity.grade.Grade;
import edusecure.edusecure.entity.auth.User;
import edusecure.edusecure.entity.submission.Submission;
import edusecure.edusecure.entity.submission.SubmissionVerificationStatus;
import edusecure.edusecure.repository.auth.UserRepository;
import edusecure.edusecure.repository.grade.GradeRepository;
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
import java.util.List;
import java.util.Locale;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubmissionService {

    private static final String PDF_SIGNATURE = "%PDF-";

    private final SubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final GradeRepository gradeRepository;
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
                    .signatureAlgorithm(cryptoService.signatureAlgorithm())
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

    @Transactional(readOnly = true)
    public SubmissionResponse getLatestSubmissionForAssignment(String currentUserEmail, UUID assignmentId) {
        User student = findUserByEmail(currentUserEmail);
        assignmentService.getAssignmentOrThrow(assignmentId);

        Submission submission = submissionRepository
                .findFirstByAssignmentIdAndStudentUserIdOrderBySubmittedAtDescIdDesc(assignmentId, student.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No submission found for this assignment"));

        return toResponse(submission);
    }

    @Transactional(readOnly = true)
    public java.util.List<SubmissionResponse> listSubmissionsForAssignment(UUID assignmentId) {
        assignmentService.getAssignmentOrThrow(assignmentId);

        java.util.List<Submission> submissions = submissionRepository.findAllByAssignmentIdOrderBySubmittedAtDesc(assignmentId);
        Map<UUID, Grade> gradesBySubmissionId = gradeRepository.findAllBySubmissionIdIn(
                        submissions.stream().map(Submission::getId).toList())
                .stream()
                .collect(Collectors.toMap(Grade::getSubmissionId, Function.identity()));

        return submissions.stream()
                .map(submission -> toResponse(submission, gradesBySubmissionId.get(submission.getId())))
                .toList();
    }

    private UploadedSubmission validateAndReadUpload(MultipartFile file) {
        String fileName = resolveFileName(file);

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

        try {
            byte[] contentBytes = file.getBytes();
            String declaredContentType = resolveContentType(file, fileName);
            String normalizedContentType = validateSupportedUploadType(fileName, declaredContentType, contentBytes);
            return new UploadedSubmission(fileName, normalizedContentType, contentBytes);
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

    private String validateSupportedUploadType(String fileName, String contentType, byte[] contentBytes) {
        if (isPdfUpload(fileName, contentType, contentBytes)) {
            return MediaType.APPLICATION_PDF_VALUE;
        }

        if (isSupportedContentType(contentType, MediaType.TEXT_PLAIN_VALUE)) {
            ensureUtf8Text(contentBytes);
            return MediaType.TEXT_PLAIN_VALUE;
        }

        throw new ResponseStatusException(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Only " + supportedContentTypesDescription() + " uploads are supported in the current submission flow"
        );
    }

    private boolean isPdfUpload(String fileName, String contentType, byte[] contentBytes) {
        boolean pdfByContentType = isSupportedContentType(contentType, MediaType.APPLICATION_PDF_VALUE);
        boolean pdfByFileName = fileName.toLowerCase(Locale.ROOT).endsWith(".pdf");

        if (!pdfByContentType && !pdfByFileName) {
            return false;
        }

        ensurePdfSignature(contentBytes);
        return true;
    }

    private boolean isSupportedContentType(String actualContentType, String supportedContentType) {
        if (!isConfiguredSupportedContentType(supportedContentType)) {
            return false;
        }

        try {
            MediaType parsedActualContentType = MediaType.parseMediaType(actualContentType);
            MediaType parsedSupportedContentType = MediaType.parseMediaType(supportedContentType);
            return parsedSupportedContentType.isCompatibleWith(parsedActualContentType);
        } catch (InvalidMediaTypeException ex) {
            return false;
        }
    }

    private boolean isConfiguredSupportedContentType(String supportedContentType) {
        return submissionUploadProperties.getAllowedContentTypes().stream()
                .anyMatch(configuredContentType -> {
                    try {
                        MediaType parsedConfiguredContentType = MediaType.parseMediaType(configuredContentType);
                        MediaType parsedSupportedContentType = MediaType.parseMediaType(supportedContentType);
                        return parsedConfiguredContentType.isCompatibleWith(parsedSupportedContentType);
                    } catch (InvalidMediaTypeException ex) {
                        return false;
                    }
                });
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

    private void ensurePdfSignature(byte[] contentBytes) {
        byte[] pdfSignatureBytes = PDF_SIGNATURE.getBytes(StandardCharsets.US_ASCII);
        if (contentBytes.length < pdfSignatureBytes.length) {
            throw invalidPdfUpload();
        }

        for (int index = 0; index < pdfSignatureBytes.length; index++) {
            if (contentBytes[index] != pdfSignatureBytes[index]) {
                throw invalidPdfUpload();
            }
        }
    }

    private ResponseStatusException invalidPdfUpload() {
        return new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Uploaded PDF files must include a valid PDF header");
    }

    private String supportedContentTypesDescription() {
        List<String> allowedContentTypes = submissionUploadProperties.getAllowedContentTypes();
        if (allowedContentTypes.size() == 1) {
            return allowedContentTypes.getFirst();
        }

        return String.join(" and ", allowedContentTypes);
    }

    private String formatUploadLimit(long sizeBytes) {
        long megabyte = 1024L * 1024L;
        if (sizeBytes % megabyte == 0) {
            return (sizeBytes / megabyte) + "MB";
        }

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
            byte[] decryptedContent = submissionContentEncryptionService.decrypt(
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
                    decryptedContent
            );
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Submission content could not be decrypted", ex);
        }
    }

    private SubmissionResponse toResponse(Submission submission) {
        Grade grade = gradeRepository.findBySubmissionId(submission.getId()).orElse(null);
        return toResponse(submission, grade);
    }

    private SubmissionResponse toResponse(Submission submission, Grade grade) {
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
                submission.getVerificationMessage(),
                grade != null,
                grade != null ? grade.getId() : null
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


