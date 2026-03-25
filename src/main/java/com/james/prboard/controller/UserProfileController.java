package com.james.prboard.controller;

import com.james.prboard.domain.User;
import com.james.prboard.model.UpdateProfileRequestDto;
import com.james.prboard.model.UserProfileDto;
import com.james.prboard.service.UserProfileService;
import com.james.prboard.service.UserResolutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users/profile")
@Slf4j
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final UserResolutionService userResolutionService;

    @GetMapping
    public UserProfileDto getProfile() {
        User user = userResolutionService.resolveCurrentUser();
        return userProfileService.getProfile(user);
    }

    @PatchMapping
    public UserProfileDto updateProfile(@RequestBody UpdateProfileRequestDto request) {
        User user = userResolutionService.resolveCurrentUser();
        return userProfileService.updateProfile(user, request);
    }
}