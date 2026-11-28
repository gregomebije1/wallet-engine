package com.gregomebije.wallet.model;

import lombok.*;
import jakarta.persistence.*;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "inbox")
@Builder
public class Inbox {
    @Id
    @Column(name = "message_id", nullable = false, unique = true)
    private UUID messageId;

    //@Generated(event = EventType.INSERT)
    @Builder.Default
    @Column(name = "received_at", nullable = false)
    private OffsetDateTime receivedAt = OffsetDateTime.now();;

}
