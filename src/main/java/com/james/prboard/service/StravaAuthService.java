package com.james.prboard.service;

import com.james.prboard.config.StravaConfig;
import com.james.prboard.domain.StravaOAuthState;
import com.james.prboard.domain.StravaSession;
import com.james.prboard.domain.User;
import com.james.prboard.model.StravaTokenResponseDto;
import com.james.prboard.repository.StravaOAuthStateRepository;
import com.james.prboard.repository.StravaSessionRepository;
import com.james.prboard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class StravaAuthService {

    private static final int STATE_TOKEN_BYTES = 32;
    private static final int STATE_EXPIRY_MINUTES = 10;

    private final StravaConfig stravaConfig;
    private final StravaOAuthStateRepository stravaOAuthStateRepository;
    private final StravaSessionRepository stravaSessionRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    private final SecureRandom secureRandom;
    private final PersonalRecordService personalRecordService;

    @Transactional
    public String generateAuthUrl(UUID userId) {
        String stateToken = generateStateToken();

        StravaOAuthState state = new StravaOAuthState();
        state.setStateToken(stateToken);
        state.setUserId(userId);
        state.setExpiresAt(OffsetDateTime.now().plusMinutes(STATE_EXPIRY_MINUTES));
        stravaOAuthStateRepository.save(state);

        return UriComponentsBuilder
                .fromUriString(stravaConfig.getBaseUrl() + "/oauth/authorize")                .queryParam("client_id", stravaConfig.getClientId())
                .queryParam("redirect_uri", stravaConfig.getRedirectUri())
                .queryParam("response_type", "code")
                .queryParam("approval_prompt", "auto")
                .queryParam("scope", "activity:read_all")
                .queryParam("state", stateToken)
                .build()
                .toUriString();
    }

    @Transactional
    public void handleCallback(String code, String stateToken) {
        StravaOAuthState state = stravaOAuthStateRepository
                .findByStateToken(stateToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid OAuth state"));

        if (state.isExpired()) {
            stravaOAuthStateRepository.delete(state);
            throw new IllegalArgumentException("OAuth state has expired — please try connecting Strava again");
        }

        stravaOAuthStateRepository.delete(state);

        User user = userRepository.findByUserId(state.getUserId())
                .orElseThrow(() -> new IllegalStateException("User not found for OAuth state"));

        StravaTokenResponseDto tokenResponse = exchangeCode(code);

        log.info("Exchanging auth code for token for user {}", user.getUserId());

        StravaSession session = stravaSessionRepository
                .findByUser(user)
                .orElseGet(() -> {
                    StravaSession s = new StravaSession();
                    s.setUser(user);
                    return s;
                });

        boolean isFirstConnect = session.getId() == null;

        session.setAccessToken(tokenResponse.getAccessToken());
        session.setRefreshToken(tokenResponse.getRefreshToken());
        session.setExpiresAt(tokenResponse.getExpiresAt());
        stravaSessionRepository.save(session);

        log.info("Strava session persisted for user {}", user.getUserId());

        // On first connect, recalculate all PRs after the user syncs their history.
        // We don't do it here since no activities exist yet — the sync triggers it
        // via updatePrsForNewActivities. But if they reconnect an existing account
        // (e.g. revoked and re-authorised), recalculate to catch anything missed.
        if (!isFirstConnect) {
            personalRecordService.recalculateAllPrs(user.getUserId());
        }
    }

    private StravaTokenResponseDto exchangeCode(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", stravaConfig.getClientId());
        body.add("client_secret", stravaConfig.getClientSecret());
        body.add("code", code);
        body.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        return restTemplate.postForObject(
                stravaConfig.getBaseUrl() + "/oauth/token",
                request,
                StravaTokenResponseDto.class
        );
    }

    private String generateStateToken() {
        byte[] bytes = new byte[STATE_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Scheduled(fixedRateString = "PT1H")
    @Transactional
    public void cleanUpExpiredStates() {
        log.debug("Cleaning up expired Strava OAuth states");
        stravaOAuthStateRepository.deleteExpired(OffsetDateTime.now());
    }
}