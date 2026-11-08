package com.gregomebije.wallet.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.gregomebije.wallet.model.Models.*;

@Entity
@Table(name = "services")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // Required by JPA, protected prevents direct instantiation
@AllArgsConstructor
@Builder // Optional: Gives you a clean fluent API to create objects without setters

public class Service {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING) // Explicitly maps Java text names to Postgres Enum items
    @Column(name = "name", nullable = false, unique = true)
    private ServiceType name;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_providers_id", nullable = false)
    private ServiceProvider serviceProvider;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime updatedAt;

    public Service(ServiceType name, ServiceProvider serviceProvider) {
        this.name = name;
        this.serviceProvider = serviceProvider;
    }

}
