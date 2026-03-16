package edusecure.edusecure.controller;

import edusecure.edusecure.dto.AesDecryptRequest;
import edusecure.edusecure.dto.AesDecryptResponse;
import edusecure.edusecure.dto.AesEncryptRequest;
import edusecure.edusecure.dto.AesEncryptResponse;
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

