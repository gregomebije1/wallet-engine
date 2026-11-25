package com.gregomebije.wallet.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;


public final class Models {
    private Models() {}
 
    //Ledger DTOs
    public enum LedgerLineType { DEBIT, CREDIT }
    public enum AccountType { 
          ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE;
          
           /** ASSET/EXPENSE are debit; everything else is credit. */
          public boolean isDebitNormal() {
               return this == ASSET || this == EXPENSE;
          }
     }

    public record CreateAccountRequest (  String accountNumber,  String name,  AccountType type,  String currency ) {}

    public record CreateTransactionRequest ( String description,  String idempotencyKey,  List<LedgerLine> lines ) {}

    // A line may carry a sub_ref: the customer wallet Id or merchant id
    // That gives per-customer balances inside one account (a sub-ledger) 
    // without creating an account per customer.
    public record LedgerLine(UUID accountId, String subRef, LedgerLineType ledgerLineType,  long debit, long credit) {}

    
    //Error messages
    public record ErrorMessage (
        String code,  //"invalid_amount", 
        String message  //"amount must be positive"
    ) {}

    public record ErrorResponse (
        ErrorMessage error 
    ) {}
   

    //Onboarding
    public record CreateUserRequest (String email, String password) {}
    public record LoginRequest (  String tenantId,  String email,  String password ) {}
    public record LoginResponse (  String accessToken ) {}

    public record WalletLoginRequest (  String tenantId,  String email,  String password ) {}
    
    //Transactions
    public record DepositRequest(
                @NotBlank String walletId, 
                @NotNull long amount,
                @NotBlank String currency,
                @NotBlank String cardToken) {}



    public record DepositResponse (
            String transactionId, //"d0000000-…-0001"
            String type, //"DEPOSIT", 
            String status,  //"PENDING",
            long amount,   //10000 
            long fee,      //300, 
            long totalCharged,  //10300, 
            String createdAt //"2026-11-20T10:15:00Z" 
    ) {}
   
    public record TransactionResponse (
        String transaction_id,  //"d0000000-…-0001", 
        String type,            //"DEPOSIT", 
        String status,           //: "SUCCESS",
        long amount,             //: 10000, 
        long fee,                // 300, 
        String wallet_id        //"a1000000-…-0001" }
    ) {}

    public record TransferRequest (
            @NotBlank String senderWalletId,
            @NotBlank String receiverWalletId,
            @NotBlank String currency,
            @NotNull long amount) {}
    
    public record TransferResponse (
            String transactionId,   //d0000000-…-0002
            String type,        //P2P
            String status,      //Status
            @NotNull long amount) {}

    public record Vendor (
        String type,    //airtime
        String reference    //0770900123
    ) {}

    public record PurchaseRequest(
            @NotBlank String walletId, 
            @NotNull long amount,
            @NotBlank String currency,
            Vendor vendor) {}

    public record PurchaseResponse(
       String transactionId,
       String type, //VAS
       String status, //PENDING
       long held // 1000
    ){}
    
    public record GatewayRequest (
        String type,    //charge.captured", 
        String charge_id   // "d0000000-…-0001"
    ) {}

     public record WalletResponse (
        String wallet_id,   //"a1000000-…-0001", 
        long available,    //8000, 
        long held          //},
    ) {}

    public record WebhookRequest (
        String type, //charge.captured, settlement.paid
        //payout.succeeded, payout.failed
        String chargeId,
        long gross,
        long fee,
        long net,
        String payoutId,
        String reason
    ){}

    public record WebhookResponse (
         String received, //true
         String duplicate  //true
    ){}
    
    public record WalletBalanceResponse (
        String AsOf,    // "2026-11-20T10:20:00Z",
        List<WalletResponse> wallets
     ) {}
    
}
