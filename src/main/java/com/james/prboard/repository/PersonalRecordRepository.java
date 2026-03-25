package com.james.prboard.repository;

import com.james.prboard.domain.PersonalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PersonalRecordRepository extends JpaRepository<PersonalRecord, Long> {

    List<PersonalRecord> findByUserIdAndDisciplineId(UUID userId, Short disciplineId);

    List<PersonalRecord> findByUserId(UUID userId);

    // Used by PR update service — finds the current PR for a specific type + target combo
    Optional<PersonalRecord> findByUserIdAndDisciplineIdAndPrTypeAndTargetId(
            UUID userId, Short disciplineId, String prType, Integer targetId);

    // Finds non-targeted PRs (LONGEST_ACTIVITY, BEST_POWER, HEAVIEST_SESSION)
    Optional<PersonalRecord> findByUserIdAndDisciplineIdAndPrTypeAndTargetIsNull(
            UUID userId, Short disciplineId, String prType);

    // All PRs for a user's selected disciplines — used by the stats endpoint
    @Query("""
            SELECT pr FROM PersonalRecord pr
            JOIN FETCH pr.discipline
            LEFT JOIN FETCH pr.target
            JOIN FETCH pr.activity
            WHERE pr.userId = :userId
            ORDER BY pr.discipline.id, pr.prType, pr.achievedAt DESC
            """)
    List<PersonalRecord> findAllWithDetailsByUserId(UUID userId);
}