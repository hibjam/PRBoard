package com.james.prboard.controller.strava;

import com.james.prboard.service.strava.StravaAuthService;
import com.james.prboard.service.UserResolutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;

@RestController
@Slf4j
@RequiredArgsConstructor
public class StravaAuthController {

    @Value("${app.frontend-url}")
    private String frontendUrl;

    private final StravaAuthService stravaAuthService;
    private final UserResolutionService userResolutionService;

    @GetMapping("/auth/strava")
    public ResponseEntity<Map<String, String>> initiateStravaAuth() {
        var user = userResolutionService.resolveCurrentUser();
        String authUrl = stravaAuthService.generateAuthUrl(user.getUserId());
        return ResponseEntity.ok(Map.of("url", authUrl));
    }

    @GetMapping("/callback/strava")
    public ResponseEntity<Void> handleStravaCallback(
            @RequestParam String code,
            @RequestParam String state) {

        stravaAuthService.handleCallback(code, state);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(frontendUrl + "?stravaConnected=true"))
                .build();
    }
}