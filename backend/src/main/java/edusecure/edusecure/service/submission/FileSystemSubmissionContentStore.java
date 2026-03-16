package edusecure.edusecure.service.submission;

import edusecure.edusecure.config.SubmissionStorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class FileSystemSubmissionContentStore implements SubmissionContentStore {

    private static final String REFERENCE_PREFIX = "submission://";
    private static final Pattern FILE_NAME_PATTERN = Pattern.compile("[a-f0-9\\-]+\\.bin");

    private final SubmissionStorageProperties properties;

    @Override
    public String store(byte[] ciphertext) {
        String fileName = UUID.randomUUID() + ".bin";
        Path basePath = basePath();
        Path targetPath = basePath.resolve(fileName);

        try {
            Files.createDirectories(basePath);
            Files.write(targetPath, ciphertext);
            return REFERENCE_PREFIX + fileName;
        } catch (IOException ex) {
            throw new UncheckedIOException("Submission ciphertext could not be stored", ex);
        }
    }

    @Override
    public byte[] read(String storedFileReference) {
        try {
            return Files.readAllBytes(resolve(storedFileReference));
        } catch (IOException ex) {
            throw new UncheckedIOException("Submission ciphertext could not be read", ex);
        }
    }

    @Override
    public void delete(String storedFileReference) {
        try {
            Files.deleteIfExists(resolve(storedFileReference));
        } catch (IOException ex) {
            throw new UncheckedIOException("Submission ciphertext could not be deleted", ex);
        }
    }

    private Path basePath() {
        return Paths.get(properties.getBasePath()).toAbsolutePath().normalize();
    }

    private Path resolve(String storedFileReference) {
        if (storedFileReference == null || !storedFileReference.startsWith(REFERENCE_PREFIX)) {
            throw new IllegalArgumentException("Stored file reference is invalid");
        }

        String fileName = storedFileReference.substring(REFERENCE_PREFIX.length());
        if (!FILE_NAME_PATTERN.matcher(fileName).matches()) {
            throw new IllegalArgumentException("Stored file reference is invalid");
        }

        Path resolved = basePath().resolve(fileName).normalize();
        if (!resolved.startsWith(basePath())) {
            throw new IllegalArgumentException("Stored file reference is invalid");
        }

        return resolved;
    }
}

