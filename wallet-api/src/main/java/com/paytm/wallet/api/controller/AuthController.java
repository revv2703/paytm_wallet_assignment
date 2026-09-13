package com.paytm.wallet.api.controller;

import com.paytm.wallet.common.dto.TokenRequest;
import com.paytm.wallet.common.dto.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Bearer token generation APIs")
public class AuthController {

    @PostMapping("/token")
    @Operation(summary = "Generate a base64 bearer token for a user ID")
    public ResponseEntity<TokenResponse> createToken(@RequestBody TokenRequest request) {
        if (request == null || request.userId() == null || request.userId().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        String encoded = Base64.getEncoder().encodeToString(request.userId().getBytes(StandardCharsets.UTF_8));
        return ResponseEntity.ok(new TokenResponse(encoded));
    }
}
