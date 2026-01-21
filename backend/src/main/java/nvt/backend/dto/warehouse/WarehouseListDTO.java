package nvt.backend.dto.warehouse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nvt.backend.model.warehouse.Warehouse;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseListDTO implements Serializable {
    private Long id;
    private String name;
    private String countryName;
    private String cityName;
    private String street;
    private String streetNumber;
    private Double totalCapacity;
    private int sectorCount;
    private boolean active;
    private boolean online;
    private LocalDateTime lastHeartbeat;
    private LocalDateTime createdAt;

    public static WarehouseListDTO fromEntity(Warehouse warehouse) {
        return WarehouseListDTO.builder()
                .id(warehouse.getId())
                .name(warehouse.getName())
                .countryName(warehouse.getCountry() != null ? warehouse.getCountry().getName() : null)
                .cityName(warehouse.getCity() != null ? warehouse.getCity().getName() : null)
                .street(warehouse.getStreet())
                .streetNumber(warehouse.getStreetNumber())
                .totalCapacity(warehouse.getTotalCapacity())
                .sectorCount(warehouse.getSectors() != null ? warehouse.getSectors().size() : 0)
                .active(warehouse.isActive())
                .online(warehouse.isOnline())
                .lastHeartbeat(warehouse.getLastHeartbeat())
                .createdAt(warehouse.getCreatedAt())
                .build();
    }
}
