package edusecure.edusecure.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "submission.upload")
public class SubmissionUploadProperties {

    @NotBlank
    private String allowedContentType = "text/plain";

    @NotNull
    private DataSize maxFileSize = DataSize.ofKilobytes(256);
}

