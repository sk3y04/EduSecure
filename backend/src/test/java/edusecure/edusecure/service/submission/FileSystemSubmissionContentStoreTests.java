package edusecure.edusecure.service.submission;

import edusecure.edusecure.config.SubmissionStorageProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileSystemSubmissionContentStoreTests {

    @TempDir
    Path tempDir;

    @Test
    void storeReadAndDeleteRoundTripCiphertext() {
        FileSystemSubmissionContentStore store = new FileSystemSubmissionContentStore(properties());
        byte[] ciphertext = "ciphertext-payload".getBytes(StandardCharsets.UTF_8);

        String reference = store.store(ciphertext);
        byte[] stored = store.read(reference);
        store.delete(reference);

        assertThat(reference).startsWith("submission://");
        assertThat(stored).isEqualTo(ciphertext);
        assertThatThrownBy(() -> store.read(reference))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void invalidReferenceIsRejected() {
        FileSystemSubmissionContentStore store = new FileSystemSubmissionContentStore(properties());

        assertThatThrownBy(() -> store.read("../escape.bin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid");
    }

    private SubmissionStorageProperties properties() {
        SubmissionStorageProperties properties = new SubmissionStorageProperties();
        properties.setMasterKey("c3VibWlzc2lvbi1zdG9yYWdlLW1hc3Rlci1rZXktMDI=");
        properties.setMasterKeyVersion("test-v1");
        properties.setKeyWrapAlgorithm("AESWrap");
        properties.setCipherAlgorithm("AES/GCM/NoPadding");
        properties.setBasePath(tempDir.resolve("submission-storage").toString());
        return properties;
    }
}

