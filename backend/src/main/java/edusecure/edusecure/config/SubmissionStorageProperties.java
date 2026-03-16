package edusecure.edusecure.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "submission.storage")
public class SubmissionStorageProperties {

    @NotBlank
    private String masterKey;

    @NotBlank
    private String masterKeyVersion;

    @NotBlank
    private String keyWrapAlgorithm = "AESWrap";

    @NotBlank
    private String cipherAlgorithm = "AES/GCM/NoPadding";

    @NotBlank
    private String basePath = "./build/submission-storage";
}

