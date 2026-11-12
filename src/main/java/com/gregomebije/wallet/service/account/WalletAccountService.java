package com.gregomebije.wallet.service.account;

import com.gregomebije.wallet.model.Models.WalletAccountRequest;
import com.gregomebije.wallet.model.Models.WalletAccountResponse;
import org.springframework.stereotype.Service;
import java.time.Instant;

@Service
//  Orchestrating the Workflow in a Wallet Service
public class WalletAccountService {

    //private final PaymentGatewayClient paymentGatewayClient;
    //private final LedgerClient ledgerClient;
    // private final WalletRepository walletRepository;

    /*
    public WalletAccountService(//PaymentGatewayClient paymentGatewayClient, 
                         //LedgerClient ledgerClient, 
                        // WalletRepository walletRepository) {
        //this.paymentGatewayClient = paymentGatewayClient;
        //this.ledgerClient = ledgerClient;
        //this.walletRepository = walletRepository;
    }
    */
    public WalletAccountResponse provision(WalletAccountRequest r) {
        return new WalletAccountResponse("wlt-test", "acc-test", r.userId(), r.currency(),
                "ACTIVE", Instant.now());
    }

    /*
    @Transactional
    public void fundWallet(String customerId, BigDecimal amount, String currency, String idempotencyKey) {
        // 1. Call Payment Gateway to charge the card (Blocking call example for simplicity)
        ChargeRequest chargeRequest = new ChargeRequest(customerId, amount, currency);
        PaymentResponse paymentResponse = paymentGatewayClient.charge(chargeRequest, idempotencyKey)
                .block(); // Use .block() if running standard Spring MVC, or chain reactively for WebFlux

        if (paymentResponse == null || !"SUCCESS".equals(paymentResponse.getStatus())) {
            throw new RuntimeException("Payment charge was unsuccessful");
        }

        // 2. Prepare and post to Ledger System
        JournalEntryRequest ledgerRequest = buildFundingJournalRequest(customerId, amount, currency, paymentResponse.getReferenceId());
        ledgerClient.recordJournalEntry(ledgerRequest, idempotencyKey).block();

        // 3. Update Wallet Engine fast-access balance locally
        walletRepository.incrementBalance(customerId, amount);
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