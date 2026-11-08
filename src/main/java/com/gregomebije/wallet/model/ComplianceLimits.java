package com.gregomebije.wallet.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.Checks;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.gregomebije.wallet.model.Models.*;

@Entity
@Table(name = "compliance_limits")

@Checks({
    @Check(name = "ck_transaction_limit", constraints = "single_transaction_limit > 0"),
    @Check(name = "ck_velocity_limit", constraints = "daily_velocity_limit > 0")
})

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder 

public class ComplianceLimits {

    @Id
    @Enumerated(EnumType.STRING) // Explicitly maps Java text names to Postgres Enum items
    @Column(name = "tier")
    private ComplianceTier tier;

    @Column(name = "single_transaction_limit", nullable = false)
    private long singleTransactionLimit;

    @Column(name = "daily_velocity_limit", nullable = false)
    private long dailyVelocityLimit; 

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, updatable = false)
    private OffsetDateTime updatedAt;

}