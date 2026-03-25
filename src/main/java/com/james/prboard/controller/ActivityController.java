package com.james.prboard.controller;

import com.james.prboard.domain.User;
import com.james.prboard.model.ActivityDto;
import com.james.prboard.model.ReclassifyRequestDto;
import com.james.prboard.service.ActivityService;
import com.james.prboard.service.ReclassifyService;
import com.james.prboard.service.StravaActivityService;
import com.james.prboard.service.UserResolutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/activities")
@Slf4j
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;
    private final StravaActivityService stravaActivityService;
    private final UserResolutionService userResolutionService;
    private final ReclassifyService reclassifyService;

    @GetMapping
    public List<ActivityDto> getActivities() {
        User user = userResolutionService.resolveCurrentUser();
        return activityService.getActivitiesForUser(user.getUserId());
    }

    @PostMapping("/sync/strava")
    public void syncStravaActivities() {
        User user = userResolutionService.resolveCurrentUser();
        stravaActivityService.syncActivities(user.getUserId(), user);
    }

    @PatchMapping("/{id}/discipline")
    public ResponseEntity<Void> reclassify(
            @PathVariable Long id,
            @RequestBody ReclassifyRequestDto request) {
        User user = userResolutionService.resolveCurrentUser();
        reclassifyService.reclassify(id, request.getDisciplineCode(), user);
        return ResponseEntity.noContent().build();
    }
}