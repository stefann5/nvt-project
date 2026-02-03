package nvt.backend.dto.factory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nvt.backend.model.factory.Factory;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FactoryResponseDTO implements Serializable {
    private Long id;
    private String name;
    private String description;
    private Long countryId;
    private String countryName;
    private Long cityId;
    private String cityName;
    private String street;
    private String streetNumber;
    private Double latitude;
    private Double longitude;
    private boolean active;
    private boolean online;
    private LocalDateTime lastHeartbeat;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ProductDTO> products;
    private List<ImageDTO> images;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductDTO implements Serializable {
        private Long id;
        private String name;
        private String categoryName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageDTO implements Serializable {
        private Long id;
        private String originalName;
    }

    public static FactoryResponseDTO fromEntity(Factory factory) {
        return FactoryResponseDTO.builder()
                .id(factory.getId())
                .name(factory.getName())
                .description(factory.getDescription())
                .countryId(factory.getCountry() != null ? factory.getCountry().getId() : null)
                .countryName(factory.getCountry() != null ? factory.getCountry().getName() : null)
                .cityId(factory.getCity() != null ? factory.getCity().getId() : null)
                .cityName(factory.getCity() != null ? factory.getCity().getName() : null)
                .street(factory.getStreet())
                .streetNumber(factory.getStreetNumber())
                .latitude(factory.getLatitude())
                .longitude(factory.getLongitude())
                .active(factory.isActive())
                .online(factory.isOnline())
                .lastHeartbeat(factory.getLastHeartbeat())
                .createdAt(factory.getCreatedAt())
                .updatedAt(factory.getUpdatedAt())
                .products(factory.getProducts() != null ? factory.getProducts().stream()
                        .<ProductDTO>map(product -> ProductDTO.builder()
                                .id(product.getId())
                                .name(product.getName())
                                .categoryName(product.getCategory() != null ? product.getCategory() : null)
                                .build())
                        .toList() : List.of())
                .images(factory.getImages() != null ? factory.getImages().stream()
                        .<ImageDTO>map(img -> ImageDTO.builder()
                                .id(img.getId())
                                .originalName(img.getOriginalName())
                                .build())
                        .toList() : List.of())
                .build();
    }
}
