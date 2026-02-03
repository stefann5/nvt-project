package nvt.backend.dto.factory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FactoryProductionDTO {
    private Long factoryId;
    private String factoryName;
    private String timestamp;
    private String reportType; // "MORNING" or "EVENING"
    private List<ProductionItem> products;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductionItem {
        private Long productId;
        private String productName;
        private Integer quantity;
    }
}
