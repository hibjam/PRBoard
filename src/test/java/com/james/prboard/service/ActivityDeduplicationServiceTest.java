package com.james.prboard.service;

import com.james.prboard.domain.Activity;
import com.james.prboard.domain.ActivityEnduranceData;
import com.james.prboard.domain.Discipline;
import com.james.prboard.domain.User;
import com.james.prboard.repository.ActivityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityDeduplicationServiceTest {

    @Mock
    private ActivityRepository activityRepository;

    @InjectMocks
    private ActivityDeduplicationService activityDeduplicationService;

    private static final Discipline RUN = Discipline.builder()
            .id((short) 3)
            .code("RUN")
            .distanceBased(true)
            .active(true)
            .build();

    @Test
    void deduplicateNewActivities_marksNewerAsDuplicate_whenStartTimeAndDurationMatch() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = userWithId(userId);
        OffsetDateTime startTime = OffsetDateTime.now();

        Activity existing = activity(1L, user, startTime, 3600);
        Activity incoming = activity(2L, user, startTime.plusMinutes(2), 3600);

        when(activityRepository.findDuplicateCandidates(eq(userId), eq((short) 3), any(), any()))
                .thenReturn(List.of(existing));

        // When
        activityDeduplicationService.deduplicateNewActivities(List.of(incoming));

        // Then — incoming (higher id) is the duplicate
        ArgumentCaptor<Activity> captor = ArgumentCaptor.forClass(Activity.class);
        verify(activityRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(2L);
        assertThat(captor.getValue().getCanonicalActivity().getId()).isEqualTo(1L);
    }

    @Test
    void deduplicateNewActivities_doesNotMark_whenDurationsAreTooFarApart() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = userWithId(userId);
        OffsetDateTime startTime = OffsetDateTime.now();

        Activity existing = activity(1L, user, startTime, 3600);
        Activity incoming = activity(2L, user, startTime.plusMinutes(2), 2000); // >20% diff

        when(activityRepository.findDuplicateCandidates(eq(userId), eq((short) 3), any(), any()))
                .thenReturn(List.of(existing));

        // When
        activityDeduplicationService.deduplicateNewActivities(List.of(incoming));

        // Then
        verify(activityRepository, never()).save(any());
    }

    @Test
    void deduplicateNewActivities_doesNotMark_whenCandidateIsSameActivity() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = userWithId(userId);
        Activity activity = activity(1L, user, OffsetDateTime.now(), 3600);

        when(activityRepository.findDuplicateCandidates(eq(userId), eq((short) 3), any(), any()))
                .thenReturn(List.of(activity)); // returns itself

        // When
        activityDeduplicationService.deduplicateNewActivities(List.of(activity));

        // Then
        verify(activityRepository, never()).save(any());
    }

    @Test
    void deduplicateNewActivities_skipsAlreadyMarkedActivities() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = userWithId(userId);
        Activity canonical = activity(1L, user, OffsetDateTime.now(), 3600);
        Activity alreadyDuplicate = activity(2L, user, OffsetDateTime.now(), 3600);
        alreadyDuplicate.setCanonicalActivity(canonical);

        // When
        activityDeduplicationService.deduplicateNewActivities(List.of(alreadyDuplicate));

        // Then
        verify(activityRepository, never()).findDuplicateCandidates(any(), any(), any(), any());
        verify(activityRepository, never()).save(any());
    }

    @Test
    void deduplicateNewActivities_doesNotMark_whenNoCandidatesFound() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = userWithId(userId);
        Activity incoming = activity(1L, user, OffsetDateTime.now(), 3600);

        when(activityRepository.findDuplicateCandidates(eq(userId), eq((short) 3), any(), any()))
                .thenReturn(List.of());

        // When
        activityDeduplicationService.deduplicateNewActivities(List.of(incoming));

        // Then
        verify(activityRepository, never()).save(any());
    }

    @Test
    void deduplicateNewActivities_toleratesExactly20PercentDurationDifference() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = userWithId(userId);
        OffsetDateTime startTime = OffsetDateTime.now();

        // 3600 vs 2880 = exactly 20% difference
        Activity existing = activity(1L, user, startTime, 3600);
        Activity incoming = activity(2L, user, startTime.plusMinutes(1), 2880);

        when(activityRepository.findDuplicateCandidates(eq(userId), eq((short) 3), any(), any()))
                .thenReturn(List.of(existing));

        // When
        activityDeduplicationService.deduplicateNewActivities(List.of(incoming));

        // Then — exactly 20% is within tolerance
        verify(activityRepository).save(any());
    }

    @Test
    void deduplicateAll_processesAllCanonicalActivitiesForUser() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = userWithId(userId);
        OffsetDateTime startTime = OffsetDateTime.now();

        Activity a1 = activity(1L, user, startTime, 3600);
        Activity a2 = activity(2L, user, startTime.plusMinutes(3), 3600);

        when(activityRepository.findByUserUserIdAndCanonicalActivityIsNull(userId))
                .thenReturn(List.of(a1, a2));
        when(activityRepository.findDuplicateCandidates(eq(userId), eq((short) 3), any(), any()))
                .thenReturn(List.of(a1, a2));

        // When
        activityDeduplicationService.deduplicateAll(userId);

        // Then — a2 marked as duplicate of a1
        ArgumentCaptor<Activity> captor = ArgumentCaptor.forClass(Activity.class);
        verify(activityRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(2L);
        assertThat(captor.getValue().getCanonicalActivity().getId()).isEqualTo(1L);
    }

    // --- helpers ---

    private User userWithId(UUID userId) {
        User user = new User();
        user.setUserId(userId);
        return user;
    }

    private Activity activity(Long id, User user, OffsetDateTime startTime, int durationSeconds) {
        Activity activity = new Activity();
        activity.setId(id);
        activity.setUser(user);
        activity.setDiscipline(RUN);
        activity.setStartTime(startTime);
        activity.setSource("STRAVA");

        ActivityEnduranceData endurance = new ActivityEnduranceData();
        endurance.setDurationSeconds(durationSeconds);
        endurance.setActivity(activity);
        activity.setEnduranceData(endurance);

        return activity;
    }
}