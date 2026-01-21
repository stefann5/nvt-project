package nvt.backend.dto.warehouse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemperatureStatisticsDTO implements Serializable {
    private Long warehouseId;
    private String warehouseName;
    private Long sectorId;
    private String sectorName;
    private LocalDate startDate;
    private LocalDate endDate;
    private String aggregationType;
    private Double averageTemperature;
    private Double minTemperature;
    private Double maxTemperature;
    private List<TemperatureDataPoint> dataPoints;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemperatureDataPoint implements Serializable {
        private String label;
        private LocalDate startDate;
        private LocalDate endDate;
        private Double avgTemperature;
        private Double minTemperature;
        private Double maxTemperature;
    }
}
