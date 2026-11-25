package com.gregomebije.wallet.repository;

import com.gregomebije.wallet.model.Bank;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface BankRepository extends JpaRepository <Bank, Long> {
}
