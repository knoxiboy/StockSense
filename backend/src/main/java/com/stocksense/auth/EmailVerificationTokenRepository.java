package com.stocksense.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    Optional<EmailVerificationToken> findFirstByEmailIgnoreCaseAndUsedFalseOrderByCreatedAtDesc(String email);
    void deleteByEmailIgnoreCase(String email);
}
