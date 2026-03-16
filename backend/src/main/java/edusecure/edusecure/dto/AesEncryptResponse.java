package edusecure.edusecure.dto;

public record AesEncryptResponse(
        String algorithm,
        String nonce,
        String ciphertext
) {
}

