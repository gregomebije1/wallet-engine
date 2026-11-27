package com.gregomebije.wallet.service;

import tools.jackson.databind.json.JsonMapper;
import java.util.List;
import java.util.ArrayList;
import java.util.Map; 
import java.util.UUID;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import com.gregomebije.wallet.dto.Models.*;
import com.gregomebije.wallet.model.*;
import com.gregomebije.wallet.repository.*;
import com.gregomebije.wallet.service.LedgerTokenService;
import com.gregomebije.wallet.config.JwtService;
import com.gregomebije.wallet.integration.LedgerClient;
import com.gregomebije.wallet.config.*;

@Service
//  Orchestrating the Workflow in a Wallet Service
public class AuthenticationService {

    private final OutboxRepository outboxRepository;
    private final AccountRepository accountRepository;
    private final InboxRepository inboxRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final UserRepository userRepository;
    private final RestClient gatewayRestClient;
    private final LedgerClient ledgerClient;
    private final LedgerTokenService ledgerTokenService;
    private final JsonMapper jsonMapper;
    private final String ledgerServiceTenantId;
    private final PasswordEncoder passwordEncoder;
    private final String gatewayChargesUrl;
    private final ServiceProviderRepository serviceProviderRepository;
    private final JwtService jwtService;
	private final AuthenticationManager authenticationManager;
   

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

    public AuthenticationService (
            OutboxRepository outboxRepository,
            AccountRepository accountRepository,
            InboxRepository inboxRepository,
            WalletRepository walletRepository,
            WalletTransactionRepository walletTransactionRepository,
            UserRepository userRepository,
            @Qualifier("gatewayRestClient") RestClient gatewayRestClient,
            LedgerClient ledgerClient,
            LedgerTokenService ledgerTokenService,
            JsonMapper jsonMapper, 
            @Value("${services.ledger.tenant-id}") String ledgerServiceTenantId,
            PasswordEncoder passwordEncoder,
            @Value("${services.gateway.charges.url}") String gatewayChargesUrl,
            ServiceProviderRepository serviceProviderRepository,
            JwtService jwtService, 
            AuthenticationManager authenticationManager) {
        this.outboxRepository = outboxRepository;
        this.accountRepository = accountRepository;
        this.inboxRepository = inboxRepository;
        this.walletRepository = walletRepository;
        this.userRepository = userRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.gatewayRestClient = gatewayRestClient;
        this.ledgerClient = ledgerClient;
        this.ledgerTokenService = ledgerTokenService;
        this.jsonMapper = jsonMapper;
        this.ledgerServiceTenantId = ledgerServiceTenantId;
        this.passwordEncoder = passwordEncoder;
        this.gatewayChargesUrl = gatewayChargesUrl;  
        this.serviceProviderRepository = serviceProviderRepository;
        this.jwtService = jwtService;
		this.authenticationManager = authenticationManager;
        
    }

    @Transactional
    public String setup() {
        String token = ledgerTokenService.getTokenForTenant(this.ledgerServiceTenantId).toString();

        List<CreateAccountRequest> specs = List.of(
            new CreateAccountRequest("W010", "Settlement Bank", AccountType.ASSET, "USD"),
            new CreateAccountRequest("W100", "Customer Available", AccountType.LIABILITY, "USD"),

            // A line may carry a sub_ref: the customer's wallet id
            // That gives per-customer balances inside one account (a sub-ledger) 
            // without creating an account per customer.
            new CreateAccountRequest("W110", "Customer Held", AccountType.LIABILITY, "USD"), 
            new CreateAccountRequest("W200", "Fee Revenue", AccountType.REVENUE, "USD"),
            new CreateAccountRequest("W210", "Gateway Fees Expense", AccountType.EXPENSE, "USD"),
            new CreateAccountRequest("W300", "Funding Receivable", AccountType.ASSET, "USD"),
            new CreateAccountRequest("W400", "Vendor Payout Clearing", AccountType.ASSET, "USD")
        );

        List<Account> createdAccounts = new ArrayList<>();
        for (CreateAccountRequest spec : specs) {
            try {
                String response = ledgerClient.createAccount(token, spec);
                // Ensure response parsing strips any JSON formatting if applicable
                String rawId = response.substring(response.indexOf(":") + 1)
                                    .replaceAll("[\"{}]", "")
                                    .trim();

                Account account = Account.builder()
                    .accountId(rawId)
                    .name(spec.name())
                    .type(spec.type())
                    .currency(spec.currency())
                    .isActive(true)
                    .build();

                createdAccounts.add(account);
            } catch (HttpClientErrorException.Unauthorized ex) {
                ledgerTokenService.evict(ledgerServiceTenantId);
                logger.error("Unauthorized error in AuthenticationService.setup() for tenant {}", ledgerServiceTenantId, ex);
                throw new IllegalStateException("Failed to authenticate with ledger service", ex);
            }
        }
        accountRepository.saveAll(createdAccounts);
        return "Setup complete";
    }

    @Transactional
    public String register(CreateUserRequest request) throws RuntimeException {
         if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already exists");
        }
        User user = User.builder()
            .email(request.email())
            .password(passwordEncoder.encode(request.password()))
            .role("USER")
            .build();
        
        User savedUser = userRepository.save(user);

        Wallet wallet = Wallet.builder()
            .user(savedUser).currency("USD").build();
        walletRepository.save(wallet);

        return wallet.getId().toString();
    }

    @Transactional(readOnly = true)
    public LoginResponse login(WalletLoginRequest request) throws IllegalArgumentException {
        /*User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        //return jwtProvider.generateToken(userTenant.getTenant().getId(), userTenant.getUser().getEmail());
        return new LoginResponse("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb2huZG9lQG1hbmdpdGFsLmNvbSI");
        */
        UsernamePasswordAuthenticationToken creds = new UsernamePasswordAuthenticationToken(
				request.email(), request.password());

		Authentication auth = authenticationManager.authenticate(creds);
		
		String jwts = jwtService.getToken(auth.getName(), auth.getAuthorities());
		
        return new LoginResponse(jwts);

        /*
		return ResponseEntity.ok()
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwts)
				.header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "Authorization")
				.build();
        */

    }
    
}