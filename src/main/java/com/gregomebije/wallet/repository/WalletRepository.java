package com.gregomebije.wallet.repository;

//import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
//import java.util.List;
//import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.core.RowMapper;
import java.sql.Timestamp;

import com.gregomebije.wallet.dto.Models.DepositResponse;
import com.gregomebije.wallet.dto.Models.DepositRequest;
import com.gregomebije.wallet.model.Wallet;


@Repository
//public interface WalletRepository extends JpaRepository<Wallet, UUID> {
public class WalletRepository {

    //Optional<Wallet> findById(UUID walletId);
    private final JdbcClient jdbcClient;

    public WalletRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Wallet save(Wallet wallet) {
        if (wallet.getId() == null) {
            wallet.setId(UUID.randomUUID()); 
        }

        String sql = """
            INSERT INTO wallets (id, user_id, currency, available)
            VALUES (:id, :userId, :currency, :available)
            """;

        jdbcClient.sql(sql)
                .param("id", wallet.getId())
                .param("userId", wallet.getUser().getId())
                .param("currency", wallet.getCurrency())
                .param("available", wallet.getAvailable())
                .update();

        return wallet;
    }

    public DepositResponse createDeposit(String idempotencyKey, DepositRequest request, long fee) {
        String sql = "SELECT * FROM fn_create_deposit( :idempKey, :walletId, :amount, :fee)";

        return jdbcClient.sql(sql)
                .param("idempKey", UUID.fromString(idempotencyKey))
                .param("walletId", UUID.fromString(request.walletId()))
                .param("amount", request.amount())
                .param("fee", fee)
                .query((rs, rowNum) -> {
                    long amt = rs.getLong("amount");
                    long f = rs.getLong("fee");
                    Timestamp createdAt = rs.getTimestamp("created_at");
                    return new DepositResponse(
                            rs.getString("idempotency_key"),
                            rs.getString("wallet_id"),
                            rs.getString("service_type"),
                            amt,
                            f,
                            rs.getString("service_status"),
                            //createdAt != null ? createdAt.toInstant() : null
                            createdAt.toString()
                    );
                })
                .single();
    }

    public String captureDeposit(String idempotencyKey, String chargeId) {
        return jdbcClient.sql("SELECT fn_capture_deposit(:msgId, :chargeId)")
                .param("msgId", UUID.fromString(idempotencyKey))
                .param("chargeId", UUID.fromString(chargeId))
                .query(String.class)
                .single(); // Returns "CAPTURED" or "DUPLICATE"
    }

    public String settleGateway(String idempotencyKey, String chargeId, long gross, long fee, long net) {
        String sql = "SELECT fn_settle_gateway(:msgId, :chargeId, :gross, :fee, :net)";

        return jdbcClient.sql(sql)
            .param("msgId", UUID.fromString(idempotencyKey))
            .param("chargeId", UUID.fromString(chargeId))
            .param("gross", gross)
            .param("fee", fee)
            .param("net", net)
            .query(String.class)
            .single(); // Returns "SETTLED" or "DUPLICATE"
}


}
