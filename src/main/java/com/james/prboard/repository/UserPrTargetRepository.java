package com.james.prboard.repository;

import com.james.prboard.domain.UserPrTarget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserPrTargetRepository extends JpaRepository<UserPrTarget, Integer> {

    List<UserPrTarget> findByUserIdAndDisciplineId(UUID userId, Short disciplineId);

    List<UserPrTarget> findByUserId(UUID userId);

    Optional<UserPrTarget> findByUserIdAndDisciplineIdAndDistanceMetres(
            UUID userId, Short disciplineId, BigDecimal distanceMetres);
}