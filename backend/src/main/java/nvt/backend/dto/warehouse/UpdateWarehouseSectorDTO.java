package nvt.backend.dto.warehouse;

import lombok.Data;

@Data
public class UpdateWarehouseSectorDTO {
    private Long id;
    private String name;
    private Double minTemperature;
    private Double maxTemperature;
    private Double capacity;
}
