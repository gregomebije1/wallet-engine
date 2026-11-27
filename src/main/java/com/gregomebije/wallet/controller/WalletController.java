package com.gregomebije.wallet.controller;

import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import com.gregomebije.wallet.service.WalletService;
import com.gregomebije.wallet.dto.Models.*;
import com.gregomebije.wallet.model.*;
import com.gregomebije.wallet.repository.*;

@RestController
@RequestMapping("/api/wallet/v1")
public class WalletController {
    
    private final WalletService walletService;
     
    public WalletController(WalletService walletService) { 
        this.walletService = walletService; 
    }

    @PostMapping("/deposit")
    ResponseEntity<DepositResponse> deposit(@RequestHeader("X-Idempotency-Key") String idempotencyKey,
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
       

        return ResponseEntity.status(HttpStatus.OK).body(walletService.webhook(request));
    }

    
    @PostMapping("/balances/as_of")
    ResponseEntity<WalletBalanceResponse> balances(@RequestHeader("X-Idempotency-Key") String key, @Valid @RequestParam("as_of") String asOf) {
        return ResponseEntity.status(HttpStatus.OK).body(walletService.walletBalances(asOf));
    }
    
  

}



