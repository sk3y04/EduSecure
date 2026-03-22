package edusecure.edusecure.dto.crypto;

public record AesEncryptResponse(
        String algorithm,
        String nonce,
        String ciphertext
) {
}

