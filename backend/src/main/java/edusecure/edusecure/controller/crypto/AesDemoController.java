package edusecure.edusecure.controller.crypto;

import edusecure.edusecure.dto.crypto.AesDecryptRequest;
import edusecure.edusecure.dto.crypto.AesDecryptResponse;
import edusecure.edusecure.dto.crypto.AesEncryptRequest;
import edusecure.edusecure.dto.crypto.AesEncryptResponse;
import edusecure.edusecure.service.crypto.AesGcmDemoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/crypto-demo")
@RequiredArgsConstructor
public class AesDemoController {

    private final AesGcmDemoService aesGcmDemoService;

    @PostMapping("/encrypt")
    public ResponseEntity<AesEncryptResponse> encrypt(@Valid @RequestBody AesEncryptRequest request) {
        return ResponseEntity.ok(aesGcmDemoService.encrypt(request));
    }

    @PostMapping("/decrypt")
    public ResponseEntity<AesDecryptResponse> decrypt(@Valid @RequestBody AesDecryptRequest request) {
        return ResponseEntity.ok(aesGcmDemoService.decrypt(request));
    }
}

