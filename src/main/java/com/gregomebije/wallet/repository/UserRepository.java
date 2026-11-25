package com.gregomebije.wallet.repository;

import com.gregomebije.wallet.model.User;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository <User, Long> {
    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);
}
