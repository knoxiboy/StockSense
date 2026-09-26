package com.stocksense.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RevokedTokenRepository extends JpaRepository<RevokedToken, Long> {
    boolean existsByTokenSignature(String tokenSignature);
    void deleteByExpiryTimeLessThan(Long currentTimeSeconds);
}
