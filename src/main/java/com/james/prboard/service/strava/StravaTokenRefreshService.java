package com.james.prboard.service.strava;

import com.james.prboard.config.StravaConfig;
import com.james.prboard.domain.strava.StravaSession;
import com.james.prboard.model.strava.StravaTokenResponseDto;
import com.james.prboard.repository.strava.StravaSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class StravaTokenRefreshService {

    private final StravaConfig stravaConfig;
    private final RestTemplate restTemplate;
    private final StravaSessionRepository stravaSessionRepository;

    public StravaSession getValidSession(UUID userId) {
        StravaSession session = stravaSessionRepository.findByUserUserId(userId)
                .orElseThrow(() -> new IllegalStateException(
                        "No Strava session found for user " + userId + " — please re-authenticate"));

        if (session.isExpired()) {
            log.info("Strava token expired for user {} — refreshing", userId);
            return refresh(session);
        }

        return session;
    }

    private StravaSession refresh(StravaSession session) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", stravaConfig.getClientId());
        params.add("client_secret", stravaConfig.getClientSecret());
        params.add("grant_type", "refresh_token");
        params.add("refresh_token", session.getRefreshToken());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        ResponseEntity<StravaTokenResponseDto> response = restTemplate.postForEntity(
                stravaConfig.getBaseUrl() + "/oauth/token",
                request,
                StravaTokenResponseDto.class
        );

        StravaTokenResponseDto body = response.getBody();

        if (body == null) {
            log.error("Received null response from Strava token refresh");
            throw new IllegalStateException("Failed to refresh Strava token");
        }

        session.setAccessToken(body.getAccessToken());
        session.setRefreshToken(body.getRefreshToken());
        session.setExpiresAt(body.getExpiresAt());

        stravaSessionRepository.save(session);
        log.info("Strava token refreshed successfully for user {}", session.getUser().getUserId());

        return session;
    }
}