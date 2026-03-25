package com.james.prboard.service;

import com.james.prboard.config.StravaConfig;
import com.james.prboard.domain.StravaOAuthState;
import com.james.prboard.domain.StravaSession;
import com.james.prboard.domain.User;
import com.james.prboard.model.StravaTokenResponseDto;
import com.james.prboard.repository.StravaOAuthStateRepository;
import com.james.prboard.repository.StravaSessionRepository;
import com.james.prboard.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StravaAuthServiceTest {

    @Mock private StravaConfig stravaConfig;
    @Mock private StravaOAuthStateRepository stravaOAuthStateRepository;
    @Mock private StravaSessionRepository stravaSessionRepository;
    @Mock private UserRepository userRepository;
    @Mock private RestTemplate restTemplate;
    @Mock private SecureRandom secureRandom;

    @InjectMocks
    private StravaAuthService stravaAuthService;

    @Test
    void generateAuthUrl_savesStateAndReturnsCorrectUrl() {
        // Given
        UUID userId = UUID.randomUUID();
        when(stravaConfig.getBaseUrl()).thenReturn("https://www.strava.com");
        when(stravaConfig.getClientId()).thenReturn("12345");
        when(stravaConfig.getRedirectUri()).thenReturn("http://localhost:8080/callback/strava");

        // When
        String url = stravaAuthService.generateAuthUrl(userId);

        // Then
        assertThat(url).startsWith("https://www.strava.com/oauth/authorize");
        assertThat(url).contains("client_id=12345");
        assertThat(url).contains("redirect_uri=http://localhost:8080/callback/strava");
        assertThat(url).contains("response_type=code");
        assertThat(url).contains("scope=activity:read_all");
        assertThat(url).contains("state=");

        ArgumentCaptor<StravaOAuthState> stateCaptor = ArgumentCaptor.forClass(StravaOAuthState.class);
        verify(stravaOAuthStateRepository).save(stateCaptor.capture());
        assertThat(stateCaptor.getValue().getUserId()).isEqualTo(userId);
        assertThat(stateCaptor.getValue().getExpiresAt()).isAfter(OffsetDateTime.now());
    }

    @Test
    void generateAuthUrl_stateTokenDoesNotContainUserId() {
        // Given
        UUID userId = UUID.randomUUID();
        when(stravaConfig.getBaseUrl()).thenReturn("https://www.strava.com");
        when(stravaConfig.getClientId()).thenReturn("12345");
        when(stravaConfig.getRedirectUri()).thenReturn("http://localhost:8080/callback/strava");

        // When
        String url = stravaAuthService.generateAuthUrl(userId);

        // Then — the raw userId must never appear in the URL
        assertThat(url).doesNotContain(userId.toString());
    }

    @Test
    void handleCallback_persistsSession_whenStateIsValid() {
        // Given
        String stateToken = "valid-state-token";
        String code = "valid-auth-code";
        UUID userId = UUID.randomUUID();

        User user = new User();
        user.setUserId(userId);

        StravaOAuthState state = new StravaOAuthState();
        state.setStateToken(stateToken);
        state.setUserId(userId);
        state.setExpiresAt(OffsetDateTime.now().plusMinutes(5));

        StravaTokenResponseDto tokenResponse = new StravaTokenResponseDto();
        tokenResponse.setAccessToken("access-token");
        tokenResponse.setRefreshToken("refresh-token");
        tokenResponse.setExpiresAt(9999999999L);

        when(stravaOAuthStateRepository.findByStateToken(stateToken)).thenReturn(Optional.of(state));
        when(userRepository.findByUserId(userId)).thenReturn(Optional.of(user));
        when(stravaSessionRepository.findByUser(user)).thenReturn(Optional.empty());
        when(restTemplate.postForObject(
                contains("/oauth/token"),
                any(HttpEntity.class),
                eq(StravaTokenResponseDto.class)
        )).thenReturn(tokenResponse);

        // When
        stravaAuthService.handleCallback(code, stateToken);

        // Then
        verify(stravaOAuthStateRepository).delete(state);
        verify(stravaSessionRepository).save(any(StravaSession.class));
    }

    @Test
    void handleCallback_throwsException_whenStateTokenNotFound() {
        // Given
        when(stravaOAuthStateRepository.findByStateToken("unknown")).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> stravaAuthService.handleCallback("code", "unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid OAuth state");

        verify(stravaSessionRepository, never()).save(any());
    }

    @Test
    void handleCallback_throwsException_andDeletesState_whenStateIsExpired() {
        // Given
        String stateToken = "expired-state-token";

        StravaOAuthState expiredState = new StravaOAuthState();
        expiredState.setStateToken(stateToken);
        expiredState.setUserId(UUID.randomUUID());
        expiredState.setExpiresAt(OffsetDateTime.now().minusMinutes(1));

        when(stravaOAuthStateRepository.findByStateToken(stateToken)).thenReturn(Optional.of(expiredState));

        // When / Then
        assertThatThrownBy(() -> stravaAuthService.handleCallback("code", stateToken))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expired");

        verify(stravaOAuthStateRepository).delete(expiredState);
        verify(stravaSessionRepository, never()).save(any());
    }

    @Test
    void handleCallback_updatesExistingSession_whenSessionAlreadyExists() {
        // Given
        String stateToken = "valid-state-token";
        String code = "valid-auth-code";
        UUID userId = UUID.randomUUID();

        User user = new User();
        user.setUserId(userId);

        StravaOAuthState state = new StravaOAuthState();
        state.setStateToken(stateToken);
        state.setUserId(userId);
        state.setExpiresAt(OffsetDateTime.now().plusMinutes(5));

        StravaSession existingSession = new StravaSession();
        existingSession.setUser(user);
        existingSession.setAccessToken("old-access-token");

        StravaTokenResponseDto tokenResponse = new StravaTokenResponseDto();
        tokenResponse.setAccessToken("new-access-token");
        tokenResponse.setRefreshToken("new-refresh-token");
        tokenResponse.setExpiresAt(9999999999L);

        when(stravaOAuthStateRepository.findByStateToken(stateToken)).thenReturn(Optional.of(state));
        when(userRepository.findByUserId(userId)).thenReturn(Optional.of(user));
        when(stravaSessionRepository.findByUser(user)).thenReturn(Optional.of(existingSession));
        when(restTemplate.postForObject(
                contains("/oauth/token"),
                any(HttpEntity.class),
                eq(StravaTokenResponseDto.class)
        )).thenReturn(tokenResponse);

        // When
        stravaAuthService.handleCallback(code, stateToken);

        // Then
        ArgumentCaptor<StravaSession> sessionCaptor = ArgumentCaptor.forClass(StravaSession.class);
        verify(stravaSessionRepository).save(sessionCaptor.capture());
        assertThat(sessionCaptor.getValue().getAccessToken()).isEqualTo("new-access-token");
    }

    @Test
    void cleanUpExpiredStates_deletesExpiredRecords() {
        // When
        stravaAuthService.cleanUpExpiredStates();

        // Then
        verify(stravaOAuthStateRepository).deleteExpired(any(OffsetDateTime.class));
    }
}