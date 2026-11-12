package com.gregomebije.wallet.model;

import jakarta.persistence.*;
import lombok.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.AssertTrue;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.gregomebije.wallet.model.Models.*;

@Entity
@Table(name = "wallets")
@Check(name = "ck_entry_one_side",
    constraints = "(available >= 0 AND held >= 0)")

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;
    
    //@Column(name = "wallet_id", nullable = false, unique = true, length = 32)
    //private String walletId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Column(name = "ledger_account_id", nullable = false, unique = true, length = 64)
    private String ledgerAccountId;

    @Min(0)
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private long available = 0L;

    @Min(0)
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private long held = 0L;

    @Column(length = 3, nullable = false, updatable = false)
    private String currency;

    @Builder.Default
    @Column(nullable = false)
    private int version = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, updatable = false)
    private OffsetDateTime updatedAt;


}
