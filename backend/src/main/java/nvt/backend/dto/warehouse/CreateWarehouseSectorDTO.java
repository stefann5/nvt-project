package nvt.backend.dto.warehouse;

import lombok.Data;

@Data
public class CreateWarehouseSectorDTO {
    private String name;
    private Double minTemperature;
    private Double maxTemperature;
    private Double capacity;
}
