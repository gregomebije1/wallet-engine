package com.gregomebije.wallet.controller;

import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import com.gregomebije.wallet.service.account.*;
import com.gregomebije.wallet.dto.Models.*;
import com.gregomebije.wallet.model.*;
import com.gregomebije.wallet.repository.*;

@RestController
@RequestMapping("/api/wallet/v1")
public class WalletController {
    
    private final WalletAccountService walletAccountService;
     
    public WalletController(WalletAccountService walletAccountService) { 
        this.walletAccountService = walletAccountService; 
    }

    @GetMapping("/setup")
    ResponseEntity<?> setup() {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(Map.of("status", "success", "data", walletAccountService.setup()));
    }

    @PostMapping("/register")
    ResponseEntity<?> create(@Valid @RequestBody CreateUserRequest request) {
         return ResponseEntity.status(HttpStatus.CREATED)
            .body(Map.of("status", "success", "data", walletAccountService.register(request)));
    }

    @PostMapping("/login")
    ResponseEntity<LoginResponse> login(@Valid @RequestBody WalletLoginRequest request) {
         return ResponseEntity.ok(walletAccountService.login(request));
    }

    @PostMapping("/deposit")
    ResponseEntity<DepositResponse> deposit(@RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody DepositRequest request) {
        return ResponseEntity.ok(walletAccountService.deposit(idempotencyKey, request));
    }

    @GetMapping("/transactions")
    ResponseEntity<TransactionResponse> transactions(
	    @RequestHeader("X-Idempotency-Key") String key,
        @PathVariable("transactionId") String transactionId) {
        return new ResponseEntity<>(
            walletAccountService.transactions(transactionId),
	        HttpStatus.OK);
    }

    @PostMapping("/transfer")
    ResponseEntity<TransferResponse> transfer(@RequestHeader("X-Idempotency-Key") String key,
            @Valid @RequestBody TransferRequest request) {
        return ResponseEntity.accepted().body(walletAccountService.transfer(request));
    }
    
    @PostMapping("/purchase")
    ResponseEntity<PurchaseResponse> purchase(@RequestHeader("X-Idempotency-Key") String key,
            @Valid @RequestBody PurchaseRequest request) {
        return ResponseEntity.accepted().body(walletAccountService.purchase(request));
    }

    @PostMapping("/webhooks/gateway")
    ResponseEntity<WebhookResponse> webhook(@Valid @RequestBody WebhookRequest request) {
        /*-- [Wallet DB]
        ---Deposit captured
            BEGIN;
            INSERT INTO inbox (message_id) VALUES (:msg1) ON CONFLICT DO NOTHING;   -- 0 rows => ROLLBACK, return 200
            UPDATE wallet_txns SET status = 'SUCCESS' WHERE id = :dep1 AND status = 'PENDING';  -- must be 1 row
            UPDATE wallets SET available = available + 10000 WHERE id = :w1;
            SELECT enqueue_entry(:e1, 'wallet', 'Deposit captured',
            ARRAY[eline('W300', NULL, 10300, 0), eline('W100', :w1, 0, 10000), eline('W200', NULL, 0, 300)]);
            COMMIT;

            
            -- [Wallet DB]
            --- Gateway settlement 
            BEGIN;
            INSERT INTO inbox (message_id) VALUES (:msg2) ON CONFLICT DO NOTHING;   -- 0 rows => ROLLBACK, return 200
            SELECT enqueue_entry(:e2, 'wallet', 'Gateway settlement', ARRAY[
            eline('W010', NULL, 10150, 0), eline('W210', NULL, 150, 0), eline('W300', NULL, 0, 10300)]);
            COMMIT;
            Post GL e2
        */
       

        return ResponseEntity.status(HttpStatus.OK).body(walletAccountService.webhook(request));
    }

    
    @PostMapping("/balances/as_of")
    ResponseEntity<WalletBalanceResponse> balances(@RequestHeader("X-Idempotency-Key") String key, @Valid @RequestParam("as_of") String asOf) {
        return ResponseEntity.status(HttpStatus.OK).body(walletAccountService.walletBalances(asOf));
    }
    
  

}



