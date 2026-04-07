package com.james.prboard.repository.wahoo;

import com.james.prboard.domain.wahoo.WahooOAuthState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface WahooOAuthStateRepository extends JpaRepository<WahooOAuthState, Long> {

    Optional<WahooOAuthState> findByStateToken(String stateToken);

    @Modifying
    @Transactional
    void deleteByExpiresAtBefore(OffsetDateTime now);
}