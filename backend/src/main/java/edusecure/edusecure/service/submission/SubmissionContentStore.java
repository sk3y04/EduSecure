package edusecure.edusecure.service.submission;

public interface SubmissionContentStore {

    String store(byte[] ciphertext);

    byte[] read(String storedFileReference);

    void delete(String storedFileReference);
}

