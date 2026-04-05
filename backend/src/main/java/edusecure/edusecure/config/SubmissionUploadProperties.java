package edusecure.edusecure.config;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "submission.upload")
public class SubmissionUploadProperties {

    @NotEmpty
    private List<String> allowedContentTypes = new ArrayList<>(List.of(
            MediaType.TEXT_PLAIN_VALUE,
            MediaType.APPLICATION_PDF_VALUE
    ));

    @NotNull
    private DataSize maxFileSize = DataSize.ofMegabytes(5);
}

