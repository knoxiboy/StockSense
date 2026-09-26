package com.stocksense.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findFirstByEmailIgnoreCaseAndUsedFalseOrderByCreatedAtDesc(String email);
    List<PasswordResetToken> findAllByEmailIgnoreCase(String email);
}
