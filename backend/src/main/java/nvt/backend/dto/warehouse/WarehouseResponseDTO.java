package nvt.backend.dto.warehouse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nvt.backend.model.warehouse.Warehouse;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseResponseDTO implements Serializable {
    private Long id;
    private String name;
    private Long countryId;
    private String countryName;
    private Long cityId;
    private String cityName;
    private String street;
    private String streetNumber;
    private Double latitude;
    private Double longitude;
    private Double totalCapacity;
    private boolean active;
    private boolean online;
    private LocalDateTime lastHeartbeat;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<SectorDTO> sectors;
    private List<ImageDTO> images;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SectorDTO implements Serializable {
        private Long id;
        private String name;
        private Double minTemperature;
        private Double maxTemperature;
        private Double currentTemperature;
        private LocalDateTime lastTemperatureUpdate;
        private Double capacity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageDTO implements Serializable {
        private Long id;
        private String originalName;
    }

    public static WarehouseResponseDTO fromEntity(Warehouse warehouse) {
        return WarehouseResponseDTO.builder()
                .id(warehouse.getId())
                .name(warehouse.getName())
                .countryId(warehouse.getCountry() != null ? warehouse.getCountry().getId() : null)
                .countryName(warehouse.getCountry() != null ? warehouse.getCountry().getName() : null)
                .cityId(warehouse.getCity() != null ? warehouse.getCity().getId() : null)
                .cityName(warehouse.getCity() != null ? warehouse.getCity().getName() : null)
                .street(warehouse.getStreet())
                .streetNumber(warehouse.getStreetNumber())
                .latitude(warehouse.getLatitude())
                .longitude(warehouse.getLongitude())
                .totalCapacity(warehouse.getTotalCapacity())
                .active(warehouse.isActive())
                .online(warehouse.isOnline())
                .lastHeartbeat(warehouse.getLastHeartbeat())
                .createdAt(warehouse.getCreatedAt())
                .updatedAt(warehouse.getUpdatedAt())
                .sectors(warehouse.getSectors() != null ? warehouse.getSectors().stream()
                        .map(sector -> SectorDTO.builder()
                                .id(sector.getId())
                                .name(sector.getName())
                                .minTemperature(sector.getMinTemperature())
                                .maxTemperature(sector.getMaxTemperature())
                                .currentTemperature(sector.getCurrentTemperature())
                                .lastTemperatureUpdate(sector.getLastTemperatureUpdate())
                                .capacity(sector.getCapacity())
                                .build())
                        .toList() : List.of())
                .images(warehouse.getImages() != null ? warehouse.getImages().stream()
                        .map(img -> ImageDTO.builder()
                                .id(img.getId())
                                .originalName(img.getOriginalName())
                                .build())
                        .toList() : List.of())
                .build();
    }
}
