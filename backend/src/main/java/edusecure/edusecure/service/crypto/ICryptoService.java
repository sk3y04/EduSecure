package edusecure.edusecure.service.crypto;

public interface ICryptoService {

    String hash(byte[] data);

    String signatureAlgorithm();

    String sign(byte[] data);

    boolean verify(byte[] data, String signature);

    String computeAuditMac(String canonicalPayload, String previousIntegrityValue);
}

