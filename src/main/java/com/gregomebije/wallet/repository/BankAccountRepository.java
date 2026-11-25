package com.gregomebije.wallet.repository;

import com.gregomebije.wallet.model.BankAccount;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface BankAccountRepository extends JpaRepository <BankAccount, Long> {
}
