package com.gregomebije.wallet.controller;

import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;

import com.gregomebije.wallet.dto.Models.GatewayWebhookEvent;
import com.gregomebije.wallet.utility.WebhookSignatureVerifier;
import com.gregomebije.wallet.service.WalletService;
import com.gregomebije.wallet.dto.Models.*;

@RestController
@RequestMapping("/api/wallet/v1/webhooks")
@Tag(name = "Webhook Operations", description = "webhook operations")   
public class WebhookController {
    
    private final WalletService walletService;
    private final WebhookSignatureVerifier signatureVerifier;
    private final ObjectMapper objectMapper;
    
    public WebhookController(
        WalletService walletService,
        WebhookSignatureVerifier signatureVerifier,
        ObjectMapper objectMapper) 
    { 
        this.walletService = walletService; 
        this.signatureVerifier = signatureVerifier;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/gateway")
    /*
        curl -X POST http://localhost:8080/wallet/v1/webhooks/gateway \
            -H "Content-Type: application/json" \
            -H "Idempotency-Key: e0000000-0000-0000-0000-000000000001" \
            -H "X-Signature: sha256=544c02d3d80fa72e8dd8e14dda481ae2893713b6a5f3283d1039e7549f185380" \
            -d '{"type":"charge.captured","charge_id":"d0000000-0000-0000-0000-000000000001"}'

            { "type": "settlement.paid", "charge_id": "d0000000-…-0001",
                "gross": 10300, "fee": 150, "net": 10150 }    
    */
    public ResponseEntity<?> handleGatewayWebhook(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Signature", required = false) String signature,
            @RequestBody String rawBody) {

        // 1. Verify HMAC-SHA256 signature -> 401 on mismatch
        if (!signatureVerifier.isValid(rawBody, signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", Map.of("code", "invalid_signature")));
        }

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", Map.of("code", "missing_idempotency_key")));
        }

        GatewayWebhookEvent event;
        try {
            event = objectMapper.readValue(rawBody, GatewayWebhookEvent.class);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", Map.of("code", "invalid_payload")));
        }

        if (event.charge_id() == null || event.charge_id().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", Map.of("code", "missing_charge_id")));
        }

        try {
            String result = switch (event.type()) {
                case "charge.captured" ->
                        walletService.captureDeposit(idempotencyKey, event.charge_id());

                case "settlement.paid" -> {
                    if (event.gross() == null || event.fee() == null || event.net() == null) {
                        yield "MISSING_AMOUNTS";
                        
                    }
                    yield walletService.settleGateway(idempotencyKey, event);
                }

                default -> "IGNORED";
            };

            if ("MISSING_AMOUNTS".equals(result)) {
                return ResponseEntity.badRequest()
                                .body(Map.of("error", Map.of("code", "missing_settlement_amounts")));
            }
            if ("DUPLICATE".equals(result)) {
                return ResponseEntity.ok(Map.of("received", true, "duplicate", true));
            }

            return ResponseEntity.ok(Map.of("received", true));

        } catch (DataAccessException dbEx) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", Map.of("code", "database_unavailable " + dbEx.toString())));
        }

    }


    @PostMapping("/sign")
    /*
       PAYLOAD='{"type":"charge.captured","charge_id":"f0000000-0000-0000-0000-000000000001"}'

        curl -s -X POST 'http://localhost:8080/api/wallet/v1/webhooks/gateway/sign' \
        -H 'Content-Type: application/json' \
        -d "$PAYLOAD"
    */
    public ResponseEntity<Map<String, String>> generateSignature(@RequestBody String rawBody) {
        String signature = signatureVerifier.sign(rawBody);
        return ResponseEntity.ok(Map.of("signature", signature));
    }
}