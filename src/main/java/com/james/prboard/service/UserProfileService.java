package com.james.prboard.service;

import com.james.prboard.domain.Discipline;
import com.james.prboard.domain.User;
import com.james.prboard.model.UpdateProfileRequestDto;
import com.james.prboard.model.UserProfileDto;
import com.james.prboard.repository.DisciplineRepository;
import com.james.prboard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final DisciplineRepository disciplineRepository;
    private final PersonalRecordService personalRecordService;

    @Transactional(readOnly = true)
    public UserProfileDto getProfile(User user) {
        User managed = userRepository.findByUserId(user.getUserId())
                .orElseThrow(() -> new IllegalStateException("User not found: " + user.getUserId()));
        return toDto(managed);
    }

    @Transactional
    public UserProfileDto updateProfile(User user, UpdateProfileRequestDto request) {
        User managed = userRepository.findByUserId(user.getUserId())
                .orElseThrow(() -> new IllegalStateException("User not found: " + user.getUserId()));

        if (request.getHeightCm() != null) {
            managed.setHeightCm(request.getHeightCm());
        }
        if (request.getWeightKg() != null) {
            managed.setWeightKg(request.getWeightKg());
        }
        if (request.getPreferredUnits() != null) {
            String units = request.getPreferredUnits().toLowerCase();
            if (!units.equals("km") && !units.equals("mi")) {
                throw new IllegalArgumentException("preferredUnits must be 'km' or 'mi'");
            }
            managed.setPreferredUnits(units);
        }
        if (request.getDisciplineCodes() != null) {
            if (request.getDisciplineCodes().isEmpty()) {
                throw new IllegalArgumentException("At least one sport must be selected");
            }

            Set<Discipline> selected = request.getDisciplineCodes().stream()
                    .map(code -> disciplineRepository.findByCode(code)
                            .orElseThrow(() -> new IllegalArgumentException("Unknown discipline: " + code)))
                    .collect(Collectors.toSet());

            // Determine which disciplines are newly added so we only seed
            // PR targets for them, not for disciplines already set up
            Set<String> existingCodes = managed.getDisciplines().stream()
                    .map(Discipline::getCode)
                    .collect(Collectors.toSet());

            Set<Discipline> newlyAdded = selected.stream()
                    .filter(d -> !existingCodes.contains(d.getCode()))
                    .collect(Collectors.toSet());

            managed.getDisciplines().clear();
            managed.getDisciplines().addAll(selected);
            userRepository.save(managed);

            // Seed default PR targets for each newly selected discipline
            for (Discipline discipline : newlyAdded) {
                personalRecordService.seedDefaultTargets(
                        managed.getUserId(),
                        discipline.getCode(),
                        discipline.getId()
                );
            }
        } else {
            userRepository.save(managed);
        }

        log.info("Updated profile for user {}", managed.getUserId());
        return toDto(managed);
    }

    private UserProfileDto toDto(User managed) {
        UserProfileDto dto = new UserProfileDto();
        dto.setEmail(managed.getEmail());
        dto.setHeightCm(managed.getHeightCm());
        dto.setWeightKg(managed.getWeightKg());
        dto.setPreferredUnits(managed.getPreferredUnits() != null ? managed.getPreferredUnits() : "km");
        dto.setProfileComplete(!managed.getDisciplines().isEmpty());
        dto.setDisciplines(
                managed.getDisciplines().stream()
                        .map(d -> com.james.prboard.domain.constant.Discipline.fromCode(d.getCode()))
                        .collect(Collectors.toSet())
        );
        return dto;
    }
}