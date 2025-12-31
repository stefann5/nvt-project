package nvt.backend.dto.vehicle;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class DistanceStatisticsDTO {
    private Long vehicleId;
    private String licensePlate;
    private LocalDate startDate;
    private LocalDate endDate;
    private String aggregationType;
    private Double totalDistance;
    private List<DistanceDataPoint> dataPoints;

    @Data
    @Builder
    public static class DistanceDataPoint {
        private String label;
        private LocalDate startDate;
        private LocalDate endDate;
        private Double distance;
    }
}
