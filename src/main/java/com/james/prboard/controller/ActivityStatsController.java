package com.james.prboard.controller;

import com.james.prboard.domain.User;
import com.james.prboard.domain.constant.StatsPeriod;
import com.james.prboard.model.ActivityStatsDto;
import com.james.prboard.model.RecentActivityDto;
import com.james.prboard.model.TrendPointDto;
import com.james.prboard.service.ActivityStatsService;
import com.james.prboard.service.UserResolutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/stats")
@Slf4j
@RequiredArgsConstructor
public class ActivityStatsController {

    private final ActivityStatsService activityStatsService;
    private final UserResolutionService userResolutionService;

    @GetMapping
    public List<ActivityStatsDto> getStats(
            @RequestParam(defaultValue = "MONTH") StatsPeriod period) {
        User user = userResolutionService.resolveCurrentUser();
        return activityStatsService.getStatsPerDiscipline(user.getUserId(), period);
    }

    @GetMapping("/recent")
    public List<RecentActivityDto> getRecentActivities(
            @RequestParam(defaultValue = "10") int limit) {
        User user = userResolutionService.resolveCurrentUser();
        return activityStatsService.getRecentActivities(user.getUserId(), limit);
    }

    @GetMapping("/trends")
    public List<TrendPointDto> getTrends(
            @RequestParam(defaultValue = "monthly") String groupBy,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        User user = userResolutionService.resolveCurrentUser();
        return activityStatsService.getTrends(user.getUserId(), groupBy, from, to);
    }
}