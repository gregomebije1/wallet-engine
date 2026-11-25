package com.gregomebije.wallet.repository;

import com.gregomebije.wallet.model.User;
import com.gregomebije.wallet.dto.Models.*;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository <User, Long> {
    //boolean existsByEmail(String email);
    //Optional<User> findByEmail(String email);

    //tells JPA to eagerly load the roles association in a single query 
    // for that specific query—without changing User.roles from FetchType.LAZY globally.
    @EntityGraph(attributePaths = "roles") 
    Optional<User> findByEmail(String email);

    @EntityGraph(attributePaths = "roles")
    Optional<User> findWithRolesById(Long id);

    @Override
    @EntityGraph(attributePaths = "roles")
    Page<User> findAll(Pageable pageable);

    boolean existsByEmail(String email);
    boolean existsByRolesName(ERole roleName);
}
