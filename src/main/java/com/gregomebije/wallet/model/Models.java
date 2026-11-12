package com.gregomebije.wallet.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.*;

public final class Models {
    private Models() {}

    public enum ComplianceTier { TIER_1, TIER_2, TIER_3}
    public enum FundingStatus { INITIATED, PENDING_PROVIDER, SUCCESS, FAILED}
    public enum ServiceType { FUND, P2P, PAYOUT, AIRTIME, DATA_SUBSCRIPTION, UTILITY_BILL, CABLE_TV}
    public enum AccountStatus { PENDING_VERIFICATION, ACTIVE, SUSPENDED };
    public enum ServiceStatus { PENDING, SUCCESS, FAILED };
    public enum SagaStatus {STARTED, LEDGER_HOLD_SUCCESS, VENDOR_CALL_SUCCESS, COMPLETED, FAILED, COMPENSATING};

    public record WalletAccountRequest(
        @NotBlank String userId, 
        @NotNull ComplianceTier complianceTier, 
        @NotNull String currency) {}

    public record WalletAccountResponse(String walletId, String ledgerAccountId, String userId, String currency, String status, Instant createdAt) {}

    public record FundingIntentRequest(String walletId, BigDecimal amount, String currency, String paymentMethod, String provider) {}
    public record FundingIntentResponse(String fundingId, String walletId, BigDecimal amount, String currency, FundingStatus status, String clientSecret, String redirectUrl) {}

    public record P2PRequest(String senderWalletId, String receiverWalletId, BigDecimal amount, String currency, String narration) {}
    public record P2PResponse(String transactionId, String senderWalletId, String receiverWalletId, BigDecimal amount, BigDecimal feeCharged) {}

    public record VasPurchaseRequest(String walletId, ServiceType serviceType, String provider, BigDecimal amount, String destinationIdentifier) {}
    public record PaymentIntentResult(String reference, String clientSecret, String redirectUrl) {}
    public record LedgerTransactionResult(String transactionId) {}
    public record UtilityPurchaseResult(boolean success, String providerReference) {}
    public record Activity(String userId, String type, BigDecimal amount) {}
    public record LedgerTransaction(String id, String type, BigDecimal amount) {}
    public record SagaRequest(String walletId, String merchantId, BigDecimal amount) {}
    public record WebhookResult(String ledgerTransactionId) {}
}
