package nvt.backend.dto.vehicle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleStatusDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Long vehicleId;
    private String licensePlate;
    private String brandName;
    private String modelName;
    private boolean online;
    private Double latitude;
    private Double longitude;
    private Double totalDistance;
    private LocalDateTime lastHeartbeat;
    private LocalDateTime lastTelemetry;
    private String currentState;
}
