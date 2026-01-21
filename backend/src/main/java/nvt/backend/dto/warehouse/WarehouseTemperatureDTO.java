package nvt.backend.dto.warehouse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseTemperatureDTO implements Serializable {
    private Long warehouseId;
    private String warehouseName;
    private String timestamp;
    private List<SectorTemperature> sectors;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SectorTemperature implements Serializable {
        private String sectorName;
        private Double temperature;
    }
}
