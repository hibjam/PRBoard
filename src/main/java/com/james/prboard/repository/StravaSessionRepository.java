package com.james.prboard.repository;

import com.james.prboard.domain.StravaSession;
import com.james.prboard.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StravaSessionRepository extends JpaRepository<StravaSession, Long> {

    Optional<StravaSession> findByUserUserId(UUID userId);
    Optional<StravaSession> findByUser(User user);
}