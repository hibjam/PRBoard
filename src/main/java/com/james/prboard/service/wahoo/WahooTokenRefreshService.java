package com.james.prboard.service.wahoo;

import com.james.prboard.config.WahooConfig;
import com.james.prboard.domain.wahoo.WahooSession;
import com.james.prboard.model.wahoo.WahooTokenResponseDto;
import com.james.prboard.repository.wahoo.WahooSessionRepository;
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
public class WahooTokenRefreshService {

    private final WahooConfig wahooConfig;
    private final RestTemplate restTemplate;
    private final WahooSessionRepository wahooSessionRepository;

    public WahooSession getValidSession(UUID userId) {
        WahooSession session = wahooSessionRepository.findByUserUserId(userId)
                .orElseThrow(() -> new IllegalStateException(
                        "No Wahoo session found for user " + userId + " — please re-authenticate"));

        if (session.isExpired()) {
            log.info("Wahoo token expired for user {} — refreshing", userId);
            return refresh(session);
        }

        return session;
    }

    private WahooSession refresh(WahooSession session) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", wahooConfig.getClientId());
        params.add("grant_type", "refresh_token");
        params.add("refresh_token", session.getRefreshToken());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        ResponseEntity<WahooTokenResponseDto> response = restTemplate.postForEntity(
                wahooConfig.getBaseUrl() + "/oauth/token",
                request,
                WahooTokenResponseDto.class
        );

        WahooTokenResponseDto body = response.getBody();

        if (body == null) {
            log.error("Received null response from Wahoo token refresh");
            throw new IllegalStateException("Failed to refresh Wahoo token");
        }

        long expiresAt = (System.currentTimeMillis() / 1000) + body.getExpiresIn();

        session.setAccessToken(body.getAccessToken());
        session.setRefreshToken(body.getRefreshToken());
        session.setExpiresAt(expiresAt);

        wahooSessionRepository.save(session);
        log.info("Wahoo token refreshed successfully for user {}", session.getUser().getUserId());

        return session;
    }
}