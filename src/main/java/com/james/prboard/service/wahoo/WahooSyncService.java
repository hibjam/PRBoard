package com.james.prboard.service.wahoo;

import com.james.prboard.domain.Activity;
import com.james.prboard.domain.ActivityEnduranceData;
import com.james.prboard.domain.Discipline;
import com.james.prboard.domain.User;
import com.james.prboard.mapper.WahooActivityMapper;
import com.james.prboard.model.wahoo.WahooWorkoutResponseDto;
import com.james.prboard.model.wahoo.WahooWorkoutsPageDto;
import com.james.prboard.repository.ActivityRepository;
import com.james.prboard.repository.DisciplineRepository;
import com.james.prboard.service.ActivityDeduplicationService;
import com.james.prboard.service.PersonalRecordService;
import com.james.prboard.service.UserResolutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class WahooSyncService {

    private static final int PAGE_SIZE = 30;

    private static final Map<String, String> DISCIPLINE_MAP = Map.ofEntries(
            Map.entry("cycling",             "BIKE"),
            Map.entry("cycling_indoors",     "BIKE"),
            Map.entry("virtual_cycling",     "BIKE"),
            Map.entry("running",             "RUN"),
            Map.entry("running_indoors",     "RUN"),
            Map.entry("trail_running",       "TRAIL_RUN"),
            Map.entry("walking",             "HIKE"),
            Map.entry("hiking",              "HIKE"),
            Map.entry("rowing",              "ROW"),
            Map.entry("rowing_indoors",      "ROW"),
            Map.entry("swimming",            "SWIM"),
            Map.entry("open_water_swimming", "SWIM")
    );

    private final WahooTokenRefreshService wahooTokenRefreshService;
    private final WahooActivityMapper wahooActivityMapper;
    private final ActivityRepository activityRepository;
    private final DisciplineRepository disciplineRepository;
    private final ActivityDeduplicationService activityDeduplicationService;
    private final PersonalRecordService personalRecordService;
    private final UserResolutionService userResolutionService;
    private final RestTemplate restTemplate;

    @Transactional
    public void syncActivities(UUID userId, User user) {
        var session = wahooTokenRefreshService.getValidSession(userId);
        log.info("Syncing activities from Wahoo for user {}", userId);

        List<WahooWorkoutResponseDto> workouts = fetchAllWorkouts(session.getAccessToken());
        log.info("Fetched {} workouts from Wahoo", workouts.size());

        List<Activity> saved = new ArrayList<>();
        for (WahooWorkoutResponseDto workout : workouts) {
            saveIfNotExists(workout, user).ifPresent(saved::add);
        }

        log.info("Wahoo sync complete for user {} — {} new activities saved", userId, saved.size());
        activityDeduplicationService.deduplicateNewActivities(saved);
        personalRecordService.updatePrsForNewActivities(saved, userId);
    }

    private Optional<Activity> saveIfNotExists(WahooWorkoutResponseDto workout, User user) {
        String externalId = String.valueOf(workout.getId());

        if (activityRepository.findByExternalIdAndSource(externalId, "WAHOO").isPresent()) {
            log.debug("Wahoo workout {} already exists, skipping", externalId);
            return Optional.empty();
        }

        Discipline discipline = resolveDiscipline(workout);
        if (discipline == null) {
            log.info("Skipping Wahoo workout {} — unrecognised type: {}",
                    externalId,
                    workout.getWorkoutType() != null ? workout.getWorkoutType().getName() : "null");
            return Optional.empty();
        }

        Activity activity = wahooActivityMapper.toActivity(workout);
        activity.setDiscipline(discipline);
        activity.setUser(user);

        ActivityEnduranceData enduranceData = wahooActivityMapper.toEnduranceData(workout);
        enduranceData.setActivity(activity);
        activity.setEnduranceData(enduranceData);

        Activity saved = activityRepository.save(activity);
        log.info("Saved Wahoo activity {} — {}", externalId, discipline.getCode());
        return Optional.of(saved);
    }

    private Discipline resolveDiscipline(WahooWorkoutResponseDto workout) {
        if (workout.getWorkoutType() == null || workout.getWorkoutType().getName() == null) {
            return null;
        }

        String wahooType = workout.getWorkoutType().getName().toLowerCase();
        String code = DISCIPLINE_MAP.get(wahooType);

        if (code == null) return null;

        return disciplineRepository.findByCode(code).orElseGet(() -> {
            log.warn("Discipline code {} not found in DB — skipping Wahoo activity", code);
            return null;
        });
    }

    private List<WahooWorkoutResponseDto> fetchAllWorkouts(String accessToken) {
        List<WahooWorkoutResponseDto> all = new ArrayList<>();
        int page    = 1;
        boolean hasMore = true;

        while (hasMore) {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<WahooWorkoutsPageDto> response = restTemplate.exchange(
                    "https://api.wahooligan.com/v1/workouts?page=" + page + "&per_page=" + PAGE_SIZE,
                    HttpMethod.GET,
                    request,
                    WahooWorkoutsPageDto.class
            );

            WahooWorkoutsPageDto body = response.getBody();
            if (body == null || body.getWorkouts() == null || body.getWorkouts().isEmpty()) break;

            all.addAll(body.getWorkouts());
            log.info("Fetched Wahoo page {} — {} workouts", page, body.getWorkouts().size());

            int total = body.getMeta() != null && body.getMeta().getTotal() != null
                    ? body.getMeta().getTotal() : 0;
            hasMore = (page * PAGE_SIZE) < total;
            page++;
        }

        log.info("Total Wahoo workouts fetched: {}", all.size());
        return all;
    }
}