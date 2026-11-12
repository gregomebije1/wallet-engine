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
@RequestMapping("/api/v1/wallets/funding")
public class FundingController {
    private final FundingService service;
    public FundingController(FundingService service) { this.service = service; }

    @PostMapping("/intent")
    ResponseEntity<?> intent(@Valid @RequestBody FundingIntentDto r) {
        return ResponseEntity.ok(Map.of("status", "success", "data",
                service.createIntent(new FundingIntentRequest(r.walletId(), r.amount(), r.currency(), r.paymentMethod(), r.provider()))));
    }

    public record FundingIntentDto(@NotBlank String walletId, @NotNull @Positive BigDecimal amount,
                                    @NotBlank String currency, @NotBlank String paymentMethod, @NotBlank String provider) {}
}
