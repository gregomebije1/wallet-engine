package com.gregomebije.wallet.model;

import jakarta.persistence.*;
import lombok.*;
import jakarta.validation.constraints.Min;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.OffsetDateTime;
import java.util.UUID;


import com.gregomebije.wallet.dto.Models.*;

@Entity
@Table(name = "wallets")
@Check(constraints = "available >= 0 AND held >= 0")

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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Column(name = "ledger_account_id", length = 64)
    private String ledgerAccountId;

    @Min(0)
    @Column(nullable = false)
    @Builder.Default
    private long available = 0L;

    @Min(0)
    @Column(nullable = false)
    @Builder.Default
    private long held = 0L;

    @Column(length = 3, nullable = false, updatable = false)
    private String currency;


    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

}
