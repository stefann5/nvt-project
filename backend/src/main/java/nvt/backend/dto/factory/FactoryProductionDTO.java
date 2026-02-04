package nvt.backend.dto.factory;

import com.fasterxml.jackson.annotation.JsonSetter;
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

    @JsonSetter("factoryId")
    public void setFactoryId(Object factoryId) {
        if (factoryId instanceof Number) {
            this.factoryId = ((Number) factoryId).longValue();
        } else if (factoryId instanceof String) {
            this.factoryId = Long.parseLong((String) factoryId);
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductionItem {
        private Long productId;
        private String productName;
        private Integer quantity;

        @JsonSetter("productId")
        public void setProductId(Object productId) {
            if (productId instanceof Number) {
                this.productId = ((Number) productId).longValue();
            } else if (productId instanceof String) {
                this.productId = Long.parseLong((String) productId);
            }
        }
    }
}
