package nvt.backend.dto.factory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nvt.backend.model.factory.Factory;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FactoryListDTO implements Serializable {
    private Long id;
    private String name;
    private String description;
    private String countryName;
    private String cityName;
    private String street;
    private String streetNumber;
    private int productCount;
    private boolean active;
    private boolean online;
    private LocalDateTime lastHeartbeat;
    private LocalDateTime createdAt;

    public static FactoryListDTO fromEntity(Factory factory) {
        return FactoryListDTO.builder()
                .id(factory.getId())
                .name(factory.getName())
                .description(factory.getDescription())
                .countryName(factory.getCountry() != null ? factory.getCountry().getName() : null)
                .cityName(factory.getCity() != null ? factory.getCity().getName() : null)
                .street(factory.getStreet())
                .streetNumber(factory.getStreetNumber())
                .productCount(factory.getProducts() != null ? factory.getProducts().size() : 0)
                .active(factory.isActive())
                .online(factory.isOnline())
                .lastHeartbeat(factory.getLastHeartbeat())
                .createdAt(factory.getCreatedAt())
                .build();
    }
}
