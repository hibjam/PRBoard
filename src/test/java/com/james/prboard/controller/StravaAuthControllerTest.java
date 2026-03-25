package com.james.prboard.controller;

import com.james.prboard.domain.User;
import com.james.prboard.service.StravaAuthService;
import com.james.prboard.service.UserResolutionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StravaAuthController.class)
class StravaAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StravaAuthService stravaAuthService;

    @MockitoBean
    private UserResolutionService userResolutionService;

    @Test
    void initiateStravaAuth_returns200_withUrlJson() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setUserId(userId);

        String expectedUrl = "https://www.strava.com/oauth/authorize?client_id=12345&state=someOpaqueToken";

        when(userResolutionService.resolveCurrentUser()).thenReturn(user);
        when(stravaAuthService.generateAuthUrl(userId)).thenReturn(expectedUrl);

        // When / Then
        mockMvc.perform(get("/auth/strava"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value(expectedUrl));
    }

    @Test
    void initiateStravaAuth_doesNotExposeUserId_inResponse() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setUserId(userId);

        String urlWithOpaqueState = "https://www.strava.com/oauth/authorize?state=someOpaqueToken";

        when(userResolutionService.resolveCurrentUser()).thenReturn(user);
        when(stravaAuthService.generateAuthUrl(userId)).thenReturn(urlWithOpaqueState);

        // When / Then
        mockMvc.perform(get("/auth/strava"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value(urlWithOpaqueState));
    }

    @Test
    void handleStravaCallback_returns200_whenCallbackSucceeds() throws Exception {
        // Given
        String code = "valid-auth-code";
        String stateToken = "opaque-state-token";
        doNothing().when(stravaAuthService).handleCallback(code, stateToken);

        // When / Then
        mockMvc.perform(get("/callback/strava")
                        .param("code", code)
                        .param("state", stateToken))
                .andExpect(status().isOk());

        verify(stravaAuthService).handleCallback(code, stateToken);
    }

    @Test
    void handleStravaCallback_returns400_whenStateIsInvalid() throws Exception {
        // Given
        String code = "valid-auth-code";
        String invalidState = "invalid-state-token";

        doThrow(new IllegalArgumentException("Invalid OAuth state"))
                .when(stravaAuthService).handleCallback(code, invalidState);

        // When / Then
        mockMvc.perform(get("/callback/strava")
                        .param("code", code)
                        .param("state", invalidState))
                .andExpect(status().isBadRequest());
    }

    @Test
    void handleStravaCallback_returns400_whenStateIsExpired() throws Exception {
        // Given
        String code = "valid-auth-code";
        String expiredState = "expired-state-token";

        doThrow(new IllegalArgumentException("OAuth state has expired — please try connecting Strava again"))
                .when(stravaAuthService).handleCallback(code, expiredState);

        // When / Then
        mockMvc.perform(get("/callback/strava")
                        .param("code", code)
                        .param("state", expiredState))
                .andExpect(status().isBadRequest());
    }
}