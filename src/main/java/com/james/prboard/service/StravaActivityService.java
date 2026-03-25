package com.james.prboard.service;

import com.james.prboard.config.StravaConfig;
import com.james.prboard.domain.Activity;
import com.james.prboard.domain.ActivityEnduranceData;
import com.james.prboard.domain.ActivityWeightliftingSession;
import com.james.prboard.domain.Discipline;
import com.james.prboard.domain.StravaSession;
import com.james.prboard.domain.User;
import com.james.prboard.mapper.StravaActivityMapper;
import com.james.prboard.model.StravaActivityResponseDto;
import com.james.prboard.repository.ActivityRepository;
import com.james.prboard.repository.DisciplineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class StravaActivityService {

    private final StravaConfig stravaConfig;
    private final RestTemplate restTemplate;
    private final ActivityRepository activityRepository;
    private final DisciplineRepository disciplineRepository;
    private final StravaActivityMapper stravaActivityMapper;
    private final StravaTokenRefreshService stravaTokenRefreshService;
    private final ActivityDeduplicationService activityDeduplicationService;
    private final PersonalRecordService personalRecordService;

    @Transactional
    public void syncActivities(UUID userId, User user) {
        StravaSession session = stravaTokenRefreshService.getValidSession(userId);
        log.info("Syncing activities from Strava for user {}", userId);

        List<StravaActivityResponseDto> activities = fetchActivitiesFromStrava(session.getAccessToken());
        log.info("Fetched {} activities from Strava", activities.size());

        List<Activity> saved = new java.util.ArrayList<>();
        for (StravaActivityResponseDto stravaActivity : activities) {
            saveIfNotExists(stravaActivity, user).ifPresent(saved::add);
        }

        log.info("Sync complete for user {} — {} new activities saved", userId, saved.size());
        activityDeduplicationService.deduplicateNewActivities(saved);
        personalRecordService.updatePrsForNewActivities(saved, userId);
    }

    private Optional<Activity> saveIfNotExists(StravaActivityResponseDto stravaActivity, User user) {
        String externalId = String.valueOf(stravaActivity.getId());

        Optional<Activity> existing = activityRepository.findByExternalIdAndSource(externalId, "STRAVA");
        if (existing.isPresent()) {
            log.debug("Activity {} already exists, skipping", externalId);
            return Optional.empty();
        }

        Discipline discipline = resolveDiscipline(stravaActivity.getType());
        if (discipline == null) {
            log.info("Skipping activity {} - unrecognised type: {}", externalId, stravaActivity.getType());
            return Optional.empty();
        }

        Activity activity = stravaActivityMapper.toActivity(stravaActivity);
        activity.setDiscipline(discipline);
        activity.setUser(user);

        if (discipline.isDistanceBased()) {
            ActivityEnduranceData enduranceData = stravaActivityMapper.toEnduranceData(stravaActivity);
            enduranceData.setActivity(activity);
            activity.setEnduranceData(enduranceData);
        } else {
            ActivityWeightliftingSession session = stravaActivityMapper.toWeightliftingSession(stravaActivity);
            session.setActivity(activity);
            activity.setWeightliftingSession(session);
        }

        Activity saved = activityRepository.save(activity);
        log.info("Saved activity {} - {}", externalId, discipline.getCode());
        return Optional.of(saved);
    }

    private Discipline resolveDiscipline(String stravaType) {
        String code = switch (stravaType) {
            case "Swim"                             -> "SWIM";
            case "Ride", "VirtualRide", "EBikeRide" -> "BIKE";
            case "Run", "VirtualRun"                -> "RUN";
            case "TrailRun"                         -> "TRAIL_RUN";
            case "Rowing", "VirtualRow"             -> "ROW";
            case "Hike"                             -> "HIKE";
            case "WeightTraining", "Crossfit"       -> "WEIGHTLIFT";
            default                                 -> null;
        };

        if (code == null) return null;

        return disciplineRepository.findByCode(code).orElseGet(() -> {
            log.warn("Discipline code {} not found in DB — skipping activity", code);
            return null;
        });
    }

    private List<StravaActivityResponseDto> fetchActivitiesFromStrava(String accessToken) {
        List<StravaActivityResponseDto> allActivities = new java.util.ArrayList<>();
        int page = 1;
        int perPage = 200;
        boolean hasMore = true;

        while (hasMore) {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<StravaActivityResponseDto[]> response = restTemplate.exchange(
                    stravaConfig.getBaseUrl() + "/api/v3/athlete/activities?per_page=" + perPage + "&page=" + page,
                    HttpMethod.GET,
                    request,
                    StravaActivityResponseDto[].class
            );

            StravaActivityResponseDto[] body = response.getBody();

            if (body == null || body.length == 0) {
                hasMore = false;
            } else {
                allActivities.addAll(Arrays.asList(body));
                log.info("Fetched page {} - {} activities", page, body.length);
                hasMore = body.length == perPage;
                page++;
            }
        }

        log.info("Total activities fetched: {}", allActivities.size());
        return allActivities;
    }
}