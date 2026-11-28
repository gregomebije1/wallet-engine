package com.gregomebije.wallet.utilities;

import com.gregomebije.wallet.repository.OutboxRepository;
import com.gregomebije.wallet.integration.LedgerClient;
import com.gregomebije.wallet.dto.Models.*;
import com.gregomebije.wallet.service.LedgerTokenService;
import com.gregomebije.wallet.model.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;

@Component
public class OutboxPoller {

    private static final Logger log = LoggerFactory.getLogger(OutboxPoller.class);
    private static final int BATCH_SIZE = 50;
    private static final int MAX_ATTEMPTS = 5;

    private final OutboxRepository outboxRepository;
    private final RestClient gatewayRestClient;
    private final LedgerClient ledgerClient;
    private final LedgerTokenService ledgerTokenService;
    private final JsonMapper jsonMapper;
    private final String tenantId;

    public OutboxPoller(
            OutboxRepository outboxRepository,
            @Qualifier("gatewayRestClient") RestClient gatewayRestClient,
            LedgerClient ledgerClient,
            LedgerTokenService ledgerTokenService,
            JsonMapper jsonMapper, 
            @Value("${services.ledger.tenant-id}") String tenantId) {
        this.outboxRepository = outboxRepository;
        this.gatewayRestClient = gatewayRestClient;
        this.ledgerClient = ledgerClient;
        this.ledgerTokenService = ledgerTokenService;
        this.jsonMapper = jsonMapper;
        this.tenantId = tenantId;
    }

    

    @Scheduled(fixedDelayString = "${outbox.poll-interval-ms:180000}") //500 milliseconds
    @Transactional
    public void pollAndDispatch() {
        List<Outbox> batch = outboxRepository.claimBatchForProcessing(BATCH_SIZE);
        for (Outbox message : batch) {
            try {
                dispatch(message);
                message.markSent();     // <--- 1. Changes status to SENT
            } catch (Exception ex) {
                log.warn("Outbox dispatch failed id={} tenant={} target={} path={}: {}",
                        message.getId(),  message.getTarget(), message.getPath(), ex.getMessage());
                message.markFailed(MAX_ATTEMPTS); //<--- 2. Increments attempts & sets DEAD_LETTER (or keeps PENDING)
            }
        }
        //Why Isn't outboxRepository.save(message) Called?
        //because of a JPA / Hibernate feature called Managed Entity Dirty Checking:
        /* Calling outboxRepository.save(message) explicitly wouldn't hurt 
          anything, because Spring Data JPA's save() simply calls 
          entityManager.merge(), which is a no-op on an already-managed 
          entity—it's just redundant inside a @Transactional method)
        */

    }

    private void dispatch(Outbox message) {
        switch (message.getTarget()) {
            case "gateway" -> dispatchWebhook(message);
            case "ledger" -> dispatchToLedger(message);
            default -> throw new IllegalArgumentException("Unknown outbox target: " + message.getTarget());
        }
    }

    private void dispatchWebhook(Outbox message) {
             gatewayRestClient.post()
                .uri(message.getPath())
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", message.getIdempotencyKey().toString())
                //3.header("X-Tenant-Id", message.getTenantId().toString())
                .body(message.getBody())
                .retrieve()
                .toBodilessEntity();
    }

    private void dispatchToLedger(Outbox message) {
        CreateTransactionRequest request = jsonMapper.convertValue(
                message.getBody(),
                CreateTransactionRequest.class
        );

        String token = ledgerTokenService.getTokenForTenant(tenantId);
        try {
            ledgerClient.postTransaction(token, request);
;
        } catch (HttpClientErrorException.Unauthorized ex) {
            ledgerTokenService.evict(tenantId);
            String refreshedToken = ledgerTokenService.getTokenForTenant(tenantId);
            ledgerClient.postTransaction(refreshedToken, request);
        }
    }
}