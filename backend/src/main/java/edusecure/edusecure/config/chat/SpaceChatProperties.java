package edusecure.edusecure.config.chat;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "app.chat")
public class SpaceChatProperties {

    private boolean enabled = false;

    @Min(value = 1, message = "Chat message max length must be at least 1")
    private int messageMaxLength = 2000;

    @Min(value = 1, message = "Default chat page size must be at least 1")
    @Max(value = 100, message = "Default chat page size must not exceed 100")
    private int pageSizeDefault = 30;

    @Min(value = 1, message = "Maximum chat page size must be at least 1")
    @Max(value = 100, message = "Maximum chat page size must not exceed 100")
    private int pageSizeMax = 100;

    @AssertTrue(message = "Default chat page size must be less than or equal to the maximum chat page size")
    public boolean isPageSizeConfigurationValid() {
        return pageSizeDefault <= pageSizeMax;
    }
}

