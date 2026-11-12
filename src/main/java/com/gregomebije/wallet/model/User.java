package com.gregomebije.wallet.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;
import java.time.OffsetDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.gregomebije.wallet.model.Models.*;


@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // Required by JPA, protected prevents direct instantiation
@AllArgsConstructor
@Builder // Optional: Gives you a clean fluent API to create objects without setters

public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, unique = true)
    private UUID id;

    //@Column(nullable = false, unique = true)
    //private String user_reference;

    //@Builder.Default
    //@Enumerated(EnumType.STRING)
    //@Column(nullable = false)
    //private ComplianceTier complianceTier = ComplianceTier.TIER_1;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(columnDefinition = "account_status", nullable = false)
    private AccountStatus status = AccountStatus.PENDING_VERIFICATION; 

    //private String password;
    //private String firstname;
    //private String lastname;

    @Column(nullable = false, unique = true)
    private String email;

    //@Column(nullable = false, unique = true)
    //private String phoneNumber;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private OffsetDateTime updatedAt;

}
