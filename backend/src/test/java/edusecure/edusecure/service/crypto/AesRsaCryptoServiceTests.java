package edusecure.edusecure.service.crypto;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AesRsaCryptoServiceTests {

    private static final String PRIVATE_KEY_LOCATION = "classpath:crypto-demo-signing-private.pem";
    private static final String PUBLIC_KEY_LOCATION = "classpath:crypto-demo-signing-public.pem";
    private static final String AUDIT_SECRET = "QXVkaXRJbnRlZ3JpdHlTZWNyZXRGb3JUZXN0c0luRWR1U2VjdXJlMTIzNDU2Nzg5MDEyMzQ1Ng==";

    @Test
    void configuredDemoSigningKeyPairProducesStableSignatureAcrossServiceInstances() {
        AesRsaCryptoService firstService = new AesRsaCryptoService(
                new DefaultResourceLoader(),
                PRIVATE_KEY_LOCATION,
                PUBLIC_KEY_LOCATION,
                AUDIT_SECRET
        );
        AesRsaCryptoService secondService = new AesRsaCryptoService(
                new DefaultResourceLoader(),
                PRIVATE_KEY_LOCATION,
                PUBLIC_KEY_LOCATION,
                AUDIT_SECRET
        );

        byte[] payload = "EduSecure stable signing evidence".getBytes(StandardCharsets.UTF_8);

        String firstSignature = firstService.sign(payload);
        String secondSignature = secondService.sign(payload);

        assertThat(firstSignature).isEqualTo(secondSignature);
        assertThat(firstService.verify(payload, firstSignature)).isTrue();
        assertThat(secondService.verify(payload, firstSignature)).isTrue();
    }

    @Test
    void missingConfiguredSigningKeyFailsFast() {
        assertThatThrownBy(() -> new AesRsaCryptoService(
                new DefaultResourceLoader(),
                "classpath:missing-demo-signing-private.pem",
                PUBLIC_KEY_LOCATION,
                AUDIT_SECRET
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to load configured demo signing key pair");
    }
}

