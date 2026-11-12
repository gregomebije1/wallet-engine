package com.gregomebije.wallet.controller;

import com.gregomebije.wallet.model.Models.*;
import com.gregomebije.wallet.service.account.*;
import com.gregomebije.wallet.service.transfer.*;
import com.gregomebije.wallet.service.funding.*;
import com.gregomebije.wallet.exception.Exceptions.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/transactions")
public class P2PTransferController {
    private final P2PTransferService service;
    public P2PTransferController(P2PTransferService service) { this.service = service; }

    @PostMapping("/p2p")
    ResponseEntity<?> transfer(@RequestHeader("X-Idempotency-Key") String key,
                               @Valid @RequestBody P2PRequestDto r) {
        P2PResponse x = service.transfer(
                new P2PRequest(r.senderWalletId(), r.receiverWalletId(), r.amount(), r.currency(), r.narration()), key);
        return ResponseEntity.accepted().body(Map.of("status", "success",
                "transaction_id", x.transactionId(), "summary", x));
    }

    public record P2PRequestDto(
            @NotBlank String senderWalletId,
            @NotBlank String receiverWalletId,
            @NotNull @Positive BigDecimal amount,
            @NotBlank String currency,
            String narration) {}
}