package com.gregomebije.wallet.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "service_providers")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // Required by JPA, protected prevents direct instantiation
@AllArgsConstructor
@Builder // Optional: Gives you a clean fluent API to create objects without setters

public class ServiceProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;
    
    @Column(nullable = false)
    private BigDecimal cost;  //store in kobo

    @Column(nullable = false)
    private BigDecimal revenue;

    @Column(nullable = false)
    private String type; //percent or amount

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    public ServiceProvider(String name, BigDecimal cost, BigDecimal revenue) {
        this.name = name;
        this.cost = cost;
        this.revenue = revenue;
    }
    public long getCost(long amount) {
        BigDecimal result; 

        if ("percent".equals(type)) {
            result = BigDecimal.valueOf(amount)
                 .multiply(cost)
                .setScale(2, RoundingMode.HALF_UP);
        } else {
            result = cost;
        }

        return result
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
    }

    public long getRevenue(long amount) {
        BigDecimal result; 

        if ("percent".equals(type)) {
            result = BigDecimal.valueOf(amount)
                 .multiply(revenue)
                .setScale(2, RoundingMode.HALF_UP);
        } else {
            result = revenue;
        }

        return result
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
    }

}
