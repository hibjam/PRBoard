package com.james.prboard.repository;

import com.james.prboard.domain.Discipline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DisciplineRepository extends JpaRepository<Discipline, Short> {

    List<Discipline> findByActiveTrue();

    Optional<Discipline> findByCode(String code);

    // Returns only disciplines the user has selected on their profile
    @Query("""
            SELECT d FROM Discipline d
            JOIN d.users u
            WHERE u.userId = :userId
              AND d.active = true
            """)
    List<Discipline> findByUserUserId(UUID userId);
}