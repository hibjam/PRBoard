package com.james.prboard.service.wahoo;

import com.james.prboard.config.WahooConfig;
import com.james.prboard.domain.User;
import com.james.prboard.domain.wahoo.WahooOAuthState;
import com.james.prboard.domain.wahoo.WahooSession;
import com.james.prboard.model.wahoo.WahooTokenResponseDto;
import com.james.prboard.repository.UserRepository;
import com.james.prboard.repository.wahoo.WahooOAuthStateRepository;
import com.james.prboard.repository.wahoo.WahooSessionRepository;
import com.james.prboard.service.PersonalRecordService;
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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class WahooAuthService {

    private static final int STATE_TOKEN_BYTES   = 32;
    private static final int STATE_EXPIRY_MINUTES = 10;
    private static final String SCOPES           = "user_read power_zones_read workouts_read";

    private final WahooConfig wahooConfig;
    private final WahooOAuthStateRepository wahooOAuthStateRepository;
    private final WahooSessionRepository wahooSessionRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    private final SecureRandom secureRandom;
    private final PersonalRecordService personalRecordService;

    @Transactional
    public String generateAuthUrl(UUID userId) {
        String codeVerifier  = generateCodeVerifier();
        String codeChallenge = generateCodeChallenge(codeVerifier);
        String stateToken    = generateStateToken();

        WahooOAuthState state = new WahooOAuthState();
        state.setStateToken(stateToken);
        state.setCodeVerifier(codeVerifier);
        state.setUserId(userId);
        state.setExpiresAt(OffsetDateTime.now().plusMinutes(STATE_EXPIRY_MINUTES));
        wahooOAuthStateRepository.save(state);

        return UriComponentsBuilder
                .fromUriString(wahooConfig.getBaseUrl() + "/oauth/authorize")
                .queryParam("client_id", wahooConfig.getClientId())
                .queryParam("redirect_uri", wahooConfig.getRedirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", SCOPES)
                .queryParam("state", stateToken)
                .queryParam("code_challenge", codeChallenge)
                .queryParam("code_challenge_method", "S256")
                .build()
                .toUriString();
    }

    @Transactional
    public void handleCallback(String code, String stateToken) {
        WahooOAuthState state = wahooOAuthStateRepository
                .findByStateToken(stateToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid OAuth state"));

        if (state.isExpired()) {
            wahooOAuthStateRepository.delete(state);
            throw new IllegalArgumentException("OAuth state has expired — please try connecting Wahoo again");
        }

        wahooOAuthStateRepository.delete(state);

        User user = userRepository.findByUserId(state.getUserId())
                .orElseThrow(() -> new IllegalStateException("User not found for OAuth state"));

        WahooTokenResponseDto tokenResponse = exchangeCode(code, state.getCodeVerifier());

        log.info("Exchanging auth code for Wahoo token for user {}", user.getUserId());

        long expiresAt = (System.currentTimeMillis() / 1000) + tokenResponse.getExpiresIn();

        WahooSession session = wahooSessionRepository
                .findByUser(user)
                .orElseGet(() -> {
                    WahooSession s = new WahooSession();
                    s.setUser(user);
                    return s;
                });

        boolean isFirstConnect = session.getId() == null;

        session.setAccessToken(tokenResponse.getAccessToken());
        session.setRefreshToken(tokenResponse.getRefreshToken());
        session.setExpiresAt(expiresAt);
        wahooSessionRepository.save(session);

        log.info("Wahoo session persisted for user {}", user.getUserId());

        if (!isFirstConnect) {
            personalRecordService.recalculateAllPrs(user.getUserId());
        }
    }

    public boolean isConnected(UUID userId) {
        return wahooSessionRepository.findByUserUserId(userId).isPresent();
    }

    private WahooTokenResponseDto exchangeCode(String code, String codeVerifier) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id",     wahooConfig.getClientId());
        body.add("code",          code);
        body.add("code_verifier", codeVerifier);
        body.add("grant_type",    "authorization_code");
        body.add("redirect_uri",  wahooConfig.getRedirectUri());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        return restTemplate.postForObject(
                wahooConfig.getBaseUrl() + "/oauth/token",
                request,
                WahooTokenResponseDto.class
        );
    }

    @Scheduled(fixedRateString = "PT1H")
    @Transactional
    public void cleanUpExpiredStates() {
        log.debug("Cleaning up expired Wahoo OAuth states");
        wahooOAuthStateRepository.deleteByExpiresAtBefore(OffsetDateTime.now());
    }

    private String generateStateToken() {
        byte[] bytes = new byte[STATE_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String generateCodeVerifier() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String generateCodeChallenge(String verifier) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}