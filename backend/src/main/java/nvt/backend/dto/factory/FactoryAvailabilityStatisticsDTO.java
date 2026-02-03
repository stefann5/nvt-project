package nvt.backend.dto.factory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FactoryAvailabilityStatisticsDTO {
    private Long factoryId;
    private String factoryName;
    private LocalDate startDate;
    private LocalDate endDate;
    private String aggregationType;
    private List<AvailabilityDataPoint> dataPoints;
    private Double averageUptimePercentage;
    private Integer dataPointCount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AvailabilityDataPoint {
        private LocalDate date;
        private LocalDate endDate;
        private String label;
        private Double uptimePercentage;
    }
}
