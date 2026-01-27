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
    private boolean forSale;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ImageDTO> images;
    private Integer totalQuantity;
    private List<FactorySimpleDTO> factories;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageDTO implements Serializable {
        private Long id;
        private String originalName;
        private String minioPath;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FactorySimpleDTO implements Serializable {
        private Long id;
        private String name;
        private String city;
        private String country;
        private boolean online;
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
                .forSale(product.isForSale())
                .active(product.isActive())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .images(product.getImages().stream()
                        .map(img -> ImageDTO.builder()
                                .id(img.getId())
                                .originalName(img.getOriginalName())
                                .minioPath(img.getMinioPath())
                                .build())
                        .toList())
                .factories(product.getFactories() != null ? product.getFactories().stream()
                        .map(f -> FactorySimpleDTO.builder()
                                .id(f.getId())
                                .name(f.getName())
                                .city(f.getCity() != null ? f.getCity().getName() : null)
                                .country(f.getCountry() != null ? f.getCountry().getName() : null)
                                .online(f.isOnline())
                                .build())
                        .toList() : List.of())
                .build();
    }

    public static ProductResponseDTO fromEntityWithQuantity(Product product, Integer totalQuantity) {
        ProductResponseDTO dto = fromEntity(product);
        dto.setTotalQuantity(totalQuantity);
        return dto;
    }
}
