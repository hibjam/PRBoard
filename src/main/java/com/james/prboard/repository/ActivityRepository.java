package com.james.prboard.repository;

import com.james.prboard.domain.Activity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ActivityRepository extends JpaRepository<Activity, Long> {

    // Used by stats service — excludes duplicates, no discipline filter
    List<Activity> findByUserUserIdAndCanonicalActivityIsNull(UUID userId);

    // Used by stats service — loads all activities with child data in one query,
    // avoiding N+1 when iterating disciplines
    @EntityGraph(attributePaths = {"discipline", "enduranceData", "weightliftingSession"})
    @Query("""
            SELECT a FROM Activity a
            WHERE a.user.userId = :userId
              AND a.canonicalActivity IS NULL
            """)
    List<Activity> findAllWithDataByUserUserId(UUID userId);

    // Used by stats service — excludes duplicates, filtered to one discipline
    List<Activity> findByUserUserIdAndDisciplineIdAndCanonicalActivityIsNull(UUID userId, Short disciplineId);

    // Used by recent activities — excludes duplicates, paged
    List<Activity> findByUserUserIdAndCanonicalActivityIsNullOrderByStartTimeDesc(UUID userId, Pageable pageable);

    // Used by Strava sync — checks for existing activity before saving
    Optional<Activity> findByExternalIdAndSource(String externalId, String source);

    @EntityGraph(attributePaths = {"discipline", "enduranceData", "weightliftingSession"})
    @Query("SELECT a FROM Activity a WHERE a.id IN :ids")
    List<Activity> findAllWithDataByIds(List<Long> ids);
    @Query("""
            SELECT a FROM Activity a
            WHERE a.user.userId = :userId
              AND a.discipline.id = :disciplineId
              AND a.startTime BETWEEN :from AND :to
              AND a.canonicalActivity IS NULL
            """)
    List<Activity> findDuplicateCandidates(
            UUID userId,
            Short disciplineId,
            OffsetDateTime from,
            OffsetDateTime to
    );
}