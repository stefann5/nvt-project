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
public class FactoryProductionStatisticsDTO {
    private Long factoryId;
    private String factoryName;
    private Long productId;
    private String productName;
    private LocalDate startDate;
    private LocalDate endDate;
    private String aggregationType;
    private List<ProductionDataPoint> dataPoints;
    private Long totalQuantity;
    private Double averageQuantity;
    private Integer minQuantity;
    private Integer maxQuantity;
    private Integer dataPointCount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductionDataPoint {
        private LocalDate date;
        private LocalDate endDate;
        private String label;
        private Integer quantity;
    }
}
