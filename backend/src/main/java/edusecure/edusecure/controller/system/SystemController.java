package edusecure.edusecure.controller.system;

import edusecure.edusecure.dto.system.SystemStatusResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    @GetMapping("/health")
    public SystemStatusResponse health() {
        return new SystemStatusResponse(
                "UP",
                "EduSecure",
                "Assignment-aligned backend foundation is running"
        );
    }
}

