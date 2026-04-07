package com.james.prboard.controller.wahoo;

import com.james.prboard.service.UserResolutionService;
import com.james.prboard.service.wahoo.WahooAuthService;
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
public class WahooAuthController {

    @Value("${app.frontend-url}")
    private String frontendUrl;

    private final WahooAuthService wahooAuthService;
    private final UserResolutionService userResolutionService;

    @GetMapping("/auth/wahoo")
    public ResponseEntity<Map<String, String>> initiateWahooAuth() {
        var user = userResolutionService.resolveCurrentUser();
        String authUrl = wahooAuthService.generateAuthUrl(user.getUserId());
        return ResponseEntity.ok(Map.of("url", authUrl));
    }

    @GetMapping("/callback/wahoo")
    public ResponseEntity<Void> handleWahooCallback(
            @RequestParam String code,
            @RequestParam String state) {

        wahooAuthService.handleCallback(code, state);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(frontendUrl + "?wahooConnected=true"))
                .build();
    }
}