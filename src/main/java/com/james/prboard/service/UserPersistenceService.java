package com.james.prboard.service;

import com.james.prboard.domain.User;
import com.james.prboard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class UserPersistenceService {

    private final UserRepository userRepository;

    public User resolveOrCreate(String auth0Id, String email) {
        return userRepository.findByAuth0Id(auth0Id)
                .orElseGet(() -> resolveByEmailOrCreate(auth0Id, email));
    }

    private User resolveByEmailOrCreate(String auth0Id, String email) {
        if (email != null) {
            return userRepository.findByEmail(email)
                    .map(existingUser -> {
                        log.info("Linking auth0Id to existing user with email: {}", email);
                        existingUser.setAuth0Id(auth0Id);
                        return userRepository.save(existingUser);
                    })
                    .orElseGet(() -> createUser(auth0Id, email));
        }
        return createUser(auth0Id, auth0Id);
    }

    private User createUser(String auth0Id, String email) {
        log.info("Creating new user for auth0Id: {}", auth0Id);
        User user = new User();
        user.setAuth0Id(auth0Id);
        user.setEmail(email);
        user.setCreatedAt(OffsetDateTime.now());
        return userRepository.save(user);
    }
}