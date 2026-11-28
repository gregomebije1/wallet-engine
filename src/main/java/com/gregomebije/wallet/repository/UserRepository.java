package com.gregomebije.wallet.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.gregomebije.wallet.model.User;
import com.gregomebije.wallet.dto.Models.*;

@Repository
public interface UserRepository extends JpaRepository <User, Long> {

    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);

    @Override
    Page<User> findAll(Pageable pageable);
}
