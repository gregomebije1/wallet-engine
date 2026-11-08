package com.gregomebije.wallet.service.account;

import com.gregomebije.wallet.model.Models.WalletAccountRequest;
import com.gregomebije.wallet.model.Models.WalletAccountResponse;
import org.springframework.stereotype.Service;
import java.time.Instant;

@Service
public class WalletAccountService {
    public WalletAccountResponse provision(WalletAccountRequest r) {
        return new WalletAccountResponse("wlt-test", "acc-test", r.userId(), r.currency(),
                "ACTIVE", Instant.now());
    }
}
