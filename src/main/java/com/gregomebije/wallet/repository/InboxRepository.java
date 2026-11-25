package com.gregomebije.wallet.repository;

import com.gregomebije.wallet.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InboxRepository extends JpaRepository<Inbox, InboxId> {
    @Modifying
    //@Modifying tells Spring Data JPA that this query changes rows 
    // (INSERT/UPDATE/DELETE) rather than fetching data, and returns 
    // an int representing how many rows were affected
    //If (tenantId, messageId) is new, PostgreSQL inserts it and returns 1. 
    // If it already exists, PostgreSQL silently ignores it and returns 0

    @Query(value = """
            INSERT INTO inbox (user_id, message_id)
            VALUES (:tenantId, :messageId)
            ON CONFLICT (user_id, message_id) DO NOTHING
            """, nativeQuery = true)
    int insertIgnoreConflict(@Param("userId") UUID userId, @Param("messageId") String messageId);
}