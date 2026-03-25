package com.james.prboard.service;

import com.james.prboard.domain.Activity;
import com.james.prboard.domain.Discipline;
import com.james.prboard.domain.User;
import com.james.prboard.repository.ActivityRepository;
import com.james.prboard.repository.DisciplineRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReclassifyServiceTest {

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private DisciplineRepository disciplineRepository;

    @InjectMocks
    private ReclassifyService reclassifyService;

    @Test
    void reclassify_updatesDiscipline_whenValid() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = userWithId(userId);

        Discipline weightlift = Discipline.builder().id((short) 7).code("WEIGHTLIFT").distanceBased(false).active(true).build();
        Discipline powerlifting = Discipline.builder().id((short) 8).code("POWERLIFTING").distanceBased(false).active(true).build();

        Activity activity = activityOwnedBy(user, weightlift);

        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(disciplineRepository.findByCode("POWERLIFTING")).thenReturn(Optional.of(powerlifting));
        when(activityRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // When
        reclassifyService.reclassify(1L, "POWERLIFTING", user);

        // Then
        assertThat(activity.getDiscipline().getCode()).isEqualTo("POWERLIFTING");
        verify(activityRepository).save(activity);
    }

    @Test
    void reclassify_doesNotSave_whenDisciplineUnchanged() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = userWithId(userId);
        Discipline weightlift = Discipline.builder().id((short) 7).code("WEIGHTLIFT").distanceBased(false).active(true).build();
        Activity activity = activityOwnedBy(user, weightlift);

        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(disciplineRepository.findByCode("WEIGHTLIFT")).thenReturn(Optional.of(weightlift));

        // When
        reclassifyService.reclassify(1L, "WEIGHTLIFT", user);

        // Then
        verify(activityRepository, never()).save(any());
    }

    @Test
    void reclassify_throwsException_whenActivityNotFound() {
        // Given
        when(activityRepository.findById(99L)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> reclassifyService.reclassify(99L, "POWERLIFTING", new User()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Activity not found");
    }

    @Test
    void reclassify_throwsException_whenActivityBelongsToDifferentUser() {
        // Given
        User owner = userWithId(UUID.randomUUID());
        User requester = userWithId(UUID.randomUUID());

        Discipline weightlift = Discipline.builder().id((short) 7).code("WEIGHTLIFT").distanceBased(false).active(true).build();
        Activity activity = activityOwnedBy(owner, weightlift);

        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));

        // When / Then
        assertThatThrownBy(() -> reclassifyService.reclassify(1L, "POWERLIFTING", requester))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Activity not found");

        verify(activityRepository, never()).save(any());
    }

    @Test
    void reclassify_throwsException_whenCrossingEnduranceWeightliftingBoundary() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = userWithId(userId);

        Discipline run = Discipline.builder().id((short) 3).code("RUN").distanceBased(true).active(true).build();
        Discipline powerlifting = Discipline.builder().id((short) 8).code("POWERLIFTING").distanceBased(false).active(true).build();
        Activity activity = activityOwnedBy(user, run);

        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(disciplineRepository.findByCode("POWERLIFTING")).thenReturn(Optional.of(powerlifting));

        // When / Then
        assertThatThrownBy(() -> reclassifyService.reclassify(1L, "POWERLIFTING", user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot reclassify between endurance and weightlifting");

        verify(activityRepository, never()).save(any());
    }

    @Test
    void reclassify_throwsException_whenDisciplineCodeUnknown() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = userWithId(userId);
        Discipline weightlift = Discipline.builder().id((short) 7).code("WEIGHTLIFT").distanceBased(false).active(true).build();
        Activity activity = activityOwnedBy(user, weightlift);

        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(disciplineRepository.findByCode("NONSENSE")).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> reclassifyService.reclassify(1L, "NONSENSE", user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown discipline");

        verify(activityRepository, never()).save(any());
    }

    // --- helpers ---

    private User userWithId(UUID userId) {
        User user = new User();
        user.setUserId(userId);
        return user;
    }

    private Activity activityOwnedBy(User user, Discipline discipline) {
        Activity activity = new Activity();
        activity.setUser(user);
        activity.setDiscipline(discipline);
        return activity;
    }
}