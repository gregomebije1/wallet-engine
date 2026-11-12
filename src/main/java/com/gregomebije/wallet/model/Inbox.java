package com.gregomebije.wallet.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.gregomebije.wallet.model.Models.*;

@Entity
@Table(name = "inbox")

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inbox {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;
    
    @Column(name = "event_id", columnDefinition = "TEXT", nullable = false)
    private String eventId;

    @CreationTimestamp
    @Column(name = "received_at", nullable = false, updatable = false)
    private OffsetDateTime receivedAt;

}
