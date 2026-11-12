package com.gregomebije.wallet.controller;

import com.gregomebije.wallet.model.Models.*;
import com.gregomebije.wallet.service.account.*;
import com.gregomebije.wallet.service.transfer.*;
import com.gregomebije.wallet.service.funding.*;

import jakarta.validation.Valid;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/wallets")
public class WalletAccountController {
    
    private final WalletAccountService service;
    
    public WalletAccountController(WalletAccountService service) { 
        this.service = service; 
    }

    @PostMapping("/accounts")
    ResponseEntity<?> create(@Valid @RequestBody WalletAccountRequest r) {
        WalletAccountResponse x = service.provision(r);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("status", "success", "data", x));
    }
}



