package nvt.backend.dto.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nvt.backend.model.product.Product;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponseDTO implements Serializable {
    private Long id;
    private String name;
    private String description;
    private String sku;
    private BigDecimal price;
    private BigDecimal weight;
    private String category;
    private String unit;
    private boolean active;
    private LocalDateTime createdAt;
    private List<ImageDTO> images;
    private Integer totalQuantity;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageDTO implements Serializable {
        private Long id;
        private String originalName;
    }

    public static ProductResponseDTO fromEntity(Product product) {
        return ProductResponseDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .sku(product.getSku())
                .price(product.getPrice())
                .weight(product.getWeight())
                .category(product.getCategory())
                .unit(product.getUnit())
                .active(product.isActive())
                .createdAt(product.getCreatedAt())
                .images(product.getImages().stream()
                        .map(img -> ImageDTO.builder()
                                .id(img.getId())
                                .originalName(img.getOriginalName())
                                .build())
                        .toList())
                .build();
    }

    public static ProductResponseDTO fromEntityWithQuantity(Product product, Integer totalQuantity) {
        ProductResponseDTO dto = fromEntity(product);
        dto.setTotalQuantity(totalQuantity);
        return dto;
    }
}
