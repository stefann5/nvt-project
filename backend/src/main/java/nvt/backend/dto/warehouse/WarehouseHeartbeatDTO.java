package nvt.backend.dto.warehouse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseHeartbeatDTO implements Serializable {
    private Long warehouseId;
    private String warehouseName;
    private String timestamp;
    private String status;
}
