package com.gregomebije.wallet.repository;

import com.gregomebije.wallet.dto.Models.*;
import com.gregomebije.wallet.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(ERole name);
}