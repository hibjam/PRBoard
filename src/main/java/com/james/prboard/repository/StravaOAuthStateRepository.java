package com.james.prboard.repository;

import com.james.prboard.domain.StravaOAuthState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface StravaOAuthStateRepository extends JpaRepository<StravaOAuthState, Long> {

    Optional<StravaOAuthState> findByStateToken(String stateToken);

    @Modifying
    @Transactional
    @Query("DELETE FROM StravaOAuthState s WHERE s.expiresAt < :now")
    void deleteExpired(OffsetDateTime now);
}