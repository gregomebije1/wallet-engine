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

/*
Charts of Accounts and Entry Templates
Book (owner)	Chart of accounts	Parts 1-2 codes
wallet (Wallet Service)	W010 Settlement Bank (A), W100 Customer Available (L), W110 Customer Held (L), W200 Fee Revenue (R), W210 Gateway Fees Expense (E), W300 Funding Receivable (A), W400 Vendor Payout Clearing (A)	1230, 2210, 2220, 4210, 5210, 1210, 1220
gateway (Gateway Service)	G100 Processor Receivable (A), G110 Gateway Bank (A), G200 Merchant Settlement Payable (L), G300 Processing Fee Revenue (R), G400 Interchange Expense (E)	1110, 1120, 2110, 4110, 5110
A line may carry a sub_ref: the customer's wallet id on W100/W110, or the merchant id on G200. That gives per-customer balances inside one account (a sub-ledger) without creating an account per customer. The calling service owns the business meaning, so it also owns the entry templates:

Id	Flow step (link)	Queued by	Book	Debit	Credit
e1	A3 Deposit captured	Wallet	wallet	W300 $103.00	W100[w1] $100.00; W200 $3.00
e2	A6 Settlement received	Wallet	wallet	W010 $101.50; W210 $1.50	W300 $103.00
e3	A2 Charge captured	Gateway	gateway	G100 $103.00	G200 $101.50; G300 $1.50
e4	A5 Settle and pay out	Gateway	gateway	G110 $103.00; G200 $101.50	G100 $103.00; G110 $101.50
e5	B P2P transfer	Wallet	wallet	W100[w1] $25.00	W100[w2] $25.00
e6	C1 Hold placed	Wallet	wallet	W100[w1] $10.00	W110[w1] $10.00
e13	G1 Vendor payout sent	Gateway	gateway	G200 $10.00	G110 $10.00
e7	C2a Payout done	Wallet	wallet	W110[w1] $10.00	W400 $10.00
e14	G2b Vendor payout reversed	Gateway	gateway	G110 $10.00	G200 $10.00
e8	C2b Hold released	Wallet	wallet	W110[w1] $10.00	W100[w1] $10.00
e9	M1 Merchant capture	Gateway	gateway	G100 $100.00	G200[m1] $97.20; G300 $2.80
e10	M2 Network settlement	Gateway	gateway	G110 $98.90; G400 $1.10	G100 $100.00
e11	M3 Merchant withdrawal	Gateway	gateway	G200[m1] $97.20	G110 $97.20
e12	M3b Withdrawal reversed	Gat
*/


@RestController
@RequestMapping("/api/v1/wallets")
public class WalletAccountController {
    
    private final WalletAccountService service;
    
    public WalletAccountController(WalletAccountService service) { 
        this.service = service; 
    }

    //Old
    @PostMapping("/accounts")
    ResponseEntity<?> create(@Valid @RequestBody WalletAccountRequest r) {
        WalletAccountResponse x = service.provision(r);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("status", "success", "data", x));
    }

    @PostMapping("/wallet")
    ResponseEntity<?> createWalet(@Valid @RequestBody CreateWalletRequest r) {
        //WalletAccountResponse x = service.provision(r);
        /*
        INSERT INTO books (name) VALUES ('wallet'), ('gateway');
        INSERT INTO accounts (book_id, code, name, type)
        SELECT b.id, v.code, v.name, v.type FROM (VALUES
        ('wallet','W010','Settlement Bank','Asset'), ('wallet','W100','Customer Available','Liability'),
        ('wallet','W110','Customer Held','Liability'), ('wallet','W200','Fee Revenue','Revenue'),
        ('wallet','W210','Gateway Fees Expense','Expense'), ('wallet','W300','Funding Receivable','Asset'),
        ('wallet','W400','Vendor Payout Clearing','Asset'),
        ('gateway','G100','Processor Receivable','Asset'), ('gateway','G110','Gateway Bank','Asset'),
        ('gateway','G200','Merchant Settlement Payable','Liability'),
        ('gateway','G300','Processing Fee Revenue','Revenue'), ('gateway','G400','Interchange Expense','Expense')
        ) AS v(book,code,name,type) JOIN books b ON b.name = v.book;

        INSERT INTO wallets (id, user_id) VALUES (:w1, gen_random_uuid()), (:w2, gen_random_uuid());
        */
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("status", "success", "data", x));
    }

    @PostMapping("/deposits")
    ResponseEntity<?> deposit(@Valid @RequestBody DepositRequest r) {
        /*BEGIN;
            WITH t AS (
            INSERT INTO wallet_txns (id, wallet_id, kind, amount, fee, status)
            VALUES (:dep1, :w1, 'DEPOSIT', 10000, 300, 'PENDING')
            ON CONFLICT (id) DO NOTHING RETURNING id, amount, fee)
            INSERT INTO outbox (target, path, idempotency_key, body)
            SELECT 'gateway', '/gateway/v1/charges', t.id,
                jsonb_build_object('charge_id', t.id, 'amount', t.amount + t.fee) FROM t;
            COMMIT;
        
         2.  Wallet relay Queue Gateway: Queue POST /gateway/v1/charges
            POST /ledger/v1/entries e1
            3.  Debit W300 (Funding Receivabl) Deposit captured $103.00   
              Credit W100[w1] Customer Available $100.00
              Credit ; W200  Fee Revenue $3.00
        */

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("status", "success", "data", x));
    }

    @PostMapping("/webhooks/gateway")
    ResponseEntity<?> webhook(@Valid @RequestBody WebhookRequest r) {
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
       

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("status", "success", "data", x));
    }

}



