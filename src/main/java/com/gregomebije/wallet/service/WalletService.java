package com.gregomebije.wallet.service;

import tools.jackson.databind.json.JsonMapper;
import java.util.List;
import java.util.ArrayList;
import java.util.Map; 
import java.util.UUID;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.gregomebije.wallet.dto.Models.*;
import com.gregomebije.wallet.model.*;
import com.gregomebije.wallet.repository.*;
import com.gregomebije.wallet.service.LedgerTokenService;
import com.gregomebije.wallet.integration.LedgerClient;


@Service
//  Orchestrating the Workflow in a Wallet Service
public class WalletService {

    private final OutboxRepository outboxRepository;
    private final AccountRepository accountRepository;
    private final InboxRepository inboxRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final UserRepository userRepository;
    private final RestClient gatewayRestClient;
    private final LedgerClient ledgerClient;
    private final LedgerTokenService ledgerTokenService;
    private final JsonMapper jsonMapper;
    private final String ledgerServiceTenantId;
    private final PasswordEncoder passwordEncoder;
    private final String gatewayChargesUrl;
    private final ServiceProviderRepository serviceProviderRepository;

    private static final Logger logger = LoggerFactory.getLogger(WalletService.class);

    public WalletService (
            OutboxRepository outboxRepository,
            AccountRepository accountRepository,
            InboxRepository inboxRepository,
            WalletRepository walletRepository,
            WalletTransactionRepository walletTransactionRepository,
            UserRepository userRepository,
            @Qualifier("gatewayRestClient") RestClient gatewayRestClient,
            LedgerClient ledgerClient,
            LedgerTokenService ledgerTokenService,
            JsonMapper jsonMapper, 
            @Value("${services.ledger.tenant-id}") String ledgerServiceTenantId,
            PasswordEncoder passwordEncoder,
            @Value("${services.gateway.charges.url}") String gatewayChargesUrl,
            ServiceProviderRepository serviceProviderRepository) {
        this.outboxRepository = outboxRepository;
        this.accountRepository = accountRepository;
        this.inboxRepository = inboxRepository;
        this.walletRepository = walletRepository;
        this.userRepository = userRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.gatewayRestClient = gatewayRestClient;
        this.ledgerClient = ledgerClient;
        this.ledgerTokenService = ledgerTokenService;
        this.jsonMapper = jsonMapper;
        this.ledgerServiceTenantId = ledgerServiceTenantId;
        this.passwordEncoder = passwordEncoder;
        this.gatewayChargesUrl = gatewayChargesUrl;  
        this.serviceProviderRepository = serviceProviderRepository;
    }

    public DepositResponse deposit(String idempotencyKey, DepositRequest request) {

        ServiceProvider sp = serviceProviderRepository.findByName("FUNDING")
            .orElseThrow(() -> new IllegalArgumentException("Servie provider not found"));

         return walletRepository.createDeposit(
                idempotencyKey,
                request,
                sp.getCost(request.amount()));
    }

    public String captureDeposit(String idempotencyKey, String chargeId) {
        return walletRepository.captureDeposit(idempotencyKey, chargeId);
    }

    public String settleGateway(String idempotencyKey, GatewayWebhookEvent event) {
        return walletRepository.settleGateway(
                            idempotencyKey,
                            event.charge_id(),
                            event.gross(),
                            event.fee(),
                            event.net()
                    );
    }
    /*
        
        //4. Ledger Entries
        String g100AccountId = accountRepository.findByName("Processor Receivable").get().getAccountId();
        String g200AccountId = accountRepository.findByName("Merchant Settlement Payable").get().getAccountId();
        String g300AccountId = accountRepository.findByName("Processing Fee Revenue").get().getAccountId();

        String dep2 = "d0000000-0000-0000-0000-000000000002";
       // 4. Enqueue Balanced Double-Entry Ledger SECOND using LedgerClient records directly
        var transactionRequest = new CreateTransactionRequest(
                "Wallet funding captured",
                dep2,
                List.of(
                        new LedgerLine(UUID.fromString(g100AccountId), LedgerLineType.DEBIT, 10300L, 0L),   // Debit G100: 10300
                        new LedgerLine(UUID.fromString(g200AccountId), LedgerLineType.CREDIT, 0L, 10150L),  // Credit G200: 10150
                        new LedgerLine(UUID.fromString(g300AccountId), LedgerLineType.CREDIT, 0L, 150L)     // Credit G300: 150
                )
        );
        //Todo: I need to match this.tenantId and request.tenantId()
        //this.tenantId = 'payment_gateway_tenant' but request.tenantId = "a UUID"
        outboxRepository.save(
                new Outbox(UUID.fromString(request.tenantId()), "ledger", "/api/v1/ledgers/transactions", dep2, transactionRequest)
               
        );


        */
    

    public TransactionResponse transactions(String transactionId) {
        //WalletTransaction walletTransaction = walletTransactionRepository
        //    .findById(UUID.fromString(transactionId))
        //    .orElseThrow(() -> new IllegalArgumentException("Unknown transaction"));

			return new TransactionResponse(
                "d0000000-…-0001",
                "DEPOSIT",
                "SUCCESS",
                10000,
                300,
                "a1000000-…-0001");
    }

    @Transactional
    public  TransferResponse transfer(TransferRequest request) {
        return new TransferResponse(
            "d0000000-…-0002",
            "P2P",
            "SUCCESS",
            2500);
    }
    
    @Transactional
    public  PurchaseResponse purchase(PurchaseRequest request) {
        return new PurchaseResponse(
            "d0000000-…-0003",
            "VAS",
            "PENDING",
            1000);
    }

    @Transactional
    public WebhookResponse webhook(WebhookRequest request) {
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
       

        return new WebhookResponse("true", "true");
    }

    @Transactional
    public WalletBalanceResponse walletBalances(String request) {
        return new WalletBalanceResponse(
            "2026-11-20T10:20:00Z",
            List.of(
                new WalletResponse("a1000000-…-0001", 8000, 0L),   
                new WalletResponse("a1000000-…-0002", 2500, 0L)));
        /*
        // 1. Tenant-scoped deduplication check
        int insertedRows = inboxRepository.insertIgnoreConflict(UUID.fromString(request.tenantId()), request.idempotencyKey());
        if (insertedRows == 0) {
            return paymentRepository.findByTenantIdAndIdempotencyKey(UUID.fromString(request.tenantId()), request.idempotencyKey())
                      .map(p -> new CaptureResult(p.getId().toString(), p.getStatus()))
                    .orElseThrow(() -> new IllegalStateException("Duplicate inbox key with no payment: " + request.idempotencyKey()));
        }

        // 2. Insert captured payment tied to tenant
        //Payment payment = paymentRepository.save(
        //       new Payment(dep1, tenantId, 10300L, 150L, "CAPTURED")
        //);
        Payment payment = Payment.builder()
                .idempotencyKey(request.idempotencyKey())
                .tenantId(UUID.fromString(request.tenantId()))
                .amount(request.amount())
                .fee(request.fee())
                .status("CAPTURED")
                .build();
        paymentRepository.save(payment);

        // 3. Stage Wallet Webhook FIRST (tied to tenant)
        /*var webhookPayload = new GatewayWebhookPayload("charge.captured", request.idempotencyKey());
        outboxRepository.save(
                new Outbox(UUID.fromString(request.tenantId()),
                 "wallet", 
                 "/wallet/v1/webhooks/gateway", 
                 request.idempotencyKey(), 
                 webhookPayload)
        );
        
        String g100AccountId = accountRepository.findByName("Processor Receivable").get().getAccountId();
        String g200AccountId = accountRepository.findByName("Merchant Settlement Payable").get().getAccountId();
        String g300AccountId = accountRepository.findByName("Processing Fee Revenue").get().getAccountId();

        String dep2 = "d0000000-0000-0000-0000-000000000002";
       // 4. Enqueue Balanced Double-Entry Ledger SECOND using LedgerClient records directly
        var transactionRequest = new CreateTransactionRequest(
                "Wallet funding captured",
                dep2,
                List.of(
                        new LedgerLine(UUID.fromString(g100AccountId), LedgerLineType.DEBIT, 10300L, 0L),   // Debit G100: 10300
                        new LedgerLine(UUID.fromString(g200AccountId), LedgerLineType.CREDIT, 0L, 10150L),  // Credit G200: 10150
                        new LedgerLine(UUID.fromString(g300AccountId), LedgerLineType.CREDIT, 0L, 150L)     // Credit G300: 150
                )
        );
        //Todo: I need to match this.tenantId and request.tenantId()
        //this.tenantId = 'payment_gateway_tenant' but request.tenantId = "a UUID"
        outboxRepository.save(
                new Outbox(UUID.fromString(request.tenantId()), "ledger", "/api/v1/ledgers/transactions", dep2, transactionRequest)
               
        );


        return new CaptureResult(payment.getId().toString(), payment.getStatus());
        }

            private JournalEntryRequest buildFundingJournalRequest(String customerId, BigDecimal amount, String currency, String pgRef) {
            // Constructs balanced double-entry post (Gateway Clearing -> Customer Liability)
            return new JournalEntryRequest(
                pgRef,
                "Wallet funding for customer " + customerId,
                List.of(
                    new Posting("acc_gateway_clearing", amount.negate(), currency), // Debit
                    new Posting("acc_liability_" + customerId, amount, currency)     // Credit
                )
            );
            
         }
    */
    }
    
}
