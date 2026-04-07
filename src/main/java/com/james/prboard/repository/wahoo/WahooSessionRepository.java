package com.james.prboard.repository.wahoo;

import com.james.prboard.domain.User;
import com.james.prboard.domain.wahoo.WahooSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WahooSessionRepository extends JpaRepository<WahooSession, Long> {

    Optional<WahooSession> findByUserUserId(UUID userId);
    Optional<WahooSession> findByUser(User user);
}