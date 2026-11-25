package com.gregomebije.wallet.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.validation.constraints.Min;

import com.gregomebije.wallet.dto.Models.*;

@Entity
@Table(name = "wallet_transactions")

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;
    
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 255)
    private String idempotencyKey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false, updatable = false)
    private Wallet wallet;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(columnDefinition = "service_type", nullable = false)
    private ServiceType serviceType;

    @Min(0)
    @Column(nullable = false, updatable = false)
    private long amount;

    @Min(0)
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private long fee = 0L;

    private int counterpartyWallet;
    
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(columnDefinition = "service_status", nullable = false)
    private ServiceStatus serviceStatus;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public WalletTransaction(String idempotencyKey, Wallet wallet, ServiceType serviceType, 
        long amount, long fee, ServiceStatus serviceStatus) {
        this.idempotencyKey = idempotencyKey;
        this.wallet = wallet;
        this.serviceType = serviceType;
        this.amount = amount;
        this.fee = fee;
        this.serviceStatus = serviceStatus;
    }

}
