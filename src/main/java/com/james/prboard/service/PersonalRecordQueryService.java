package com.james.prboard.service;

import com.james.prboard.domain.Discipline;
import com.james.prboard.domain.PersonalRecord;
import com.james.prboard.domain.UserPrTarget;

import com.james.prboard.domain.constant.PrType;
import com.james.prboard.model.PersonalRecordDto;
import com.james.prboard.model.PrTargetRequestDto;
import com.james.prboard.repository.DisciplineRepository;
import com.james.prboard.repository.PersonalRecordRepository;
import com.james.prboard.repository.UserPrTargetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PersonalRecordQueryService {

    private final PersonalRecordRepository personalRecordRepository;
    private final UserPrTargetRepository prTargetRepository;
    private final DisciplineRepository disciplineRepository;

    @Transactional(readOnly = true)
    public List<PersonalRecordDto> getPrsForUser(UUID userId) {
        return personalRecordRepository.findAllWithDetailsByUserId(userId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void addCustomTarget(UUID userId, PrTargetRequestDto request) {
        Discipline discipline = disciplineRepository.findByCode(request.getDisciplineCode())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown discipline: " + request.getDisciplineCode()));

        boolean exists = prTargetRepository
                .findByUserIdAndDisciplineIdAndDistanceMetres(
                        userId, discipline.getId(), request.getDistanceMetres())
                .isPresent();

        if (exists) {
            throw new IllegalArgumentException("A target for that distance already exists.");
        }

        UserPrTarget target = new UserPrTarget();
        target.setUserId(userId);
        target.setDiscipline(discipline);
        target.setDistanceMetres(request.getDistanceMetres());
        target.setLabel(request.getLabel() != null ? request.getLabel()
                : formatDistanceLabel(request.getDistanceMetres()));
        target.setPreset(false);
        prTargetRepository.save(target);
    }

    @Transactional
    public void deleteTarget(UUID userId, Integer targetId) {
        UserPrTarget target = prTargetRepository.findById(targetId)
                .orElseThrow(() -> new IllegalArgumentException("Target not found: " + targetId));

        if (!target.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Target not found: " + targetId);
        }

        prTargetRepository.delete(target);
    }

    private PersonalRecordDto toDto(PersonalRecord pr) {
        PersonalRecordDto dto = new PersonalRecordDto();
        dto.setDiscipline(com.james.prboard.domain.constant.Discipline.fromCode(pr.getDiscipline().getCode()));
        dto.setPrType(pr.getPrType());
        dto.setValue(pr.getValue());
        dto.setActivityName(pr.getActivity().getName());
        dto.setAchievedAt(pr.getAchievedAt());
        dto.setSource(pr.getActivity().getSource());

        if (pr.getTarget() != null) {
            dto.setTargetId(pr.getTarget().getId());
            dto.setTargetLabel(pr.getTarget().getLabel());
            dto.setTargetDistanceMetres(pr.getTarget().getDistanceMetres());
            dto.setPreset(pr.getTarget().isPreset());
        }

        dto.setFormattedValue(formatValue(pr));
        return dto;
    }

    private String formatValue(PersonalRecord pr) {
        return switch (pr.getPrType()) {
            case PrType.FASTEST_DISTANCE -> formatDuration(pr.getValue().intValue());
            case PrType.LONGEST_ACTIVITY -> formatDistance(pr.getValue());
            case PrType.BEST_POWER       -> pr.getValue().intValue() + "W";
            case PrType.HEAVIEST_SESSION -> formatVolume(pr.getValue()) + " kg";
            default                      -> pr.getValue().toPlainString();
        };
    }

    private String formatDuration(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%d:%02d", minutes, seconds);
    }

    private String formatDistance(BigDecimal metres) {
        BigDecimal km = metres.divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP);
        return km.toPlainString() + " km";
    }

    private String formatVolume(BigDecimal kg) {
        return String.format("%,.0f", kg.doubleValue());
    }

    private String formatDistanceLabel(BigDecimal metres) {
        if (metres.compareTo(BigDecimal.valueOf(1000)) < 0) {
            return metres.intValue() + "m";
        }
        BigDecimal km = metres.divide(BigDecimal.valueOf(1000), 1, RoundingMode.HALF_UP);
        return km.stripTrailingZeros().toPlainString() + "K";
    }
}