package edusecure.edusecure.service.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class AesRsaCryptoService implements ICryptoService {

    private final KeyPair demoSigningKeyPair;
    private final byte[] auditSecretBytes;

    public AesRsaCryptoService(
            ResourceLoader resourceLoader,
            @Value("${crypto.signing.private-key-location}") String privateKeyLocation,
            @Value("${crypto.signing.public-key-location}") String publicKeyLocation,
            @Value("${audit.hmac-secret}") String auditSecret
    ) {
        Resource privateKeyResource = resourceLoader.getResource(privateKeyLocation);
        Resource publicKeyResource = resourceLoader.getResource(publicKeyLocation);
        this.demoSigningKeyPair = loadKeyPair(privateKeyResource, publicKeyResource);
        this.auditSecretBytes = Base64.getDecoder().decode(auditSecret);
    }

    @Override
    public String hash(byte[] data) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(messageDigest.digest(data));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to hash content", ex);
        }
    }

    @Override
    public String sign(byte[] data) {
        return Base64.getEncoder().encodeToString(signWithPrivateKey(data, demoSigningKeyPair.getPrivate()));
    }

    @Override
    public boolean verify(byte[] data, String signature) {
        try {
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(demoSigningKeyPair.getPublic());
            verifier.update(data);
            return verifier.verify(Base64.getDecoder().decode(signature));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to verify signature", ex);
        }
    }

    @Override
    public String computeAuditMac(String canonicalPayload, String previousIntegrityValue) {
        try {
            String payload = canonicalPayload + "|previous=" + (previousIntegrityValue == null ? "" : previousIntegrityValue);
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(auditSecretBytes, "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to compute audit integrity value", ex);
        }
    }

    private byte[] signWithPrivateKey(byte[] data, PrivateKey privateKey) {
        try {
            Signature signer = Signature.getInstance("SHA256withRSA");
            signer.initSign(privateKey);
            signer.update(data);
            return signer.sign();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to sign content", ex);
        }
    }

    private KeyPair loadKeyPair(Resource privateKeyResource, Resource publicKeyResource) {
        try {
            PrivateKey privateKey = readPrivateKey(privateKeyResource);
            PublicKey publicKey = readPublicKey(publicKeyResource);
            return new KeyPair(publicKey, privateKey);
        } catch (IOException | GeneralSecurityException | IllegalStateException ex) {
            throw new IllegalStateException("Failed to load configured demo signing key pair", ex);
        }
    }

    private PrivateKey readPrivateKey(Resource privateKeyResource) throws IOException, GeneralSecurityException {
        byte[] decoded = decodePem(privateKeyResource, "PRIVATE KEY");
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
    }

    private PublicKey readPublicKey(Resource publicKeyResource) throws IOException, GeneralSecurityException {
        byte[] decoded = decodePem(publicKeyResource, "PUBLIC KEY");
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
    }

    private byte[] decodePem(Resource resource, String pemLabel) throws IOException {
        if (!resource.exists()) {
            throw new IllegalStateException("Configured signing key resource does not exist: " + resource);
        }

        String pem = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String normalized = pem
                .replace("-----BEGIN " + pemLabel + "-----", "")
                .replace("-----END " + pemLabel + "-----", "")
                .replaceAll("\\s", "");

        try {
            return Base64.getDecoder().decode(normalized);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("Configured signing key PEM could not be decoded: " + resource, ex);
        }
    }
}


