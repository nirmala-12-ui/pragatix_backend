package jjcet.PragatiX.modules.analytics.api.dto.response.risk;

import java.util.List;

public record InterventionStatusDto(
        Long total,
        Long high,
        Long medium,
        Long low,
        List<RiskSignalDto> risks,
        List<RiskProfileDto> students
) {
    public InterventionStatusDto(Long total, Long high, Long medium, Long low, List<RiskSignalDto> risks) {
        this(total, high, medium, low, risks, List.of());
    }

    public static InterventionStatusDto empty() {
        return new InterventionStatusDto(0L, 0L, 0L, 0L, List.of(), List.of());
    }
}