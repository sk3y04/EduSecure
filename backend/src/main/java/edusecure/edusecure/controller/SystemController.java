package edusecure.edusecure.controller;

import edusecure.edusecure.dto.SystemStatusResponse;
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

