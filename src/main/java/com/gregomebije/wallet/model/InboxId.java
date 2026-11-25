package com.gregomebije.wallet.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Embeddable
//Composite Primary Key for Inbox
public class InboxId implements Serializable {
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "message_id")
    private String messageId;

    protected InboxId() {}
    public InboxId(UUID userId, String messageId) {
        this.userId = userId;
        this.messageId = messageId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InboxId that)) return false;
        return Objects.equals(userId, that.userId) && Objects.equals(messageId, that.messageId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, messageId);
    }
}