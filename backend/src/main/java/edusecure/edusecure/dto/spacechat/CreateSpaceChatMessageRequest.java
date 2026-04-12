package edusecure.edusecure.dto.spacechat;

public record CreateSpaceChatMessageRequest(
        String body,
        Integer keyVersion,
        String algorithm,
        String nonce,
        String ciphertext,
        String contentType,
        Integer plaintextLength
) {

    public CreateSpaceChatMessageRequest(String body) {
        this(body, null, null, null, null, null, null);
    }
}

