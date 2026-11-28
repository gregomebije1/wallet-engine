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
@RequestMapping("/api/wallet/v1")
@SecurityRequirement(name = "Bearer Authentication")  
@Tag(name = "Wallet Operations", description = "deposit, transfer, purchase view transactions")   
public class WalletController {

    //{"book": "wallet", "memo": "Deposit captured", 
    // "lines": [
    //  {"debit": 30300, "credit": 0, "account": "W300", "sub_ref": null}, 
    // {"debit": 0, "credit": 30000, "account": "W100", "sub_ref": "cd369254-ffd9-47ff-bbd8-c9ef5a8e4b09"}, 
    // {"debit": 0, "credit": 300, "account": "W200", "sub_ref": null}]}
    
    private final WalletService walletService;
    
    public WalletController(WalletService walletService) 
    { 
        this.walletService = walletService; 
    }


    @PostMapping("/deposit")
    @Operation(summary = "Deposit", description = "Deposit money into your wallet")
    public ResponseEntity<DepositResponse> deposit(
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody DepositRequest request) {
        return ResponseEntity.ok(walletService.deposit(idempotencyKey, request));
    }
    
   
    @GetMapping("/transactions")
    ResponseEntity<TransactionResponse> transactions(
	    @RequestHeader("X-Idempotency-Key") String key,
        @PathVariable("transactionId") String transactionId) {
        return new ResponseEntity<>(
            walletService.transactions(transactionId),
	        HttpStatus.OK);
    }

    @PostMapping("/transfer")
    ResponseEntity<TransferResponse> transfer(@RequestHeader("X-Idempotency-Key") String key,
            @Valid @RequestBody TransferRequest request) {
        return ResponseEntity.accepted().body(walletService.transfer(request));
    }
    
    @PostMapping("/purchase")
    ResponseEntity<PurchaseResponse> purchase(@RequestHeader("X-Idempotency-Key") String key,
            @Valid @RequestBody PurchaseRequest request) {
        return ResponseEntity.accepted().body(walletService.purchase(request));
    }

    
    @PostMapping("/balances/as_of")
    ResponseEntity<WalletBalanceResponse> balances(@RequestHeader("X-Idempotency-Key") String key, @Valid @RequestParam("as_of") String asOf) {
        return ResponseEntity.status(HttpStatus.OK).body(walletService.walletBalances(asOf));
    }
    
  

}



