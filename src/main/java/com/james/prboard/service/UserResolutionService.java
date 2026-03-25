package com.james.prboard.service;

import com.james.prboard.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserResolutionService {

    private final UserPersistenceService userPersistenceService;

    public User resolveCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        Objects.requireNonNull(authentication, "No authentication in security context");

        if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new IllegalStateException("Principal is not a JWT — check security configuration");
        }

        return userPersistenceService.resolveOrCreate(jwt.getSubject(), jwt.getClaimAsString("email"));
    }
}