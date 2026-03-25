package com.james.prboard.repository;

import com.james.prboard.domain.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByAuth0Id(String auth0Id);

    Optional<User> findByEmail(String email);

    // Fetches disciplines in the same query — avoids a second round trip
    // when any service method needs to access user.getDisciplines()
    @EntityGraph(attributePaths = "disciplines")
    Optional<User> findByUserId(UUID userId);
}