package nvt.backend.dto.vehicle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityStatisticsDTO {
    private Long vehicleId;
    private String licensePlate;
    private Instant startTime;
    private Instant endTime;
    private String aggregationType;
    private long totalOnlineSeconds;
    private long totalOfflineSeconds;
    private double onlinePercentage;
    private double offlinePercentage;
    private List<AvailabilityDataPoint> dataPoints;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AvailabilityDataPoint {
        private String label;
        private Instant startTime;
        private Instant endTime;
        private long onlineSeconds;
        private long offlineSeconds;
        private double onlinePercentage;
        private double offlinePercentage;
    }
}
